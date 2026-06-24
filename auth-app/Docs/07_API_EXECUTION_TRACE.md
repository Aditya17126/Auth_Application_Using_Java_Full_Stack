# Section 5: API Execution Trace — Step-by-Step Request Walkthroughs

Let's trace exactly what happens inside your application for specific API calls.

We will follow the request through every single file and layer.

For each trace, we show:

```text
1. The CURRENT state (what happens now)
2. The INTENDED state (what should happen after building)
```

---

# Trace 1: POST /api/users — Create a New User

## The Goal

A frontend client wants to register a new user named Aditya.

---

## Sample Request

```text
POST http://localhost:8083/api/users
Content-Type: application/json
```

```json
{
    "name": "Aditya",
    "email": "aditya@gmail.com",
    "password": "Secret123",
    "image": "https://example.com/aditya.jpg"
}
```

---

## Current State: What Happens RIGHT NOW

```text
Client sends POST /api/users
         ↓
Tomcat receives the request
         ↓
Spring Security Filter Chain activates
    (Default config — all endpoints protected)
         ↓
User is NOT authenticated
         ↓
HTTP 401 Unauthorized
    OR
Redirected to /login page
         ↓
REQUEST NEVER REACHES ANY CONTROLLER
    (because no controller exists anyway)
```

Result:

```json
{
    "status": 401,
    "error": "Unauthorized"
}
```

The request dies at the security filter.

Even if security was disabled, it would return `404 Not Found` because there is no controller.

---

## Intended State: What SHOULD Happen (Full Trace)

Let's trace the complete intended flow, file by file.

---

### Step 1: Request Arrives at Tomcat

```text
File: (Embedded Tomcat — no source file)

Raw HTTP request arrives on port 8083.
Tomcat creates:
    → HttpServletRequest object
    → HttpServletResponse object
```

---

### Step 2: Spring Security Filter Chain

```text
File: config/SecurityConfig.java  ← DOES NOT EXIST YET

Intended behavior:
    POST /api/users → permitAll()
    No authentication required for registration.

Filter chain passes the request through.
```

What the SecurityConfig would do:

```java
// File: config/SecurityConfig.java (INTENDED)

http.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
    .anyRequest().authenticated()
);
```

Result:

```text
Request passes through security.
No credentials required.
```

---

### Step 3: DispatcherServlet Routes the Request

```text
File: (Spring Framework internal — DispatcherServlet.java)

DispatcherServlet.doDispatch() is called.
    ↓
HandlerMapping scans for:
    Method: POST
    URL:    /api/users
    ↓
Finds: UserController.createUser()
    ↓
HandlerAdapter prepares to call the method.
```

---

### Step 4: Jackson Deserializes JSON → UserDto

```text
File: dtos/UserDto.java  ← EXISTS

Before the controller method executes,
Spring's HttpMessageConverter (Jackson) converts:

JSON Input:
{
    "name": "Aditya",
    "email": "aditya@gmail.com",
    "password": "Secret123",
    "image": "https://example.com/aditya.jpg"
}

Into Java Object:
UserDto {
    id       = null            (not provided → null)
    name     = "Aditya"
    email    = "aditya@gmail.com"
    password = "Secret123"
    image    = "https://example.com/aditya.jpg"
    enable   = true            (default value)
    createdAt = Instant.now()  (default value)
    updatedAt = Instant.now()  (default value)
    provider  = Provider.LOCAL (default value)
    roles     = HashSet[]      (default empty)
}
```

Jackson calls the no-args constructor (from `@NoArgsConstructor`) and then uses setters (from `@Setter`) to populate fields.

---

### Step 5: Controller Receives the Request

```text
File: controllers/UserController.java  ← DOES NOT EXIST YET

Intended code:

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserDto userDto) {

        UserDto created = userService.createUser(userDto);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
    }
}
```

What happens here:

```text
Controller receives:
    UserDto { name="Aditya", email="aditya@gmail.com", ... }

Controller calls:
    userService.createUser(userDto)

Controller does NOT:
    → Talk to database
    → Validate business rules
    → Convert entities
```

The controller is a thin layer. It only delegates.

---

### Step 6: Validation Executes (if @Valid is used)

```text
File: dtos/UserDto.java  ← EXISTS but has NO validation annotations

Intended: @Valid on controller parameter triggers
Jakarta Bean Validation.

Checks:
    name     → @NotBlank  → "Aditya"      → ✅ PASS
    email    → @Email     → "aditya@..."   → ✅ PASS
    password → @Size(6)   → "Secret123"    → ✅ PASS

All validations pass → continue to service.

If any fail:
    → MethodArgumentNotValidException thrown
    → Spring returns 400 Bad Request
```

---

### Step 7: Service Layer Processes the Request

```text
File: services/UserServiceImpl.java  ← EXISTS (but returns null)
```

Current code:

```java
@Override
public UserDto createUser(UserDto userDto) {
    return null;  // ← This is what happens RIGHT NOW
}
```

Intended implementation:

```java
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDto createUser(UserDto userDto) {

        // STEP 7a: Business Validation
        boolean emailExists = userRepository.existsByEmail(userDto.getEmail());
        if (emailExists) {
            throw new RuntimeException("User with email already exists");
        }

        // STEP 7b: DTO → Entity Conversion
        User user = User.builder()
            .name(userDto.getName())
            .email(userDto.getEmail())
            .password(userDto.getPassword())   // TODO: Hash with BCrypt
            .image(userDto.getImage())
            .provider(userDto.getProvider())
            .enable(userDto.isEnable())
            .build();

        // STEP 7c: Save to Database
        User savedUser = userRepository.save(user);

        // STEP 7d: Entity → DTO Conversion
        UserDto responseDto = UserDto.builder()
            .id(savedUser.getId())
            .name(savedUser.getName())
            .email(savedUser.getEmail())
            .image(savedUser.getImage())
            .enable(savedUser.isEnable())
            .createdAt(savedUser.getCreatedAt())
            .updatedAt(savedUser.getUpdatedAt())
            .provider(savedUser.getProvider())
            .roles(savedUser.getRoles())
            .build();

        // Note: password is intentionally NOT copied to response

        return responseDto;
    }
}
```

Data flow through the service:

```text
Service RECEIVES:
    UserDto {
        name: "Aditya",
        email: "aditya@gmail.com",
        password: "Secret123",
        image: "https://example.com/aditya.jpg"
    }

Service DOES:
    1. Checks: existsByEmail("aditya@gmail.com") → false → OK
    2. Converts: UserDto → User Entity
    3. Saves: userRepository.save(user)
    4. Converts: User Entity → UserDto (without password)

Service RETURNS:
    UserDto {
        id: "550e8400-e29b-41d4-a716-446655440000",
        name: "Aditya",
        email: "aditya@gmail.com",
        image: "https://example.com/aditya.jpg",
        enable: true,
        createdAt: "2026-06-20T12:00:00Z",
        updatedAt: "2026-06-20T12:00:00Z",
        provider: "LOCAL",
        roles: []
    }
```

---

### Step 8: Repository Interacts with Hibernate

```text
File: repositories/UserRepository.java  ← EXISTS

Step 8a: existsByEmail("aditya@gmail.com")
```

What Spring Data JPA does internally:

```text
Method: existsByEmail("aditya@gmail.com")
         ↓
Spring Data generates query:
         ↓
Hibernate receives the query request
```

SQL Generated by Hibernate:

```sql
SELECT
    COUNT(*) > 0
FROM
    users u
WHERE
    u.user_email = 'aditya@gmail.com'
```

Result:

```text
false  (no user with this email exists)
```

---

```text
Step 8b: userRepository.save(user)
```

What happens inside:

```text
Method: save(user)
         ↓
EntityManager checks:
    "Does this entity have an ID?"
         ↓
    ID is null (new entity)
         ↓
    Calls: entityManager.persist(user)
         ↓
    @PrePersist callback fires:
        onCreate() method in User.java
        Sets createdAt = Instant.now()
        Sets updatedAt = Instant.now()
         ↓
    Hibernate generates UUID for ID
         ↓
    Hibernate generates INSERT SQL
```

---

### Step 9: Hibernate Generates SQL

```text
File: entities/User.java  ← EXISTS (Hibernate reads annotations from this)

Hibernate translates the User entity into SQL.
```

SQL Generated:

```sql
INSERT INTO users
    (user_id, user_email, name, password, image, enable,
     created_at, updated_at, provider)
VALUES
    (UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440000'),
     'aditya@gmail.com',
     'Aditya',
     'Secret123',
     'https://example.com/aditya.jpg',
     true,
     '2026-06-20 12:00:00.000000',
     '2026-06-20 12:00:00.000000',
     'LOCAL')
```

Hibernate also logs (with `show-sql: true`):

```text
Hibernate:
    insert
    into
        users
        (created_at,enable,user_email,image,name,password,provider,updated_at,user_id)
    values
        (?,?,?,?,?,?,?,?,?)
```

The `?` are bind parameters. Hibernate fills them using a `PreparedStatement`.

---

### Step 10: HikariCP Provides a Database Connection

```text
File: (Spring Boot auto-configuration — no source file)

Hibernate asks HikariCP for a connection.

HikariCP Pool State:
    Total:     10 connections
    Active:    0
    Idle:      10

HikariCP provides Connection #1.

After SQL executes:
    Connection #1 returned to pool.

HikariCP Pool State:
    Total:     10 connections
    Active:    0
    Idle:      10
```

---

### Step 11: MySQL Executes the INSERT

```text
MySQL receives the INSERT statement.
    ↓
Parser validates SQL syntax.
    ↓
InnoDB storage engine:
    1. Acquires row-level lock
    2. Checks UNIQUE constraint on user_email
        → No duplicate found → ✅ OK
    3. Writes data to buffer pool
    4. Writes to redo log (WAL)
    5. Commits transaction
    ↓
Returns: 1 row affected
```

---

### Step 12: Response Travels Back Up

```text
MySQL → JDBC ResultSet (with generated ID)
         ↓
Hibernate → Maps result to User Entity
    User {
        id: UUID("550e8400..."),
        email: "aditya@gmail.com",
        name: "Aditya",
        ...
    }
         ↓
Repository → Returns User entity to Service
         ↓
Service → Converts User → UserDto (no password)
         ↓
Controller → Wraps in ResponseEntity (201 CREATED)
         ↓
DispatcherServlet → Jackson serializes to JSON
         ↓
Tomcat → Sends HTTP response
```

---

### Step 13: Final HTTP Response

```text
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Aditya",
    "email": "aditya@gmail.com",
    "image": "https://example.com/aditya.jpg",
    "enable": true,
    "createdAt": "2026-06-20T12:00:00Z",
    "updatedAt": "2026-06-20T12:00:00Z",
    "provider": "LOCAL",
    "roles": []
}
```

---

### Complete File Execution Order — POST /api/users

```text
File #                File Path                                         What It Does
─────────────────────────────────────────────────────────────────────────────────────────
 1. (embedded)        Tomcat                                            Receives HTTP request
 2. (MISSING)         config/SecurityConfig.java                        Checks authentication
 3. (framework)       DispatcherServlet                                 Routes to controller
 4. (MISSING)         controllers/UserController.java                   Accepts request, calls service
 5. EXISTS            dtos/UserDto.java                                 Jackson deserializes JSON into this
 6. EXISTS            services/UserServiceImpl.java                     Business logic (currently returns null)
 7. EXISTS            services/UserService.java                         Interface contract
 8. EXISTS            repositories/UserRepository.java                  existsByEmail() + save()
 9. EXISTS            entities/User.java                                Hibernate maps this to SQL
10. EXISTS            entities/Provider.java                            Enum value: LOCAL
11. (auto-config)     HikariCP                                         Provides DB connection
12. (external)        MySQL                                             Stores the data
```

---

# Trace 2: GET /api/users/{email} — Get User by Email

## The Goal

A client wants to fetch user details for `aditya@gmail.com`.

---

## Sample Request

```text
GET http://localhost:8083/api/users/aditya@gmail.com
```

No request body (GET requests don't have one).

---

## Current State: What Happens RIGHT NOW

```text
Client sends GET /api/users/aditya@gmail.com
         ↓
Spring Security blocks it (401 Unauthorized)
         ↓
Even if security was disabled:
    No controller exists → 404 Not Found
         ↓
Request never reaches any application code
```

---

## Intended State: Full Trace

### Step 1: Tomcat Receives Request

```text
File: (Embedded Tomcat)

GET /api/users/aditya@gmail.com HTTP/1.1
Host: localhost:8083

Tomcat creates HttpServletRequest.
    Method:  GET
    URI:     /api/users/aditya@gmail.com
    Body:    (empty)
```

---

### Step 2: Spring Security

```text
File: config/SecurityConfig.java  ← DOES NOT EXIST YET

Intended:
    GET /api/users/** → authenticated()
    User must provide valid credentials (JWT token).

For this trace, assume user IS authenticated.
    → Request passes through.
```

---

### Step 3: DispatcherServlet Routes

```text
File: (Spring Framework internal)

DispatcherServlet.doDispatch()
    ↓
HandlerMapping looks up:
    Method: GET
    URL:    /api/users/aditya@gmail.com
    ↓
Matches: @GetMapping("/{email}")
    in UserController
    ↓
Extracts path variable:
    email = "aditya@gmail.com"
```

---

### Step 4: Controller Handles the Request

```text
File: controllers/UserController.java  ← DOES NOT EXIST YET

Intended code:

@GetMapping("/{email}")
public ResponseEntity<UserDto> getUserByEmail(
        @PathVariable String email) {

    UserDto user = userService.getUserByEmail(email);
    return ResponseEntity.ok(user);
}
```

What happens:

```text
Controller RECEIVES:
    email = "aditya@gmail.com" (from URL path)

Controller CALLS:
    userService.getUserByEmail("aditya@gmail.com")

Controller RETURNS:
    ResponseEntity<UserDto> with status 200 OK
```

Note: No `@RequestBody` here because GET requests have no body.

The `@PathVariable` annotation extracts the value from the URL:

```text
URL:  /api/users/aditya@gmail.com
                  └──────────────┘
                       ↓
              email = "aditya@gmail.com"
```

---

### Step 5: Service Layer

```text
File: services/UserServiceImpl.java  ← EXISTS (returns null)
```

Current code:

```java
@Override
public UserDto getUserByEmail(String email) {
    return null;  // ← RIGHT NOW this returns null
}
```

Intended implementation:

```java
@Override
public UserDto getUserByEmail(String email) {

    // Step 5a: Call repository to find user
    User user = userRepository.findByEmail(email)
        .orElseThrow(() ->
            new RuntimeException("User not found with email: " + email));

    // Step 5b: Convert Entity → DTO
    UserDto userDto = UserDto.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .image(user.getImage())
        .enable(user.isEnable())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .provider(user.getProvider())
        .roles(user.getRoles())
        .build();

    return userDto;
}
```

Data flow:

```text
Service RECEIVES:
    email = "aditya@gmail.com"

Service DOES:
    1. Calls: userRepository.findByEmail("aditya@gmail.com")
    2. Gets:  Optional<User> → unwraps to User
    3. Converts: User → UserDto

Service RETURNS:
    UserDto {
        id: "550e8400-e29b-41d4-a716-446655440000",
        name: "Aditya",
        email: "aditya@gmail.com",
        image: "https://example.com/aditya.jpg",
        enable: true,
        ...
    }
```

---

### Step 6: Repository Executes the Query

```text
File: repositories/UserRepository.java  ← EXISTS

Method: findByEmail(String email)

This is a query derivation method.
Spring Data JPA reads the method name:

    find    → SELECT
    By      → WHERE
    Email   → user_email column (mapped via @Column on User.email)
```

Spring Data generates internally:

```java
// Auto-generated implementation (you never see this code)
@Override
public Optional<User> findByEmail(String email) {
    TypedQuery<User> query = entityManager.createQuery(
        "SELECT u FROM User u WHERE u.email = :email", User.class);
    query.setParameter("email", email);
    List<User> results = query.getResultList();
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
}
```

---

### Step 7: Hibernate Generates SQL

```text
File: entities/User.java  ← EXISTS (Hibernate reads @Table, @Column annotations)
```

Hibernate translates JPQL to native MySQL:

```sql
SELECT
    u.user_id,
    u.user_email,
    u.name,
    u.password,
    u.image,
    u.enable,
    u.created_at,
    u.updated_at,
    u.provider
FROM
    users u
WHERE
    u.user_email = 'aditya@gmail.com'
```

Because the User entity has `@ManyToMany(fetch = FetchType.EAGER)` for roles, Hibernate also immediately loads the roles:

```sql
SELECT
    r.id,
    r.name
FROM
    user_roles ur
INNER JOIN
    roles r ON ur.role_id = r.id
WHERE
    ur.user_id = ?
```

This is called EAGER loading:

```text
EAGER = Load related data immediately
LAZY  = Load related data only when accessed
```

Your User entity uses EAGER for roles:

```java
@ManyToMany(fetch = FetchType.EAGER)
```

So Hibernate runs TWO queries:

```text
Query 1: Get user from 'users' table
Query 2: Get roles from 'user_roles' + 'roles' tables
```

---

### Step 8: HikariCP Connection

```text
Same as Trace 1:
    HikariCP provides a connection.
    Hibernate runs the SELECT query.
    Connection is returned to pool.
```

---

### Step 9: MySQL Executes the SELECT

```text
MySQL receives:
    SELECT ... FROM users WHERE user_email = 'aditya@gmail.com'

MySQL does:
    1. Query optimizer checks for index on user_email
       (@Column unique=true creates a unique index)
    2. Uses index scan (very fast)
    3. Finds matching row
    4. Returns result set

Result Set:
    | user_id       | user_email         | name   | password  | ... |
    |---------------|--------------------|--------|-----------|-----|
    | 550e8400-...  | aditya@gmail.com   | Aditya | Secret123 | ... |
```

---

### Step 10: Hibernate Maps ResultSet → Entity

```text
File: entities/User.java  ← EXISTS

Hibernate reads the ResultSet and creates:

User {
    id:        UUID("550e8400-e29b-41d4-a716-446655440000")
    email:     "aditya@gmail.com"
    name:      "Aditya"
    password:  "Secret123"
    image:     "https://example.com/aditya.jpg"
    enable:    true
    createdAt: Instant("2026-06-20T12:00:00Z")
    updatedAt: Instant("2026-06-20T12:00:00Z")
    provider:  Provider.LOCAL
    roles:     Set<Role> []   (empty if no roles assigned)
}
```

Hibernate also places this entity in the First Level Cache (Persistence Context):

```text
First Level Cache:
    Key:   User#550e8400-...
    Value: User { name="Aditya", ... }

If the same user is queried again in this session,
Hibernate returns the cached version.
No second database query.
```

---

### Step 11: Response Travels Back

```text
Repository → Returns Optional<User> to Service
         ↓
Service → Unwraps Optional, converts User → UserDto
         ↓
Controller → Wraps UserDto in ResponseEntity (200 OK)
         ↓
DispatcherServlet → Jackson serializes UserDto → JSON
         ↓
Tomcat → Sends HTTP response
```

---

### Step 12: Final HTTP Response

```text
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Aditya",
    "email": "aditya@gmail.com",
    "image": "https://example.com/aditya.jpg",
    "enable": true,
    "createdAt": "2026-06-20T12:00:00Z",
    "updatedAt": "2026-06-20T12:00:00Z",
    "provider": "LOCAL",
    "roles": []
}
```

---

### What If the User Is NOT Found?

If no user exists with email `aditya@gmail.com`:

```text
Repository returns: Optional.empty()
         ↓
Service calls: .orElseThrow(...)
         ↓
Throws: RuntimeException("User not found with email: aditya@gmail.com")
         ↓
Exception propagates up to DispatcherServlet
         ↓
Without GlobalExceptionHandler:
    Spring returns generic 500 Internal Server Error

With GlobalExceptionHandler (INTENDED):
    Returns structured error response:
```

```json
{
    "error": "User not found with email: aditya@gmail.com"
}
```

with HTTP status `404 Not Found`.

---

### Complete File Execution Order — GET /api/users/{email}

```text
File #                File Path                                         What It Does
─────────────────────────────────────────────────────────────────────────────────────────
 1. (embedded)        Tomcat                                            Receives HTTP request
 2. (MISSING)         config/SecurityConfig.java                        Checks authentication
 3. (framework)       DispatcherServlet                                 Routes to controller
 4. (MISSING)         controllers/UserController.java                   Extracts email from path
 5. EXISTS            services/UserServiceImpl.java                     Calls repository (returns null now)
 6. EXISTS            services/UserService.java                         Interface contract
 7. EXISTS            repositories/UserRepository.java                  findByEmail()
 8. EXISTS            entities/User.java                                Hibernate maps result to this
 9. EXISTS            entities/Role.java                                Loaded via EAGER fetch
10. EXISTS            entities/Provider.java                            Enum value mapped
11. EXISTS            dtos/UserDto.java                                 Entity converted to this
12. (auto-config)     HikariCP                                         Provides DB connection
13. (external)        MySQL                                             Executes SELECT query
```

---

# Side-by-Side Comparison: POST vs GET

| Aspect                | POST /api/users                | GET /api/users/{email}          |
|-----------------------|--------------------------------|---------------------------------|
| HTTP Method           | POST                           | GET                             |
| Request Body          | JSON with user data            | None                            |
| Path Variable         | None                           | email                           |
| Controller Annotation | `@PostMapping`                 | `@GetMapping("/{email}")`       |
| Jackson Direction     | JSON → UserDto (deserialize)   | UserDto → JSON (serialize only) |
| Service Action        | Validate + Convert + Save      | Find + Convert                  |
| Repository Method     | `existsByEmail()` + `save()`   | `findByEmail()`                 |
| SQL Operation         | INSERT                         | SELECT                          |
| Response Status       | 201 Created                    | 200 OK                          |
| Response Body         | Created user DTO               | Found user DTO                  |

---

# All SQL Queries Summary

## POST /api/users generates:

```sql
-- Query 1: Check if email exists
SELECT COUNT(*) > 0
FROM users
WHERE user_email = 'aditya@gmail.com';

-- Query 2: Insert new user
INSERT INTO users
    (user_id, user_email, name, password, image, enable,
     created_at, updated_at, provider)
VALUES
    (?, ?, ?, ?, ?, ?, ?, ?, ?);
```

## GET /api/users/{email} generates:

```sql
-- Query 1: Find user by email
SELECT u.user_id, u.user_email, u.name, u.password,
       u.image, u.enable, u.created_at, u.updated_at, u.provider
FROM users u
WHERE u.user_email = 'aditya@gmail.com';

-- Query 2: Load roles (EAGER fetch)
SELECT r.id, r.name
FROM user_roles ur
INNER JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = ?;
```

---

# The Complete Chain — Visual Summary

## POST /api/users

```text
Client
  │
  │  POST /api/users
  │  { "name":"Aditya", "email":"aditya@gmail.com", ... }
  ▼
┌─────────────────┐
│     Tomcat       │  Receives raw HTTP, creates Java objects
└────────┬────────┘
         ▼
┌─────────────────┐
│ Security Filter  │  Checks: Is this endpoint public?
│   (MISSING)      │  Intended: permitAll() for POST /api/users
└────────┬────────┘
         ▼
┌─────────────────┐
│DispatcherServlet │  Maps POST /api/users → UserController.createUser()
└────────┬────────┘
         ▼
┌─────────────────┐
│  UserController  │  @PostMapping → calls userService.createUser(dto)
│   (MISSING)      │
└────────┬────────┘
         ▼
┌─────────────────┐
│ UserServiceImpl  │  Validates → Converts DTO→Entity → Saves → Converts Entity→DTO
│   (STUBS)        │  Currently: return null;
└────────┬────────┘
         ▼
┌─────────────────┐
│ UserRepository   │  existsByEmail() → save()
│   (EXISTS)       │  Spring Data JPA auto-implements
└────────┬────────┘
         ▼
┌─────────────────┐
│   Hibernate      │  Generates SQL: INSERT INTO users VALUES (?, ?, ...)
└────────┬────────┘
         ▼
┌─────────────────┐
│   HikariCP       │  Borrows connection from pool → executes → returns connection
└────────┬────────┘
         ▼
┌─────────────────┐
│     MySQL        │  Inserts row into 'users' table
└────────┬────────┘
         │
         │  1 row affected
         ▼
    (Response travels back up through each layer)
         │
         ▼
┌─────────────────┐
│     Client       │  Receives: 201 Created + JSON body
└─────────────────┘
```

---

## GET /api/users/{email}

```text
Client
  │
  │  GET /api/users/aditya@gmail.com
  │  (no body)
  ▼
┌─────────────────┐
│     Tomcat       │  Receives request
└────────┬────────┘
         ▼
┌─────────────────┐
│ Security Filter  │  Checks authentication (JWT token intended)
│   (MISSING)      │
└────────┬────────┘
         ▼
┌─────────────────┐
│DispatcherServlet │  Maps GET /api/users/{email} → getUserByEmail()
└────────┬────────┘
         ▼
┌─────────────────┐
│  UserController  │  Extracts @PathVariable email, calls service
│   (MISSING)      │
└────────┬────────┘
         ▼
┌─────────────────┐
│ UserServiceImpl  │  Calls findByEmail() → converts Entity→DTO
│   (STUBS)        │  Currently: return null;
└────────┬────────┘
         ▼
┌─────────────────┐
│ UserRepository   │  findByEmail("aditya@gmail.com")
│   (EXISTS)       │
└────────┬────────┘
         ▼
┌─────────────────┐
│   Hibernate      │  SELECT ... FROM users WHERE user_email = ?
│                  │  + SELECT ... FROM user_roles JOIN roles (EAGER)
└────────┬────────┘
         ▼
┌─────────────────┐
│   HikariCP       │  Provides connection
└────────┬────────┘
         ▼
┌─────────────────┐
│     MySQL        │  Returns matching row
└────────┬────────┘
         │
         │  ResultSet with user data
         ▼
    (Response travels back up)
         │
         ▼
┌─────────────────┐
│     Client       │  Receives: 200 OK + JSON body
└─────────────────┘
```

---

# What You Need to Build — Action Items

Based on these traces, here is what needs to be created:

```text
Priority 1: Create UserController.java
    → Without this, no API endpoints work.

Priority 2: Implement UserServiceImpl.java methods
    → Without this, all methods return null.

Priority 3: Configure SecurityConfig.java
    → Without this, all endpoints return 401.

Priority 4: Add validation annotations to UserDto.java
    → Without this, invalid data reaches the database.

Priority 5: Create GlobalExceptionHandler.java
    → Without this, errors return ugly stack traces.

Priority 6: Add password hashing (BCryptPasswordEncoder)
    → Without this, passwords are stored in plain text.
```

---

# Interview Tip

> "Trace a POST request through a Spring Boot application."

Answer:

```text
1. Tomcat receives the HTTP request on the configured port
2. Spring Security filter chain checks authentication/authorization
3. DispatcherServlet uses HandlerMapping to find the right controller method
4. Jackson deserializes the JSON body into a DTO object
5. Controller delegates to the service layer
6. Service validates business rules (e.g., duplicate email check)
7. Service converts DTO to Entity
8. Repository's save() method is called
9. Hibernate generates an INSERT SQL statement
10. HikariCP provides a pooled database connection
11. MySQL executes the query and returns the result
12. Hibernate maps the result back to an Entity
13. Service converts Entity back to DTO
14. Controller wraps it in ResponseEntity
15. Jackson serializes the DTO to JSON
16. Tomcat sends the HTTP response back to the client
```

This 16-step answer demonstrates deep understanding of the entire Spring Boot stack.

---

> "Knowing which file executes at which step is the difference between debugging for 5 minutes vs debugging for 5 hours."
