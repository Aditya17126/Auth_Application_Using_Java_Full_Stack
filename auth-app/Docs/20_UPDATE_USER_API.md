# UPDATE USER API — Complete Execution Trace

## PUT `/api/v1/users/{userId}`

> **Document Purpose**: This document traces every single line of code that executes when a client sends a `PUT` request to `/api/v1/users/{userId}`. It is designed so that a reader can confidently explain the entire flow in a technical interview — from the moment the HTTP request hits Tomcat to the moment the JSON response is sent back.

---

## 1. API Endpoint Details

| Property            | Value                                                      |
|---------------------|------------------------------------------------------------|
| **HTTP Method**     | `PUT`                                                      |
| **URL**             | `/api/v1/users/{userId}`                                   |
| **Path Variable**   | `userId` — a UUID string (e.g. `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"`) |
| **Request Body**    | JSON — partial or full `UserDto` object                    |
| **Content-Type**    | `application/json`                                         |
| **Success Response**| `200 OK` with updated `UserDto` as JSON                    |
| **Error Responses** | `404 Not Found` (user doesn't exist), `400 Bad Request` (malformed UUID) |
| **Auth Required**   | No (Spring Security is commented out)                      |

### Request Body Schema

```json
{
  "name": "string (optional — only updated if non-null)",
  "image": "string (optional — only updated if non-null)",
  "provider": "string enum (optional — LOCAL | GOOGLE | GITHUB | FACEBOOK)",
  "password": "string (optional — only updated if non-null)",
  "enable": "boolean (always applied — defaults to true if omitted)"
}
```

> **Design Decision**: The `email` field is intentionally **never updated**, even if the client sends it. This is by design — the source code has a comment: `// We are not going to chanege email id for this project`. The `id`, `createdAt`, `roles` fields sent in the request body are also ignored.

### Success Response (200 OK)

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "aditya@gmail.com",
  "name": "Aditya Kumar",
  "password": "Secret123",
  "image": "https://example.com/aditya-new.jpg",
  "enable": true,
  "createdAt": "2026-06-20T10:30:00Z",
  "updatedAt": "2026-06-27T15:18:00Z",
  "provider": "LOCAL",
  "roles": []
}
```

### Error Response — User Not Found (404)

```json
{
  "message": "User not found with given id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

### Error Response — Malformed UUID (400)

```json
{
  "message": "Invalid UUID string: not-a-uuid",
  "status": "BAD_REQUEST",
  "statusCode": 400
}
```

---

## 2. Real-World Example Scenario

**Scenario**: Aditya registered a week ago. His current profile is:

| Field       | Current Value                        |
|-------------|--------------------------------------|
| id          | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| email       | `aditya@gmail.com`                   |
| name        | `Aditya`                             |
| password    | `Secret123`                          |
| image       | `https://example.com/aditya.jpg`     |
| enable      | `true`                               |
| provider    | `LOCAL`                              |
| createdAt   | `2026-06-20T10:30:00Z`               |
| updatedAt   | `2026-06-20T10:30:00Z`               |
| roles       | `[]`                                 |

Now Aditya wants to:
1. Change his name from `"Aditya"` to `"Aditya Kumar"`
2. Change his profile image to a new URL
3. Keep everything else the same

**The PUT request he sends:**

```http
PUT http://localhost:8080/api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Content-Type: application/json

{
  "name": "Aditya Kumar",
  "image": "https://example.com/aditya-new.jpg",
  "enable": true
}
```

> **Key Observation**: Aditya does NOT send `password`, `provider`, or `email`. Because the service uses **null-check-based partial update**, only `name` and `image` will be modified. But `enable` is always set (no null check) — so he must send `true` to keep it unchanged. This is a design trade-off: primitives like `boolean` can't be `null`, so they are always overwritten.

---

## 3. Security Filter Chain — Bypassed

```
Client sends PUT /api/v1/users/{userId}
         ↓
Tomcat receives the raw HTTP request
         ↓
Spring Security Filter Chain: COMMENTED OUT / NOT CONFIGURED
         ↓
No authentication check occurs
No JWT validation occurs
No role-based authorization occurs
         ↓
Request passes directly to DispatcherServlet
```

In this project, Spring Security is **fully commented out**. There is:
- No `SecurityFilterChain` bean
- No `JwtAuthFilter`
- No `OncePerRequestFilter` implementation

This means **any client can call this endpoint without credentials**. In a production application, you would add a `SecurityFilterChain` bean that requires authentication for `/api/v1/users/**` endpoints.

> **Interview Insight**: If Spring Security were enabled, the `JwtAuthFilter` (a `OncePerRequestFilter`) would execute before the request reaches the `DispatcherServlet`. It would extract the JWT from the `Authorization` header, validate it, create a `UsernamePasswordAuthenticationToken`, and set it in the `SecurityContextHolder`. Only then would the request proceed.

---

## 4. Complete Step-by-Step Execution Flow

### High-Level Flow Diagram

```
Client (Postman / Frontend)
         │
         │  PUT /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
         │  Body: { "name": "Aditya Kumar", "image": "https://..." }
         ▼
┌─────────────────────────────┐
│      Embedded Tomcat         │
│  Receives raw HTTP request   │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│     DispatcherServlet        │
│  Maps PUT + URL → handler    │
│  Finds: UserController       │
│         .updateUser()        │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  HttpMessageConverter        │
│  (MappingJackson2...)        │
│  JSON body → UserDto object  │
│  Path var → String userId    │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│     UserController           │
│  updateUser(userDto, userId) │
│  Delegates to UserService    │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│     UserServiceImpl          │
│  1. Parse UUID               │
│  2. Find user in DB          │
│  3. Apply partial updates    │
│  4. Save to DB               │
│  5. Map Entity → DTO         │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│     UserRepository           │
│  (Spring Data JPA)           │
│  findById() → SELECT        │
│  save()     → UPDATE         │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│     Hibernate / JPA          │
│  @PreUpdate fires onUpdate() │
│  SQL UPDATE executed         │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  ModelMapper                 │
│  User entity → UserDto       │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  HttpMessageConverter        │
│  UserDto → JSON string       │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  ResponseEntity.ok(body)     │
│  HTTP 200 + JSON body        │
└──────────────┬──────────────┘
               ▼
          Client receives
          200 OK + JSON
```

---

### Step 4.1 — Tomcat Receives the Request

```
Incoming HTTP:
  Method:  PUT
  URI:     /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
  Headers: Content-Type: application/json
  Body:    {"name":"Aditya Kumar","image":"https://example.com/aditya-new.jpg","enable":true}
```

Tomcat's `Connector` thread reads the raw bytes from the TCP socket, parses the HTTP protocol, and wraps everything into two objects:
- `HttpServletRequest` — contains the method, URI, headers, body input stream
- `HttpServletResponse` — an empty response object to be populated later

Tomcat then hands these objects to Spring's `DispatcherServlet`.

---

### Step 4.2 — DispatcherServlet Handler Mapping

The `DispatcherServlet` is Spring MVC's front controller. It:

1. **Receives** the `HttpServletRequest`
2. **Consults** its list of `HandlerMapping` beans (specifically `RequestMappingHandlerMapping`)
3. **Matches** the request `PUT /api/v1/users/a1b2c3d4-...` against all registered `@RequestMapping` annotations

The matching logic:
- `UserController` has `@RequestMapping("/api/v1/users")` → matches the prefix `/api/v1/users`
- The `updateUser` method has `@PutMapping("/{userId}")` → matches `PUT` method + `/{userId}` path template
- The path variable `{userId}` captures `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"`

**Result**: The `DispatcherServlet` creates a `HandlerMethod` pointing to:
```
UserController.updateUser(UserDto, String)
```

---

### Step 4.3 — Argument Resolution (JSON → Java Objects)

Before calling `updateUser()`, Spring must resolve its two parameters:

#### Parameter 1: `@RequestBody UserDto userDto`

The `@RequestBody` annotation tells Spring to read the HTTP request body and convert it to a `UserDto` object. Spring delegates this to the `MappingJackson2HttpMessageConverter` (Jackson):

```
Raw JSON string                          Jackson ObjectMapper                  UserDto object
─────────────────────────────── ────→ ──────────────────────── ────→ ──────────────────────
{                                         Reads each key-value              UserDto {
  "name": "Aditya Kumar",                pair from JSON and                  id = null
  "image": "https://...new.jpg",         calls the matching                  email = null
  "enable": true                         setter on a new                     name = "Aditya Kumar"
}                                        UserDto instance                    password = null
                                                                             image = "https://example.com/aditya-new.jpg"
                                                                             enable = true
                                                                             createdAt = <Instant.now()>
                                                                             updatedAt = <Instant.now()>
                                                                             provider = LOCAL (default)
                                                                             roles = [] (default)
                                                                           }
```

**How Jackson creates the UserDto:**
1. Jackson calls `new UserDto()` — the `@NoArgsConstructor` (generated by Lombok) provides this constructor
2. The `UserDto` field initializers run: `enable = true`, `createdAt = Instant.now()`, `updatedAt = Instant.now()`, `provider = Provider.LOCAL`, `roles = new HashSet<>()`
3. Jackson finds `"name"` in JSON → calls `setName("Aditya Kumar")` (Lombok `@Setter`)
4. Jackson finds `"image"` in JSON → calls `setImage("https://example.com/aditya-new.jpg")`
5. Jackson finds `"enable"` in JSON → calls `setEnable(true)`
6. Fields NOT in JSON (`id`, `email`, `password`) keep their default values: `null`, `null`, `null`

> **Critical**: `password` is `null` because Aditya didn't send it. This matters later because the null-check in the service will **skip** the password update — which is the intended partial update behavior.

> **What breaks if `@RequestBody` is removed?** Spring would not know to read the HTTP body. It would try to resolve `UserDto` from query parameters (using `@ModelAttribute` by default), which would fail or produce an empty object. The update would either error or reset all fields.

#### Parameter 2: `@PathVariable("userId") String userId`

The `@PathVariable` annotation tells Spring to extract the `{userId}` segment from the URL path:

```
URL:     /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Pattern: /api/v1/users/{userId}
                        └──────────────────────────────────┘
                        Extracted: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                        Type: String
```

Spring's `PathVariableMethodArgumentResolver` extracts this value and passes it as a `String`.

> **Why is the type String and not UUID?** The developer chose to accept it as `String` and manually parse it to `UUID` inside the service layer using `UserHelper.parseUUID()`. This gives more control over error handling.

> **What breaks if `@PathVariable("userId")` is removed?** Spring would not know which path segment to extract. The `userId` parameter would be `null`, and `UserHelper.parseUUID(null)` would throw a `NullPointerException`.

---

### Step 4.4 — Controller Layer Execution

**File**: `UserController.java`

```java
@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
   private final UserService userService;

   @PutMapping("/{userId}")
   public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto, @PathVariable("userId") String userId){
       return ResponseEntity.ok(userService.updateUser(userDto, userId));
   }
}
```

#### Line-by-line: Class Declaration

```java
@RestController
```
- **What it is**: A composed annotation = `@Controller` + `@ResponseBody`
- **Why it's here**: Tells Spring this class is a web controller AND that all method return values should be serialized directly to the HTTP response body (as JSON), not resolved as view names
- **What breaks if removed**: Spring won't register this class as a handler. No URL mapping happens. All requests to `/api/v1/users/**` return `404 Not Found`

```java
@RequestMapping("/api/v1/users")
```
- **What it is**: Sets the base URL path for ALL handler methods in this controller
- **Why it's here**: Every method's `@PutMapping`, `@GetMapping`, etc. will be prefixed with `/api/v1/users`
- **What breaks if removed**: The `@PutMapping("/{userId}")` would map to just `PUT /{userId}` at the root level. The client would need to call `PUT /a1b2c3d4-...` instead of `PUT /api/v1/users/a1b2c3d4-...`

```java
@AllArgsConstructor
```
- **What it is**: Lombok annotation that generates a constructor with all fields as parameters
- **Generated code**:
  ```java
  public UserController(UserService userService) {
      this.userService = userService;
  }
  ```
- **Why it's here**: Spring uses constructor injection to inject the `UserService` bean. Since there's only one constructor, `@Autowired` is implicit
- **What breaks if removed**: No constructor exists to inject `UserService`. Spring throws `BeanCreationException` at startup: *"No default constructor found"*

```java
private final UserService userService;
```
- **What it is**: A dependency field holding a reference to the `UserService` bean
- **Why `final`**: Ensures the dependency is immutable after injection — it cannot be reassigned. This is a best practice for constructor injection
- **What Spring injects**: At startup, Spring finds that `UserServiceImpl` (annotated with `@Service`) implements `UserService`. It creates a `UserServiceImpl` singleton and injects it here
- **What breaks if removed**: The controller has no way to call business logic. Compilation error on `userService.updateUser()`

#### Line-by-line: The `updateUser` Method

```java
@PutMapping("/{userId}")
```
- **What it is**: Shortcut for `@RequestMapping(method = RequestMethod.PUT, path = "/{userId}")`
- **Combined with class-level**: The full path becomes `PUT /api/v1/users/{userId}`
- **`{userId}`**: A URI template variable — Spring extracts the actual value from the URL
- **Why PUT and not PATCH**: PUT semantically means "replace the entire resource." However, this implementation actually does a partial update (only non-null fields). Technically, PATCH would be more RESTful, but PUT is used here for simplicity
- **What breaks if removed**: No handler mapping for `PUT /api/v1/users/{userId}`. Clients receive `405 Method Not Allowed` or `404 Not Found`

```java
public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto, @PathVariable("userId") String userId){
```
- **`ResponseEntity<UserDto>`**: Return type that gives full control over HTTP status code, headers, and body. The generic `<UserDto>` tells Jackson to serialize the body as a `UserDto`
- **`@RequestBody UserDto userDto`**: Covered in Step 4.3 — Jackson deserializes the JSON body into a `UserDto`
- **`@PathVariable("userId") String userId`**: Covered in Step 4.3 — extracts from URL path

```java
    return ResponseEntity.ok(userService.updateUser(userDto, userId));
```

This single line does multiple things:

1. **`userService.updateUser(userDto, userId)`** is called first:
   - `userService` is the injected `UserServiceImpl` instance
   - It receives the deserialized `UserDto` and the raw `String` userId
   - It returns an updated `UserDto` (details in Step 4.5)

2. **`ResponseEntity.ok(...)`** wraps the returned `UserDto`:
   - Creates a `ResponseEntity` with HTTP status `200 OK`
   - Sets the body to the returned `UserDto` object
   - Equivalent to: `new ResponseEntity<>(updatedUserDto, HttpStatus.OK)`

3. **`return`** sends the `ResponseEntity` back to the `DispatcherServlet`:
   - The `DispatcherServlet` sees `@RestController` → uses `MappingJackson2HttpMessageConverter`
   - Jackson serializes the `UserDto` to JSON
   - The JSON string is written to the `HttpServletResponse` body
   - HTTP status 200 is set in the response

> **What breaks if `ResponseEntity.ok()` is changed to `ResponseEntity.status(201).body()`?** Nothing breaks — but the client receives `201 Created` instead of `200 OK`. Semantically, `201` is wrong for an update (it implies a new resource was created).

**Data at this point:**

| Variable   | Type       | Value |
|-----------|-----------|-------|
| `userDto`  | `UserDto`  | `{id=null, email=null, name="Aditya Kumar", password=null, image="https://example.com/aditya-new.jpg", enable=true, provider=LOCAL, ...}` |
| `userId`   | `String`   | `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` |

→ Both values are passed into `UserServiceImpl.updateUser()`

---

### Step 4.5 — Service Layer Execution

**File**: `UserServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final ModelMapper modelMapper;

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
       UUID uId = UserHelper.parseUUID(userId);
       User existingUser = userRepository.findById(uId)
           .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));

        if(userDto.getName() != null){
            existingUser.setName(userDto.getName());
        }
        if(userDto.getImage() != null){
            existingUser.setImage(userDto.getImage());
        }
        if(userDto.getProvider() != null){
            existingUser.setProvider(userDto.getProvider());
        }
        if(userDto.getPassword() != null){
            existingUser.setPassword(userDto.getPassword());
        }
        existingUser.setEnable(userDto.isEnable());
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }
}
```

#### Line-by-line: Class Declaration

```java
@Service
```
- **What it is**: A Spring stereotype annotation — specialization of `@Component`
- **What it does**: Tells Spring to create a singleton instance of `UserServiceImpl` and register it in the application context as a bean
- **Why `@Service` and not `@Component`**: Semantically indicates this class holds business logic. Functionally identical to `@Component`, but improves readability
- **What breaks if removed**: Spring never creates this bean. The `UserController` fails at startup with: *`NoSuchBeanDefinitionException: No qualifying bean of type 'UserService'`*

```java
@RequiredArgsConstructor
```
- **What it is**: Lombok annotation that generates a constructor for all `final` fields
- **Generated code**:
  ```java
  public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
      this.userRepository = userRepository;
      this.modelMapper = modelMapper;
  }
  ```
- **Why it's here**: Enables constructor-based dependency injection. Spring calls this constructor and passes in the `UserRepository` proxy and the `ModelMapper` bean
- **What breaks if removed**: No constructor exists → `BeanCreationException` at startup

```java
private final UserRepository userRepository;
```
- **What it is**: A reference to the JPA repository for the `User` entity
- **What Spring injects**: Spring Data JPA automatically generates a proxy implementation class for the `UserRepository` interface. This proxy is injected here
- **What breaks if removed**: Cannot query or save users. Compilation error

```java
private final ModelMapper modelMapper;
```
- **What it is**: A reference to the `ModelMapper` bean (defined in `ProjectConfig.java`)
- **What it does**: Converts between `User` (entity) and `UserDto` (DTO) objects by matching field names
- **Where the bean comes from**: `ProjectConfig.java`:
  ```java
  @Configuration
  public class ProjectConfig {
    @Bean
    public ModelMapper modelMapper(){ return new ModelMapper(); }
  }
  ```
- **What breaks if removed**: Cannot convert `User` → `UserDto`. Compilation error

```java
@Override
public UserDto updateUser(UserDto userDto, String userId) {
```
- **`@Override`**: Compile-time check that this method exists in the `UserService` interface. If the interface method signature changes, the compiler flags this
- **Parameters received from controller**:
  - `userDto`: `{id=null, email=null, name="Aditya Kumar", password=null, image="https://example.com/aditya-new.jpg", enable=true, ...}`
  - `userId`: `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"`

---

#### Line 1: Parse UUID

```java
UUID uId = UserHelper.parseUUID(userId);
```

**What happens inside `UserHelper.parseUUID()`:**

```java
// File: UserHelper.java
public class UserHelper {
   public static UUID parseUUID(String uuid) {
      return UUID.fromString(uuid);
   }
}
```

| Aspect | Detail |
|--------|--------|
| **Input** | `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` |
| **Method called** | `UUID.fromString(String)` — a static method from `java.util.UUID` |
| **What it does** | Parses the 36-character string (8-4-4-4-12 hex format) into a 128-bit `UUID` object |
| **Output** | `UUID` object: `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| **Why a helper?** | Encapsulates UUID parsing in one place. If parsing logic changes (e.g., adding validation, logging), only this helper needs updating |

**Error Scenario — Malformed UUID:**

If the client sends `PUT /api/v1/users/not-a-valid-uuid`:

```java
UUID.fromString("not-a-valid-uuid")
// throws: java.lang.IllegalArgumentException: Invalid UUID string: not-a-valid-uuid
```

This `IllegalArgumentException` bubbles up. The `GlobalExceptionHandler` catches it:

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ErrorResponse> handleIllegalArgumentException(ResourceNotFoundException exception){
    ErrorResponse internalServerError = new ErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST, 400);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(internalServerError);
}
```

The client receives:
```json
{
  "message": "Invalid UUID string: not-a-valid-uuid",
  "status": "BAD_REQUEST",
  "statusCode": 400
}
```

> **What breaks if this line is removed?** The `userId` string cannot be used with `userRepository.findById()` because `findById()` expects a `UUID`, not a `String`. Compilation error.

**Data after this line:**

| Variable | Type | Value |
|----------|------|-------|
| `uId` | `UUID` | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |

---

#### Line 2: Find Existing User in Database

```java
User existingUser = userRepository.findById(uId)
    .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
```

This is a multi-step operation. Let's break it down:

**Step 2a: `userRepository.findById(uId)`**

| Aspect | Detail |
|--------|--------|
| **Who calls it** | `UserServiceImpl` calls the Spring Data JPA proxy |
| **Interface method** | `JpaRepository.findById(ID id)` — inherited from `CrudRepository` |
| **Input** | `UUID` object: `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| **What happens internally** | Spring Data JPA's proxy implementation delegates to `SimpleJpaRepository.findById()`, which calls `EntityManager.find(User.class, uId)` |
| **SQL generated by Hibernate** | See below |
| **Return type** | `Optional<User>` |

**SQL executed by Hibernate:**

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
FROM users u
WHERE u.user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

Because `User` has `@ManyToMany(fetch = FetchType.EAGER)` on the `roles` field, Hibernate **also** fetches roles in a join or subsequent query:

```sql
SELECT
    r.id,
    r.name
FROM user_roles ur
JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

> **Why `FetchType.EAGER`?** It means roles are loaded immediately with the user, not lazily. This avoids `LazyInitializationException` but can hurt performance if the user has many roles.

**Step 2b — Success Path: `.orElseThrow(...)`**

If the user IS found in the database, `findById()` returns `Optional.of(user)`. The `.orElseThrow()` call on a present `Optional` simply returns the contained `User` object. The lambda `() -> new ResourceNotFoundException(...)` is **never executed**.

**The `existingUser` object now holds:**

```java
User {
    id        = a1b2c3d4-e5f6-7890-abcd-ef1234567890
    email     = "aditya@gmail.com"
    name      = "Aditya"                          // ← current value, about to change
    password  = "Secret123"
    image     = "https://example.com/aditya.jpg"  // ← current value, about to change
    enable    = true
    createdAt = 2026-06-20T10:30:00Z
    updatedAt = 2026-06-20T10:30:00Z
    provider  = LOCAL
    roles     = []
}
```

> **Critical JPA Detail**: This `User` object is now in the **managed** (attached) state. Hibernate's `PersistenceContext` (first-level cache) tracks it. Any changes to this object's fields will be detected by Hibernate's dirty-checking mechanism during flush.

**Step 2b — Failure Path (Error Scenario):**

If no user exists with this UUID, `findById()` returns `Optional.empty()`. The `.orElseThrow()` on an empty `Optional` **executes** the lambda:

```java
() -> new ResourceNotFoundException("User not found with given id")
```

This creates and throws a `ResourceNotFoundException`. The exception propagates up:
1. Out of `updateUser()` method
2. Out of `UserController.updateUser()` method
3. Into the `DispatcherServlet`
4. The `DispatcherServlet` finds `GlobalExceptionHandler` (annotated with `@RestControllerAdvice`)
5. `handleResourceNotFoundException()` is called

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception){
    ErrorResponse internalServerError = new ErrorResponse(
        exception.getMessage(),    // "User not found with given id"
        HttpStatus.NOT_FOUND,      // 404
        404
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
}
```

**The error flow:**

```
ResourceNotFoundException thrown
         ↓
Propagates through Service → Controller
         ↓
DispatcherServlet catches it
         ↓
@RestControllerAdvice GlobalExceptionHandler
         ↓
@ExceptionHandler(ResourceNotFoundException.class) matches
         ↓
ErrorResponse record created:
  message   = "User not found with given id"
  status    = NOT_FOUND
  statusCode = 404
         ↓
ResponseEntity with 404 status + ErrorResponse body
         ↓
Jackson serializes ErrorResponse → JSON
         ↓
Client receives 404 + JSON error body
```

> **What breaks if `.orElseThrow()` is removed?** You'd have an `Optional<User>` instead of a `User`. Every subsequent line would fail to compile because you can't call `.setName()` on an `Optional`. If you used `.get()` instead, it would throw `NoSuchElementException` (an uncaught exception that returns a generic 500 error).

---

#### Lines 3-4: Partial Update — Name

```java
if(userDto.getName() != null){
    existingUser.setName(userDto.getName());
}
```

| Aspect | Detail |
|--------|--------|
| **`userDto.getName()`** | Returns `"Aditya Kumar"` (set by Jackson from the request JSON) |
| **Null check result** | `"Aditya Kumar" != null` → `true` → enters the `if` block |
| **`existingUser.setName("Aditya Kumar")`** | Overwrites the existing name `"Aditya"` with `"Aditya Kumar"` on the **managed** JPA entity |
| **Before** | `existingUser.name = "Aditya"` |
| **After** | `existingUser.name = "Aditya Kumar"` |

**Why the null check?**
This implements the **partial update pattern**. If the client doesn't send `"name"` in the JSON body, Jackson sets `name` to `null` on the `UserDto`. The null check prevents overwriting the database value with `null`. Only fields the client explicitly sends are updated.

**Scenario: Client omits `"name"` from JSON:**
```json
{ "image": "https://example.com/aditya-new.jpg", "enable": true }
```
→ `userDto.getName()` returns `null`
→ `null != null` is `false`
→ The `if` block is skipped
→ `existingUser.name` remains `"Aditya"` (unchanged)

> **What breaks if this null check is removed?** If the client sends `{}` (empty JSON), `setName(null)` would be called, overwriting the existing name with `null` in the database. The user would lose their name.

---

#### Lines 5-6: Partial Update — Image

```java
if(userDto.getImage() != null){
    existingUser.setImage(userDto.getImage());
}
```

| Aspect | Detail |
|--------|--------|
| **`userDto.getImage()`** | Returns `"https://example.com/aditya-new.jpg"` |
| **Null check result** | `"https://example.com/aditya-new.jpg" != null` → `true` |
| **`existingUser.setImage(...)`** | Overwrites `"https://example.com/aditya.jpg"` with `"https://example.com/aditya-new.jpg"` |
| **Before** | `existingUser.image = "https://example.com/aditya.jpg"` |
| **After** | `existingUser.image = "https://example.com/aditya-new.jpg"` |

Same partial update pattern as `name`. If the client doesn't send `"image"`, the existing value is preserved.

> **What breaks if removed?** The image field can never be updated. Even if the client sends a new image URL, it's ignored.

---

#### Lines 7-8: Partial Update — Provider

```java
if(userDto.getProvider() != null){
    existingUser.setProvider(userDto.getProvider());
}
```

| Aspect | Detail |
|--------|--------|
| **`userDto.getProvider()`** | Returns `Provider.LOCAL` (the default from `UserDto` field initializer) |
| **Null check result** | `Provider.LOCAL != null` → `true` |
| **`existingUser.setProvider(Provider.LOCAL)`** | Sets provider to `LOCAL` (was already `LOCAL`, so no actual change) |

> **Subtle Bug / Design Note**: The `UserDto` initializes `provider = Provider.LOCAL` as a default. So even if the client doesn't send `"provider"` in the JSON, the `UserDto.provider` field is `LOCAL` (not `null`). The null check will pass, and the provider will be set to `LOCAL`. This means:
> - If a user was registered via `GOOGLE` and the client sends an update without specifying `"provider"`, the provider gets silently changed from `GOOGLE` to `LOCAL`
> - To fix this, the `UserDto.provider` default should be `null` instead of `Provider.LOCAL`, or the update logic should compare against the DB value

In our scenario, Aditya's provider is already `LOCAL`, so this has no visible effect.

---

#### Lines 9-10: Partial Update — Password

```java
if(userDto.getPassword() != null){
    existingUser.setPassword(userDto.getPassword());
}
```

| Aspect | Detail |
|--------|--------|
| **`userDto.getPassword()`** | Returns `null` (Aditya didn't send `"password"` in the JSON) |
| **Null check result** | `null != null` → `false` → **block is skipped** |
| **Existing password preserved** | `existingUser.password` remains `"Secret123"` |

> **The source code has a comment**: `//TODO change password updation logic ...` — This indicates the developer knows that storing/updating passwords in plain text is insecure. In a real application, the password should be hashed with `BCryptPasswordEncoder` before saving.

> **What breaks if this null check is removed?** If the client sends `{}`, `setPassword(null)` would overwrite the existing password with `null`. The user could no longer log in.

---

#### Line 11: Direct Update — Enable (No Null Check)

```java
existingUser.setEnable(userDto.isEnable());
```

| Aspect | Detail |
|--------|--------|
| **`userDto.isEnable()`** | Returns `true` (sent in the request JSON) |
| **No null check** | `boolean` is a **primitive** type — it cannot be `null`. It always has a value (`true` or `false`) |
| **Default value** | `UserDto` initializes `enable = true`. If the client doesn't send `"enable"`, it defaults to `true` |
| **After** | `existingUser.enable = true` (unchanged in this case) |

> **Why no null check here but null checks for other fields?** `enable` is a `boolean` primitive, not `Boolean` wrapper. Primitives can't be null, so a null check is impossible (it would be a compiler error). The developer chose `boolean` over `Boolean`, which means `enable` is **always** overwritten, even if the client didn't intend to change it.

> **Design Trade-Off**: If the client sends `{}` (empty JSON), `enable` defaults to `true` in the `UserDto`, so `setEnable(true)` is called. This is safe if the default matches the expected behavior. But if a client sends `{"enable": false}` to disable a user, and later sends an update without `enable`, the user gets **re-enabled** unintentionally. To fix this, change `boolean` to `Boolean` and add a null check.

> **What breaks if this line is removed?** The `enable` field can never be updated. If an admin wants to disable a user account, they can't.

---

#### Line 12: Set Updated Timestamp

```java
existingUser.setUpdatedAt(Instant.now());
```

| Aspect | Detail |
|--------|--------|
| **`Instant.now()`** | Returns the current UTC timestamp, e.g., `2026-06-27T15:18:00Z` |
| **What it does** | Sets the `updatedAt` field on the managed entity to the current time |
| **Before** | `existingUser.updatedAt = 2026-06-20T10:30:00Z` |
| **After** | `existingUser.updatedAt = 2026-06-27T15:18:00Z` |

> **Redundancy Note**: The `User` entity also has a `@PreUpdate` callback:
> ```java
> @PreUpdate
> protected void onUpdate(){
>    updatedAt = Instant.now();
> }
> ```
> This means `updatedAt` is set **twice**:
> 1. Here, manually in the service (`existingUser.setUpdatedAt(Instant.now())`)
> 2. Again by JPA's `@PreUpdate` callback just before the SQL `UPDATE` is executed
>
> The final value in the database will be from `@PreUpdate` (since it runs last). The manual set on this line is technically **redundant** but harmless — it ensures the returned DTO has a recent timestamp even if `@PreUpdate` somehow didn't fire.

> **What breaks if this line is removed?** Nothing breaks — `@PreUpdate` still sets `updatedAt` automatically. However, the `updatedUser` object returned to the caller would have the `@PreUpdate` timestamp (which is practically the same instant).

---

#### Line 13: Save to Database

```java
User updatedUser = userRepository.save(existingUser);
```

This is the most complex line. Let's trace what happens inside:

**Step 13a: `userRepository.save(existingUser)` call chain:**

```
UserServiceImpl calls: userRepository.save(existingUser)
         ↓
Spring Data JPA proxy delegates to: SimpleJpaRepository.save(entity)
         ↓
SimpleJpaRepository checks: Is this entity new or existing?
  - entity.getId() = a1b2c3d4-... (not null)
  - EntityInformation.isNew(entity) returns false
  - Therefore: calls EntityManager.merge(entity)
         ↓
BUT WAIT: existingUser was loaded via findById() in this same transaction
  - It is already in the PersistenceContext (managed state)
  - EntityManager.merge() on an already-managed entity simply returns the same instance
  - Hibernate marks the entity as "dirty" (fields changed)
         ↓
Transaction commit (or flush) triggers:
  1. Hibernate's dirty checking compares current field values to the snapshot taken at load time
  2. Detects changes: name, image, updatedAt
  3. @PreUpdate callback fires → onUpdate() sets updatedAt = Instant.now()
  4. Generates and executes SQL UPDATE
```

**SQL generated by Hibernate:**

```sql
UPDATE users
SET
    name = 'Aditya Kumar',
    user_email = 'aditya@gmail.com',
    password = 'Secret123',
    image = 'https://example.com/aditya-new.jpg',
    enable = true,
    created_at = '2026-06-20T10:30:00Z',
    updated_at = '2026-06-27T15:18:00Z',
    provider = 'LOCAL'
WHERE user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

> **Note**: Hibernate's default behavior is to include ALL columns in the UPDATE statement, even unchanged ones. This is for performance (prepared statement caching). To update only changed columns, you can add `@DynamicUpdate` on the entity.

**Step 13b: `@PreUpdate` Callback Fires**

Just before the SQL `UPDATE` is executed, JPA fires the `@PreUpdate` lifecycle callback:

```java
@PreUpdate
protected void onUpdate(){
   updatedAt = Instant.now();
}
```

- This sets `updatedAt` to the current instant one more time
- The value is very close to (within milliseconds of) the manual `setUpdatedAt()` call above
- This is the final value that goes into the database

**Step 13c: Return Value**

`userRepository.save()` returns the saved entity. Since the entity was already managed, it returns the **same object reference** (`existingUser`). The variable `updatedUser` now points to the same `User` object with all the changes applied.

**`updatedUser` object state:**

```java
User {
    id        = a1b2c3d4-e5f6-7890-abcd-ef1234567890
    email     = "aditya@gmail.com"
    name      = "Aditya Kumar"                          // ← CHANGED
    password  = "Secret123"                              // ← unchanged
    image     = "https://example.com/aditya-new.jpg"     // ← CHANGED
    enable    = true                                     // ← unchanged
    createdAt = 2026-06-20T10:30:00Z                     // ← unchanged
    updatedAt = 2026-06-27T15:18:00Z                     // ← CHANGED
    provider  = LOCAL                                    // ← unchanged
    roles     = []                                       // ← unchanged
}
```

> **What breaks if this line is removed?** The changes would be lost. Even though Hibernate tracks dirty state on managed entities and *could* flush them at transaction commit, the method has no `@Transactional` annotation, so there is no surrounding transaction that would trigger an auto-flush. Without `save()`, the changes stay in memory and are never written to the database.

> **Interview Insight**: If `@Transactional` were on this method, Hibernate's dirty checking would automatically flush changes at transaction commit, even without calling `save()`. But since there's no `@Transactional` here, the explicit `save()` is required.

---

#### Line 14: Convert Entity → DTO

```java
return modelMapper.map(updatedUser, UserDto.class);
```

| Aspect | Detail |
|--------|--------|
| **Who calls it** | `UserServiceImpl` calls the `ModelMapper` bean |
| **Input** | `updatedUser` (a `User` entity) and `UserDto.class` (the target type) |
| **What ModelMapper does** | Uses reflection to match field names between `User` and `UserDto`. For each matching field name and compatible type, it copies the value |

**Field-by-field mapping:**

| User Entity Field | → | UserDto Field | Value Copied |
|-------------------|---|---------------|--------------|
| `id` (UUID) | → | `id` (UUID) | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| `email` (String) | → | `email` (String) | `"aditya@gmail.com"` |
| `name` (String) | → | `name` (String) | `"Aditya Kumar"` |
| `password` (String) | → | `password` (String) | `"Secret123"` |
| `image` (String) | → | `image` (String) | `"https://example.com/aditya-new.jpg"` |
| `enable` (boolean) | → | `enable` (boolean) | `true` |
| `createdAt` (Instant) | → | `createdAt` (Instant) | `2026-06-20T10:30:00Z` |
| `updatedAt` (Instant) | → | `updatedAt` (Instant) | `2026-06-27T15:18:00Z` |
| `provider` (Provider) | → | `provider` (Provider) | `LOCAL` |
| `roles` (Set\<Role\>) | → | `roles` (Set\<Role\>) | `[]` |

**The returned `UserDto` object:**

```java
UserDto {
    id        = a1b2c3d4-e5f6-7890-abcd-ef1234567890
    email     = "aditya@gmail.com"
    name      = "Aditya Kumar"
    password  = "Secret123"
    image     = "https://example.com/aditya-new.jpg"
    enable    = true
    createdAt = 2026-06-20T10:30:00Z
    updatedAt = 2026-06-27T15:18:00Z
    provider  = LOCAL
    roles     = []
}
```

> **Why convert Entity → DTO?** The `User` entity may contain sensitive information or JPA-specific annotations that shouldn't be exposed to the API consumer. The DTO is a clean data transfer object. However, in this specific project, the `UserDto` includes `password` — which is a **security issue**. In production, you would exclude `password` from the DTO or use `@JsonIgnore`.

> **What breaks if this line is removed?** The method would need to return something else. If it returned the `User` entity directly, Jackson would serialize it including JPA proxy objects, potentially causing `LazyInitializationException` or circular reference issues.

This `UserDto` is returned to the controller.

---

### Step 4.6 — Response Serialization

Back in the controller:

```java
return ResponseEntity.ok(userService.updateUser(userDto, userId));
```

The `userService.updateUser()` has returned the `UserDto`. Now:

1. `ResponseEntity.ok(userDto)` creates:
   ```
   ResponseEntity {
       status: 200 (OK)
       headers: {}
       body: UserDto { ... }
   }
   ```

2. The `DispatcherServlet` receives this `ResponseEntity`

3. It uses `MappingJackson2HttpMessageConverter` (Jackson) to serialize the `UserDto` to JSON

4. Jackson calls getters on `UserDto` (generated by Lombok `@Getter`):
   - `getId()` → `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` (UUID serialized as string)
   - `getEmail()` → `"aditya@gmail.com"`
   - `getName()` → `"Aditya Kumar"`
   - `getPassword()` → `"Secret123"`
   - `getImage()` → `"https://example.com/aditya-new.jpg"`
   - `isEnable()` → `true` (boolean getter uses `is` prefix)
   - `getCreatedAt()` → `"2026-06-20T10:30:00Z"` (Instant serialized as ISO-8601)
   - `getUpdatedAt()` → `"2026-06-27T15:18:00Z"`
   - `getProvider()` → `"LOCAL"` (enum serialized as string)
   - `getRoles()` → `[]`

5. The final JSON written to the HTTP response body:

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "aditya@gmail.com",
  "name": "Aditya Kumar",
  "password": "Secret123",
  "image": "https://example.com/aditya-new.jpg",
  "enable": true,
  "createdAt": "2026-06-20T10:30:00Z",
  "updatedAt": "2026-06-27T15:18:00Z",
  "provider": "LOCAL",
  "roles": []
}
```

6. HTTP Response:
   ```
   HTTP/1.1 200 OK
   Content-Type: application/json
   
   { ... JSON body ... }
   ```

---

## 5. Complete Error Scenarios

### Error Scenario 1: User Not Found (404)

**Request:**
```http
PUT /api/v1/users/99999999-9999-9999-9999-999999999999
Content-Type: application/json

{ "name": "Ghost User" }
```

**Execution Flow:**

```
1. UserHelper.parseUUID("99999999-9999-9999-9999-999999999999")
   → Valid UUID format → UUID object created ✓

2. userRepository.findById(UUID)
   → SQL: SELECT ... FROM users WHERE user_id = '99999999-...'
   → No row found
   → Returns Optional.empty()

3. .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"))
   → Optional is empty → lambda executes
   → new ResourceNotFoundException("User not found with given id") is created
   → Exception is thrown ✗

4. Exception propagates: Service → Controller → DispatcherServlet

5. GlobalExceptionHandler.handleResourceNotFoundException() catches it
   → Creates ErrorResponse("User not found with given id", NOT_FOUND, 404)
   → Returns ResponseEntity with 404 status
```

**Response:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "message": "User not found with given id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

---

### Error Scenario 2: Malformed UUID (400)

**Request:**
```http
PUT /api/v1/users/not-a-uuid
Content-Type: application/json

{ "name": "Hacker" }
```

**Execution Flow:**

```
1. UserHelper.parseUUID("not-a-uuid")
   → UUID.fromString("not-a-uuid")
   → Throws IllegalArgumentException: "Invalid UUID string: not-a-uuid"

2. Exception propagates: Service → Controller → DispatcherServlet

3. GlobalExceptionHandler.handleIllegalArgumentException() catches it
   → Creates ErrorResponse with BAD_REQUEST, 400
   → Returns ResponseEntity with 400 status
```

**Response:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "message": "Invalid UUID string: not-a-uuid",
  "status": "BAD_REQUEST",
  "statusCode": 400
}
```

---

### Error Scenario 3: Empty JSON Body

**Request:**
```http
PUT /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Content-Type: application/json

{}
```

**Execution Flow:**

```
1. Jackson creates UserDto with all defaults:
   → id=null, email=null, name=null, password=null, image=null
   → enable=true (primitive default), provider=LOCAL (field initializer)

2. UserHelper.parseUUID() → valid UUID ✓

3. userRepository.findById() → User found ✓

4. if(userDto.getName() != null)     → null → SKIP (name preserved)
5. if(userDto.getImage() != null)    → null → SKIP (image preserved)
6. if(userDto.getProvider() != null) → LOCAL (not null!) → ENTERS BLOCK
   → existingUser.setProvider(LOCAL)  ← Could silently change GOOGLE → LOCAL!
7. if(userDto.getPassword() != null) → null → SKIP (password preserved)
8. existingUser.setEnable(true)      → always set (primitive)
9. existingUser.setUpdatedAt(now)    → timestamp updated
10. userRepository.save()            → SQL UPDATE executed

RESULT: User is "updated" with updatedAt changed, provider potentially changed.
        Name, image, password are preserved.
```

> **Interview Point**: This is a subtle issue. Sending `{}` triggers an update where `provider` gets overwritten to `LOCAL` (because of the `UserDto` field default) and `updatedAt` changes. The `enable` field is also re-set to `true`. This is why field defaults in DTOs used for partial updates can be dangerous.

---

## 6. Data Flow Tracking

### Complete Data Transformation Journey

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 1: Raw HTTP Request                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Method: PUT                                                                 │
│ URL: /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890                    │
│ Body: {"name":"Aditya Kumar","image":"https://example.com/aditya-new.jpg",  │
│        "enable":true}                                                       │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ Jackson + PathVariableResolver
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 2: Java Objects (Controller receives)                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890" (String)                   │
│ userDto = UserDto {                                                         │
│   id=null, email=null, name="Aditya Kumar", password=null,                  │
│   image="https://example.com/aditya-new.jpg", enable=true,                  │
│   provider=LOCAL, createdAt=now, updatedAt=now, roles=[]                    │
│ }                                                                           │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ Controller delegates to Service
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 3: UUID Parsed (Service layer)                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ uId = UUID(a1b2c3d4-e5f6-7890-abcd-ef1234567890)                           │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ userRepository.findById(uId)
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 4: Database SELECT                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ SQL: SELECT * FROM users WHERE user_id = 'a1b2c3d4-...'                     │
│ Result: Row found → Hibernate hydrates User entity                         │
│ existingUser = User {                                                       │
│   id=a1b2c3d4-..., email="aditya@gmail.com", name="Aditya",                │
│   password="Secret123", image="https://example.com/aditya.jpg",             │
│   enable=true, provider=LOCAL, createdAt=2026-06-20T10:30:00Z, ...         │
│ }                                                                           │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ Null-check partial updates applied
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 5: Entity Modified (In-memory)                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ existingUser.name      = "Aditya" → "Aditya Kumar"       ✓ CHANGED         │
│ existingUser.image     = "...aditya.jpg" → "...aditya-new.jpg"  ✓ CHANGED  │
│ existingUser.provider  = LOCAL → LOCAL                    (no real change)  │
│ existingUser.password  = "Secret123"                      (skipped, null)   │
│ existingUser.enable    = true → true                      (no real change)  │
│ existingUser.updatedAt = 2026-06-20... → 2026-06-27...    ✓ CHANGED         │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ userRepository.save(existingUser)
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 6: Database UPDATE                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ @PreUpdate fires: updatedAt = Instant.now()                                 │
│ SQL: UPDATE users SET name='Aditya Kumar', image='...new.jpg',              │
│      updated_at='2026-06-27T15:18:00Z', ... WHERE user_id='a1b2c3d4-...'   │
│ Returns: same managed User entity (updatedUser)                             │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ modelMapper.map(updatedUser, UserDto.class)
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 7: Entity → DTO Conversion                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ UserDto {                                                                   │
│   id="a1b2c3d4-...", email="aditya@gmail.com", name="Aditya Kumar",        │
│   password="Secret123", image="https://example.com/aditya-new.jpg",         │
│   enable=true, provider=LOCAL, createdAt=2026-06-20T10:30:00Z,              │
│   updatedAt=2026-06-27T15:18:00Z, roles=[]                                 │
│ }                                                                           │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ ResponseEntity.ok(userDto)
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 8: ResponseEntity Created                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ ResponseEntity { status=200, body=UserDto{...} }                            │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              ↓ Jackson serialization
┌─────────────────────────────────────────────────────────────────────────────┐
│ STAGE 9: Final HTTP Response                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ HTTP/1.1 200 OK                                                             │
│ Content-Type: application/json                                              │
│ {"id":"a1b2c3d4-...","email":"aditya@gmail.com","name":"Aditya Kumar",...}  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Annotation Explanations in Context

Every annotation that participates in this API's execution, explained where it appears:

### Controller Annotations

| Annotation | Where | Purpose | Framework |
|-----------|-------|---------|-----------|
| `@RestController` | `UserController` class | Marks as web controller + auto-serializes return values to JSON | Spring MVC |
| `@RequestMapping("/api/v1/users")` | `UserController` class | Base URL prefix for all methods | Spring MVC |
| `@AllArgsConstructor` | `UserController` class | Generates constructor for dependency injection | Lombok |
| `@PutMapping("/{userId}")` | `updateUser()` method | Maps HTTP PUT + path template to this method | Spring MVC |
| `@RequestBody` | `userDto` parameter | Deserialize HTTP body JSON → Java object | Spring MVC |
| `@PathVariable("userId")` | `userId` parameter | Extract named segment from URL path | Spring MVC |

### Service Annotations

| Annotation | Where | Purpose | Framework |
|-----------|-------|---------|-----------|
| `@Service` | `UserServiceImpl` class | Register as Spring bean (business logic stereotype) | Spring Core |
| `@RequiredArgsConstructor` | `UserServiceImpl` class | Generate constructor for all `final` fields | Lombok |
| `@Override` | `updateUser()` method | Compile-time check against interface contract | Java |

### Entity Annotations

| Annotation | Where | Purpose | Framework |
|-----------|-------|---------|-----------|
| `@Entity` | `User` class | Marks as JPA entity (mapped to database table) | JPA |
| `@Table(name="users")` | `User` class | Specifies the database table name | JPA |
| `@Id` | `id` field | Marks this field as the primary key | JPA |
| `@GeneratedValue(strategy=UUID)` | `id` field | Auto-generate UUID for new entities | JPA/Hibernate |
| `@Column(name="user_id")` | `id` field | Maps field to specific column name | JPA |
| `@Column(name="user_email", unique=true, length=300)` | `email` field | Column mapping with uniqueness constraint | JPA |
| `@Enumerated(EnumType.STRING)` | `provider` field | Store enum as string (not ordinal integer) | JPA |
| `@ManyToMany(fetch=FetchType.EAGER)` | `roles` field | Many-to-many relationship, loaded immediately | JPA |
| `@JoinTable(...)` | `roles` field | Specifies the join table and foreign key columns | JPA |
| `@PreUpdate` | `onUpdate()` method | JPA lifecycle callback — runs before SQL UPDATE | JPA |
| `@PrePersist` | `onCreate()` method | JPA lifecycle callback — runs before SQL INSERT (not used in update) | JPA |
| `@Getter` / `@Setter` | `User` class | Generate all getters and setters | Lombok |
| `@AllArgsConstructor` / `@NoArgsConstructor` | `User` class | Generate both constructors | Lombok |
| `@Builder` | `User` class | Generate builder pattern | Lombok |

### DTO Annotations

| Annotation | Where | Purpose | Framework |
|-----------|-------|---------|-----------|
| `@Getter` / `@Setter` | `UserDto` class | Generate getters/setters (needed by Jackson and ModelMapper) | Lombok |
| `@AllArgsConstructor` / `@NoArgsConstructor` | `UserDto` class | Constructors (Jackson uses no-arg, ModelMapper uses no-arg) | Lombok |
| `@Builder` | `UserDto` class | Builder pattern for programmatic construction | Lombok |

### Exception Handler Annotations

| Annotation | Where | Purpose | Framework |
|-----------|-------|---------|-----------|
| `@RestControllerAdvice` | `GlobalExceptionHandler` class | Global exception handler for all controllers, auto-serializes to JSON | Spring MVC |
| `@ExceptionHandler(ResourceNotFoundException.class)` | `handleResourceNotFoundException()` | Catches this specific exception type | Spring MVC |
| `@ExceptionHandler(IllegalArgumentException.class)` | `handleIllegalArgumentException()` | Catches malformed UUID exceptions | Spring MVC |

### Configuration Annotations

| Annotation | Where | Purpose | Framework |
|-----------|-------|---------|-----------|
| `@Configuration` | `ProjectConfig` class | Marks as Spring configuration class | Spring Core |
| `@Bean` | `modelMapper()` method | Registers the return value as a Spring-managed bean | Spring Core |

---

## 8. Summary

### Execution Flow (Method Call Order)

```
1.  Tomcat receives PUT /api/v1/users/{userId}
2.  DispatcherServlet maps request → UserController.updateUser()
3.  Jackson deserializes JSON body → UserDto object
4.  PathVariableResolver extracts userId → String
5.  UserController.updateUser(userDto, userId) executes
6.    → userService.updateUser(userDto, userId) called
7.    → UserHelper.parseUUID(userId) → UUID
8.    → userRepository.findById(uuid) → SQL SELECT → Optional<User>
9.    → .orElseThrow() → User (or throws ResourceNotFoundException)
10.   → Null-check: setName() if name != null
11.   → Null-check: setImage() if image != null
12.   → Null-check: setProvider() if provider != null
13.   → Null-check: setPassword() if password != null
14.   → setEnable() — always applied (primitive boolean)
15.   → setUpdatedAt(Instant.now()) — manual timestamp
16.   → userRepository.save(existingUser) → @PreUpdate fires → SQL UPDATE
17.   → modelMapper.map(updatedUser, UserDto.class) → UserDto
18. ResponseEntity.ok(userDto) → 200 OK
19. Jackson serializes UserDto → JSON response body
20. Client receives HTTP 200 + JSON
```

### Objects Created During Execution

| Object | Created By | Purpose | Lifecycle |
|--------|-----------|---------|-----------|
| `UserDto` (input) | Jackson `ObjectMapper` | Holds deserialized request data | Method scope |
| `UUID uId` | `UUID.fromString()` | Parsed path variable | Method scope |
| `User existingUser` | Hibernate (from DB) | JPA managed entity | PersistenceContext |
| `Instant` | `Instant.now()` | Current timestamp | Field assignment |
| `User updatedUser` | Same reference as `existingUser` | Post-save entity | Method scope |
| `UserDto` (output) | `ModelMapper` | Response DTO | Serialized to JSON |
| `ResponseEntity` | `ResponseEntity.ok()` | HTTP response wrapper | Serialized by DispatcherServlet |

### Methods Called (in order)

| # | Method | Class | Input | Output |
|---|--------|-------|-------|--------|
| 1 | `updateUser()` | `UserController` | `UserDto`, `String` | `ResponseEntity<UserDto>` |
| 2 | `updateUser()` | `UserServiceImpl` | `UserDto`, `String` | `UserDto` |
| 3 | `parseUUID()` | `UserHelper` | `String` | `UUID` |
| 4 | `findById()` | `UserRepository` (proxy) | `UUID` | `Optional<User>` |
| 5 | `orElseThrow()` | `Optional<User>` | `Supplier<Exception>` | `User` |
| 6 | `getName()` | `UserDto` (Lombok) | — | `String` or `null` |
| 7 | `setName()` | `User` (Lombok) | `String` | `void` |
| 8 | `getImage()` | `UserDto` (Lombok) | — | `String` or `null` |
| 9 | `setImage()` | `User` (Lombok) | `String` | `void` |
| 10 | `getProvider()` | `UserDto` (Lombok) | — | `Provider` or `null` |
| 11 | `setProvider()` | `User` (Lombok) | `Provider` | `void` |
| 12 | `getPassword()` | `UserDto` (Lombok) | — | `String` or `null` |
| 13 | `isEnable()` | `UserDto` (Lombok) | — | `boolean` |
| 14 | `setEnable()` | `User` (Lombok) | `boolean` | `void` |
| 15 | `setUpdatedAt()` | `User` (Lombok) | `Instant` | `void` |
| 16 | `save()` | `UserRepository` (proxy) | `User` | `User` |
| 17 | `onUpdate()` | `User` (`@PreUpdate`) | — | `void` |
| 18 | `map()` | `ModelMapper` | `User`, `Class<UserDto>` | `UserDto` |
| 19 | `ok()` | `ResponseEntity` (static) | `UserDto` | `ResponseEntity<UserDto>` |

### Database Operations

| # | Operation | SQL | When |
|---|-----------|-----|------|
| 1 | **SELECT** (find user) | `SELECT * FROM users WHERE user_id = ?` | `findById()` |
| 2 | **SELECT** (eager fetch roles) | `SELECT * FROM user_roles ur JOIN roles r ON ... WHERE ur.user_id = ?` | `findById()` (EAGER) |
| 3 | **UPDATE** (save changes) | `UPDATE users SET name=?, image=?, updated_at=?, ... WHERE user_id = ?` | `save()` |

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Email is never updated** | Business rule — email is the user's identity. Source comment: *"We are not going to chanege email id"* |
| **Null-check partial update** | Only fields explicitly sent by the client are updated. Prevents accidental data loss |
| **`boolean enable` (not `Boolean`)** | Always overwritten. Trade-off: simpler code, but risks unintended re-enabling |
| **`provider` default = `LOCAL`** | Can silently change provider on partial updates — potential bug |
| **No `@Transactional`** | Relies on `save()` to persist. If `@Transactional` were present, dirty checking would auto-flush |
| **Password stored in plain text** | Marked with `//TODO` — not production-ready. Should use `BCryptPasswordEncoder` |
| **Password returned in response** | Security issue — should use `@JsonIgnore` or exclude from DTO |
| **Manual + `@PreUpdate` timestamp** | Redundant but harmless — `@PreUpdate` has final say on `updatedAt` value |
| **UUID as String in controller** | Parsed manually in service for custom error handling via `UserHelper` |

### Final Response

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "aditya@gmail.com",
  "name": "Aditya Kumar",
  "password": "Secret123",
  "image": "https://example.com/aditya-new.jpg",
  "enable": true,
  "createdAt": "2026-06-20T10:30:00Z",
  "updatedAt": "2026-06-27T15:18:00Z",
  "provider": "LOCAL",
  "roles": []
}
```

---

> **Document Version**: 1.0
> **API**: `PUT /api/v1/users/{userId}`
> **Last Updated**: 2026-06-27
