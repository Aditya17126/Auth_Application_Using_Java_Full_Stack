# GET USER BY ID — Complete API Execution Trace

## Document Overview

This document traces **every single line of code** that executes when a client sends a `GET /api/v1/users/{userId}` request. We follow the request from the moment it hits the embedded Tomcat server, through the Controller → Service → Helper → Repository → Entity → DTO layers, and back to the client as a JSON response.

> **Real-World Scenario:** Aditya (a frontend developer) wants to fetch a specific user's profile from the backend. He knows the user's UUID is `a1b2c3d4-e5f6-7890-abcd-ef1234567890` and sends a GET request to retrieve the full user object.

---

## 1. API Endpoint Details

| Property         | Value                                                        |
| ---------------- | ------------------------------------------------------------ |
| **HTTP Method**  | `GET`                                                        |
| **URL**          | `/api/v1/users/{userId}`                                     |
| **Path Variable**| `userId` — a `String` representation of a UUID               |
| **Request Body** | ❌ None (GET requests do not have a body)                     |
| **Content-Type** | Not required (no body)                                       |
| **Auth Required**| ❌ No — Spring Security is **commented out**                  |
| **Response Code**| `200 OK` (success) or `404 NOT FOUND` (user doesn't exist)   |

### Sample Request

```text
GET http://localhost:8080/api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

No headers are strictly required since security is disabled.

### Success Response — `200 OK`

```json
{
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "aditya@gmail.com",
    "name": "Aditya",
    "password": "$2a$10$hashedPasswordValueHere",
    "image": "https://example.com/aditya.jpg",
    "enable": true,
    "createdAt": "2026-06-27T10:30:00Z",
    "updatedAt": "2026-06-27T10:30:00Z",
    "provider": "LOCAL",
    "roles": [
        {
            "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            "name": "ROLE_USER"
        }
    ]
}
```

### Failure Response — `404 NOT FOUND`

```json
{
    "message": "User not found with the given id",
    "status": "NOT_FOUND",
    "statusCode": 404
}
```

### Failure Response — `400 BAD REQUEST` (Malformed UUID)

If the `userId` is not a valid UUID string (e.g., `"not-a-uuid"`), a `400 Bad Request` or `500 Internal Server Error` is returned because `UUID.fromString()` throws an `IllegalArgumentException`.

---

## 2. Real-World Example Scenario

```text
┌─────────────────────────────────────────────────────────────────────┐
│  SCENARIO: Aditya wants to view a user's profile page              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  WHO:   Aditya — a frontend developer building a React dashboard    │
│  WHAT:  He clicks on a user in the Users table                      │
│  WHY:   To view the full profile details of that specific user      │
│  HOW:   React app sends GET /api/v1/users/{userId}                  │
│                                                                     │
│  The UUID "a1b2c3d4-e5f6-7890-abcd-ef1234567890" was already       │
│  known because it came from a previous "Get All Users" API call.    │
│                                                                     │
│  EXPECTED RESULT:                                                   │
│    → 200 OK with full user JSON (name, email, roles, etc.)          │
│    → OR 404 if the UUID doesn't match any user in the database      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Frontend Code (React — what Aditya writes)

```javascript
const fetchUser = async (userId) => {
    const response = await fetch(`http://localhost:8080/api/v1/users/${userId}`);
    if (!response.ok) {
        throw new Error("User not found");
    }
    const userData = await response.json();
    console.log(userData); // { id: "a1b2c3d4-...", name: "Aditya", ... }
};

fetchUser("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
```

---

## 3. Security Filter Chain Execution

> **IMPORTANT:** Spring Security is **COMMENTED OUT** in this project. There is no `SecurityFilterChain` bean, no `JwtAuthFilter`, and no authentication/authorization enforcement.

### What This Means

```text
Client sends GET /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
         ↓
Tomcat receives the HTTP request
         ↓
Spring Boot's default DispatcherServlet handles routing
         ↓
NO security filters execute — no JWT check, no role check
         ↓
Request goes DIRECTLY to the Controller
```

### Why Does the Request Still Reach the Controller?

When Spring Security is on the classpath but **no `SecurityFilterChain` bean** is registered and security auto-configuration is excluded or commented out:

1. **No `springSecurityFilterChain`** is created in the `ApplicationFilterChain`.
2. Tomcat's default servlet pipeline passes the request straight through.
3. Spring's `DispatcherServlet` picks up the request and matches it to a `@RestController`.

### What Would Change If Security Were Enabled?

| Step | Without Security (Current) | With Security (Future) |
|------|---------------------------|----------------------|
| 1 | Request hits Tomcat | Request hits Tomcat |
| 2 | Goes to DispatcherServlet | Goes to `springSecurityFilterChain` |
| 3 | Matches controller method | `JwtAuthFilter` extracts JWT from `Authorization` header |
| 4 | Executes controller | Validates token, sets `SecurityContext` |
| 5 | — | If valid → DispatcherServlet → Controller |
| 6 | — | If invalid → `401 Unauthorized` immediately |

---

## 4. Complete Step-by-Step Execution Flow

### High-Level Flow Diagram

```text
┌──────────────────┐
│   HTTP Client     │  GET /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
│   (Postman/React) │
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│  Embedded Tomcat  │  Receives raw HTTP request on port 8080
│  (Servlet Container)│
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│ DispatcherServlet │  Spring's front controller — routes to correct handler
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│  HandlerMapping   │  Matches URL pattern /api/v1/users/{userId}
│                   │  to UserController.getUserById()
└────────┬─────────┘
         │
         ↓
┌──────────────────────────────────────────────────────────────┐
│  UserController.getUserById("a1b2c3d4-e5f6-7890-abcd-ef...") │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  userService.getUserById("a1b2c3d4-e5f6-7890-...")      │ │
│  │  ┌──────────────────────────────────────────────────────┐│ │
│  │  │ UserHelper.parseUUID("a1b2c3d4-e5f6-7890-...")      ││ │
│  │  │ → Returns UUID object                                ││ │
│  │  └──────────────────────────────────────────────────────┘│ │
│  │  ┌──────────────────────────────────────────────────────┐│ │
│  │  │ userRepository.findById(UUID)                        ││ │
│  │  │ → Hibernate generates: SELECT * FROM users           ││ │
│  │  │   WHERE user_id = ?                                  ││ │
│  │  │ → Returns Optional<User>                             ││ │
│  │  └──────────────────────────────────────────────────────┘│ │
│  │  ┌──────────────────────────────────────────────────────┐│ │
│  │  │ .orElseThrow(() -> new ResourceNotFoundException())  ││ │
│  │  │ → If empty: throws exception                         ││ │
│  │  │ → If present: returns User entity                    ││ │
│  │  └──────────────────────────────────────────────────────┘│ │
│  │  ┌──────────────────────────────────────────────────────┐│ │
│  │  │ modelMapper.map(user, UserDto.class)                 ││ │
│  │  │ → Converts User entity to UserDto                    ││ │
│  │  └──────────────────────────────────────────────────────┘│ │
│  └──────────────────────────────────────────────────────────┘ │
│  ResponseEntity.ok(userDto) → wraps in 200 OK                 │
└────────┬─────────────────────────────────────────────────────┘
         │
         ↓
┌──────────────────┐
│ HttpMessageConverter │  Jackson's ObjectMapper serializes UserDto → JSON
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│   HTTP Response   │  200 OK + JSON body sent to client
└──────────────────┘
```

---

### Step 1: Tomcat Receives the HTTP Request

```text
Raw HTTP:
GET /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

**What happens internally:**

1. Tomcat's `Connector` on port 8080 accepts the TCP connection.
2. It parses the raw HTTP bytes into a `HttpServletRequest` object.
3. The request is passed to the `DispatcherServlet` (the single front-controller in Spring MVC).

**What the `HttpServletRequest` object looks like:**

| Property        | Value                                                    |
| --------------- | -------------------------------------------------------- |
| `method`        | `"GET"`                                                  |
| `requestURI`    | `"/api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890"`  |
| `contextPath`   | `""`                                                     |
| `pathInfo`      | `null`                                                   |
| `queryString`   | `null`                                                   |

---

### Step 2: DispatcherServlet Routes the Request

Spring's `DispatcherServlet.doDispatch()` is called:

1. It consults the `RequestMappingHandlerMapping` to find a handler.
2. It looks through all registered `@RequestMapping` methods.
3. It finds a match:

```text
URL Pattern: /api/v1/users/{userId}
         ↓ matches ↓
Controller: UserController
Method:     getUserById(@PathVariable("userId") String userId)
```

**How the match works:**

- The class-level `@RequestMapping("/api/v1/users")` provides the prefix `/api/v1/users`.
- The method-level `@GetMapping("/{userId}")` adds `/{userId}` to the prefix.
- Combined pattern: `GET /api/v1/users/{userId}`.
- The path segment `a1b2c3d4-e5f6-7890-abcd-ef1234567890` is extracted and bound to the `{userId}` variable.

**Path Variable Extraction:**

```text
URL:     /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Pattern: /api/v1/users/{userId}

Extracted: userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890" (as String)
```

---

### Step 3: UserController — The Entry Point

```java
@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
   private final UserService userService;

   @GetMapping("/{userId}")
   public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId){
      return ResponseEntity.ok(userService.getUserById(userId));
   }
}
```

#### Line-by-Line Execution:

---

#### `@RestController`

```java
@RestController
```

- **What it is:** A combination of `@Controller` + `@ResponseBody`.
- **Why it executes:** At application startup, Spring's component scanning finds this annotation and registers `UserController` as a Spring-managed bean in the ApplicationContext.
- **What it does at request time:** It tells Spring that every method's return value should be serialized directly into the HTTP response body (as JSON, by default) rather than being resolved as a view name.
- **What breaks if removed:** Spring would not recognize this class as a web controller. The `/api/v1/users/{userId}` endpoint would not exist. Client gets `404 Not Found`.

---

#### `@RequestMapping("/api/v1/users")`

```java
@RequestMapping("/api/v1/users")
```

- **What it is:** A class-level URL prefix. Every method-level mapping inside this class is relative to this path.
- **Why it executes:** During startup, `RequestMappingHandlerMapping` reads this annotation and prepends `/api/v1/users` to all method-level paths.
- **What it does:** Sets the base path for all endpoints in this controller.
- **What breaks if removed:** The method-level `@GetMapping("/{userId}")` would map to just `GET /{userId}` at the root. Aditya would need to call `GET /a1b2c3d4-e5f6-7890-abcd-ef1234567890` instead.

---

#### `@AllArgsConstructor`

```java
@AllArgsConstructor
```

- **What it is:** A Lombok annotation that generates a constructor accepting all `final` fields as parameters.
- **Why it executes:** At compile time, Lombok generates:

```java
public UserController(UserService userService) {
    this.userService = userService;
}
```

- **What it does:** This generated constructor is what Spring uses for **constructor injection**. Spring sees that the constructor needs a `UserService` bean, finds `UserServiceImpl` (the only implementation, annotated with `@Service`), and injects it.
- **What breaks if removed:** No constructor is generated. Spring cannot inject `UserService`. Application fails to start with:

```
Parameter 0 of constructor in UserController required a bean of type 'UserService' that could not be found.
```

---

#### `private final UserService userService;`

```java
private final UserService userService;
```

- **What it is:** A field declaration of type `UserService` (an interface).
- **Why `final`:** (1) It ensures the field is immutable after construction. (2) It signals to Lombok's `@AllArgsConstructor` to include this field in the generated constructor. (3) It's a best practice for dependency injection — guarantees the service is never `null` after construction.
- **What object is assigned:** At startup, Spring injects `UserServiceImpl` (since it's the only bean implementing `UserService`, annotated with `@Service`).
- **What breaks if removed:** There's no reference to call `userService.getUserById()`. Compilation fails.

---

#### `@GetMapping("/{userId}")`

```java
@GetMapping("/{userId}")
```

- **What it is:** A shortcut for `@RequestMapping(method = RequestMethod.GET, path = "/{userId}")`.
- **Why it executes:** At startup, Spring registers this method as the handler for `GET /api/v1/users/{userId}`.
- **What `{userId}` means:** It's a **URI template variable**. The actual path segment in the URL (e.g., `a1b2c3d4-e5f6-7890-abcd-ef1234567890`) is captured and made available as a variable named `userId`.
- **What breaks if removed:** No handler is registered for this URL. Client gets `404 Not Found`.

---

#### `@PathVariable("userId") String userId`

```java
public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId)
```

- **What it is:** Tells Spring to extract the value of the URI template variable `{userId}` and bind it to the method parameter `userId`.
- **What value it receives:** `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` (as a `String`).
- **Why `String` and not `UUID`:** The developer chose to receive it as `String` and convert it manually using `UserHelper.parseUUID()`. This gives more control over error handling during the UUID parsing step.
- **Why `"userId"` is specified explicitly:** The annotation value `"userId"` must match the `{userId}` in `@GetMapping("/{userId}")`. If they don't match, Spring throws an error. (Note: if the parameter name matches the template variable name AND the code is compiled with `-parameters` flag, the explicit value can be omitted.)
- **What breaks if removed:** Spring cannot bind the path segment to any parameter. It would throw:

```
Missing URI template variable 'userId' for method parameter of type String
```

---

#### `return ResponseEntity.ok(userService.getUserById(userId));`

```java
return ResponseEntity.ok(userService.getUserById(userId));
```

This single line does **three things** in sequence:

1. **Calls `userService.getUserById(userId)`** — passes `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` to the service layer. This returns a `UserDto` object.
2. **Calls `ResponseEntity.ok(...)`** — wraps the `UserDto` in a `ResponseEntity` with HTTP status `200 OK`.
3. **Returns the `ResponseEntity<UserDto>`** — Spring's `HttpMessageConverter` (Jackson) serializes it to JSON.

**What `ResponseEntity.ok()` does internally:**

```java
// Inside ResponseEntity class:
public static <T> ResponseEntity<T> ok(T body) {
    return new ResponseEntity<>(body, HttpStatus.OK);  // status = 200
}
```

It creates a `ResponseEntity` with:
- Status: `200 OK`
- Body: the `UserDto` object
- Headers: default (Content-Type will be set to `application/json` by Jackson)

**What breaks if removed:** There's no return statement. Compilation fails because the method signature promises a `ResponseEntity<UserDto>`.

---

### Step 4: UserServiceImpl — Business Logic Layer

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final ModelMapper modelMapper;

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId))
            .orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"));
        return modelMapper.map(user, UserDto.class);
    }
}
```

#### Line-by-Line Execution:

---

#### `@Service`

```java
@Service
```

- **What it is:** A Spring stereotype annotation. It's a specialization of `@Component`.
- **Why it executes:** At startup, Spring's component scanning detects this annotation and registers `UserServiceImpl` as a singleton bean in the ApplicationContext.
- **What it does:** Marks this class as a **service-layer** component. This is semantically meaningful — it tells other developers this class contains business logic.
- **What breaks if removed:** Spring doesn't create a bean for `UserServiceImpl`. When `UserController` tries to inject `UserService`, it fails:

```
No qualifying bean of type 'UserService' available
```

---

#### `@RequiredArgsConstructor`

```java
@RequiredArgsConstructor
```

- **What it is:** A Lombok annotation that generates a constructor for all `final` fields and fields annotated with `@NonNull`.
- **Difference from `@AllArgsConstructor`:** `@AllArgsConstructor` generates a constructor for **ALL** fields. `@RequiredArgsConstructor` generates a constructor for only `final` and `@NonNull` fields. In this case, both `userRepository` and `modelMapper` are `final`, so the result is the same.
- **Generated constructor:**

```java
public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
    this.userRepository = userRepository;
    this.modelMapper = modelMapper;
}
```

- **What breaks if removed:** No constructor is generated. Spring cannot inject the dependencies. Application fails to start.

---

#### `private final UserRepository userRepository;`

```java
private final UserRepository userRepository;
```

- **What it is:** A reference to the JPA repository for the `User` entity.
- **What Spring injects:** At startup, Spring Data JPA automatically creates a **proxy implementation** of the `UserRepository` interface (using `SimpleJpaRepository` under the hood) and injects it here.
- **Why `final`:** Immutability + enables Lombok constructor generation.
- **What breaks if removed:** Cannot call `userRepository.findById()`. Compilation fails.

---

#### `private final ModelMapper modelMapper;`

```java
private final ModelMapper modelMapper;
```

- **What it is:** A reference to the `ModelMapper` bean, used for object-to-object mapping (Entity → DTO).
- **Where the bean comes from:** `ProjectConfig.java` defines it:

```java
@Configuration
public class ProjectConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
```

- **What Spring injects:** The `ModelMapper` singleton instance created by `ProjectConfig`.
- **What breaks if removed:** Cannot call `modelMapper.map()`. Compilation fails.

---

#### `public UserDto getUserById(String userId)`

```java
@Override
public UserDto getUserById(String userId) {
```

- **`@Override`:** Indicates this method implements the `getUserById` method declared in the `UserService` interface. If the interface method signature changes, the compiler will catch it.
- **Parameter received:** `userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"` (String, passed from the controller).
- **Return type:** `UserDto` — the data transfer object that will be serialized to JSON.

---

#### `UserHelper.parseUUID(userId)` — The UUID Parsing Step

```java
User user = userRepository.findById(UserHelper.parseUUID(userId))
```

This is the **first operation** inside the method. Let's go deep into `UserHelper.parseUUID()`.

##### UserHelper.java — Full Analysis

```java
package com.substring.auth.auth_app.helpers;
import java.util.UUID;

public class UserHelper {
   public static UUID parseUUID(String uuid) {
      return UUID.fromString(uuid);
   }
}
```

**Line: `public class UserHelper`**

- **What it is:** A utility/helper class. It has no Spring annotations — it's a plain Java class.
- **Why it's not a Spring bean:** It only contains `static` methods, so there's no need for Spring to manage an instance. You call it directly via the class name: `UserHelper.parseUUID(...)`.

**Line: `public static UUID parseUUID(String uuid)`**

- **What it is:** A static method that converts a `String` to a `java.util.UUID` object.
- **Why it's static:** No instance state is needed. Any caller can invoke it without creating a `UserHelper` object.
- **Parameter received:** `uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"`

**Line: `return UUID.fromString(uuid);`**

- **What it does:** Calls Java's built-in `UUID.fromString()` method.
- **What `UUID.fromString()` does internally:**
  1. Validates the format: the string must be 36 characters in the format `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` (8-4-4-4-12 hex digits separated by hyphens).
  2. Parses the hex characters into two `long` values (`mostSigBits` and `leastSigBits`).
  3. Creates and returns a new `UUID` object.

**Parsing breakdown:**

```text
Input:  "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
         ↓ UUID.fromString() ↓
Split:  "a1b2c3d4" - "e5f6" - "7890" - "abcd" - "ef1234567890"
         ↓ hex parsing ↓
mostSigBits:   0xa1b2c3d4e5f67890L
leastSigBits:  0xabcdef1234567890L
         ↓ construct ↓
Output: UUID object { mostSigBits, leastSigBits }
```

**What happens on SUCCESS:**
- Returns a valid `UUID` object: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- Execution continues to `userRepository.findById(...)`.

**What happens on FAILURE (invalid UUID string):**

If `userId = "not-a-uuid"`:

```java
UUID.fromString("not-a-uuid")
// throws: java.lang.IllegalArgumentException: Invalid UUID string: not-a-uuid
```

This exception propagates up through the call stack:
1. `UserHelper.parseUUID()` throws `IllegalArgumentException`
2. `UserServiceImpl.getUserById()` does NOT catch it
3. `UserController.getUserById()` does NOT catch it
4. Spring's exception handling catches it
5. Since `GlobalExceptionHandler` has no `@ExceptionHandler(IllegalArgumentException.class)`, Spring returns a generic `500 Internal Server Error`.

**Why this helper class exists:**
- **Centralized parsing:** All UUID parsing goes through one place. If you later want to add custom validation (e.g., logging, custom error messages), you change one method.
- **Readability:** `UserHelper.parseUUID(userId)` is more descriptive than `UUID.fromString(userId)`.
- **What breaks if removed:** You'd need to call `UUID.fromString(userId)` directly in the service. The code still works but loses the abstraction.

---

#### `userRepository.findById(UUID)` — Database Query

```java
User user = userRepository.findById(UserHelper.parseUUID(userId))
```

After `UserHelper.parseUUID()` returns a `UUID` object, it's passed to `userRepository.findById()`.

##### UserRepository.java — Full Analysis

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

**Line: `extends JpaRepository<User, UUID>`**

- **What it is:** `JpaRepository` is a Spring Data JPA interface that provides CRUD operations.
- **`<User, UUID>`:** The first generic is the entity type (`User`), the second is the primary key type (`UUID`).
- **What Spring does at startup:** Spring Data JPA creates a **runtime proxy** class that implements `UserRepository`. This proxy contains actual implementations of all CRUD methods (`findById`, `save`, `delete`, etc.).
- **Where does `findById()` come from?** It's defined in `CrudRepository` (parent of `JpaRepository`):

```java
// Inside CrudRepository:
Optional<T> findById(ID id);
```

For our repository, this becomes: `Optional<User> findById(UUID id)`.

**What `findById(UUID)` does internally:**

1. Spring Data JPA's proxy implementation calls `EntityManager.find(User.class, uuid)`.
2. Hibernate (the JPA provider) generates the following SQL:

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

3. **Because `roles` has `FetchType.EAGER`**, Hibernate also fetches the roles in a second query (or a JOIN):

```sql
SELECT 
    r.role_id,
    r.name
FROM user_roles ur
JOIN roles r ON ur.role_id = r.role_id  -- corrected: r.id
WHERE ur.user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

4. Hibernate maps the result set into a `User` entity object.
5. The result is wrapped in an `Optional<User>`.

**Two possible outcomes:**

| Scenario | Database Result | `Optional<User>` Value |
|----------|----------------|----------------------|
| User EXISTS | Row found | `Optional.of(userEntity)` |
| User DOES NOT EXIST | No row found | `Optional.empty()` |

---

#### `.orElseThrow(...)` — The Guard Clause

```java
.orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"));
```

- **What it is:** A method on `Optional<User>` that either unwraps the value or throws an exception.
- **What `() -> new ResourceNotFoundException(...)` is:** A Java **lambda expression** that acts as a `Supplier<ResourceNotFoundException>`. It's a function that creates the exception **only if needed** (lazy instantiation).

**Two possible outcomes:**

##### ✅ SUCCESS PATH — User Found

```text
Optional.of(userEntity)  →  .orElseThrow(...)  →  Returns the User entity

The lambda is NEVER executed. The exception is NEVER created.
Variable 'user' now holds the User entity object.
```

The `User` object looks like:

```java
User {
    id       = UUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
    email    = "aditya@gmail.com",
    name     = "Aditya",
    password = "$2a$10$hashedPasswordValueHere",
    image    = "https://example.com/aditya.jpg",
    enable   = true,
    createdAt = Instant("2026-06-27T10:30:00Z"),
    updatedAt = Instant("2026-06-27T10:30:00Z"),
    provider = Provider.LOCAL,
    roles    = Set { Role("ROLE_USER") }
}
```

##### ❌ FAILURE PATH — User Not Found

```text
Optional.empty()  →  .orElseThrow(...)  →  Executes the lambda
                                          →  Creates new ResourceNotFoundException("User not found with the given id")
                                          →  THROWS the exception
```

Execution **immediately stops** in `getUserById()`. The exception propagates up the call stack. See [Section 4 — Failure Flow](#failure-flow-user-not-found) for the complete trace.

---

#### `modelMapper.map(user, UserDto.class)` — Entity to DTO Conversion

```java
return modelMapper.map(user, UserDto.class);
```

This line only executes on the **SUCCESS PATH** (user was found).

**What it does:**

1. **`modelMapper`** is the `ModelMapper` bean injected from `ProjectConfig`.
2. **`.map(user, UserDto.class)`** converts a `User` entity into a `UserDto` object.

**How ModelMapper works internally:**

1. It inspects the **source** object (`User`) using reflection and finds all getter methods.
2. It inspects the **destination** class (`UserDto`) using reflection and finds all setter methods.
3. It matches fields by name (convention-based mapping):

```text
User (Source)                  →    UserDto (Destination)
─────────────────────────────────────────────────────────
user.getId()       → UUID     →    userDto.setId(UUID)
user.getEmail()    → String   →    userDto.setEmail(String)
user.getName()     → String   →    userDto.setName(String)
user.getPassword() → String   →    userDto.setPassword(String)
user.getImage()    → String   →    userDto.setImage(String)
user.isEnable()    → boolean  →    userDto.setEnable(boolean)
user.getCreatedAt()→ Instant  →    userDto.setCreatedAt(Instant)
user.getUpdatedAt()→ Instant  →    userDto.setUpdatedAt(Instant)
user.getProvider() → Provider →    userDto.setProvider(Provider)
user.getRoles()    → Set<Role>→    userDto.setRoles(Set<Role>)
```

4. ModelMapper creates a **new `UserDto` instance** (using the no-args constructor from `@NoArgsConstructor`).
5. It calls each setter on the new `UserDto` with the corresponding value from the `User` entity.

**The resulting `UserDto` object:**

```java
UserDto {
    id        = UUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
    email     = "aditya@gmail.com",
    name      = "Aditya",
    password  = "$2a$10$hashedPasswordValueHere",
    image     = "https://example.com/aditya.jpg",
    enable    = true,
    createdAt = Instant("2026-06-27T10:30:00Z"),
    updatedAt = Instant("2026-06-27T10:30:00Z"),
    provider  = Provider.LOCAL,
    roles     = Set { Role("ROLE_USER") }
}
```

> **⚠️ Security Note:** The `password` field is included in the DTO. This means the hashed password is returned in the API response. In production, you should either:
> - Remove the `password` field from `UserDto`, OR
> - Use `@JsonIgnore` on the password field, OR
> - Create a separate `UserResponseDto` without the password.

**What breaks if this line is removed:**
- The method has no return statement. Compilation fails.
- Even if you returned the `User` entity directly, you'd tightly couple your API response to your database schema — any database column change would break your API contract.

---

### Step 5: Response Serialization (Jackson)

After `getUserById()` returns, the flow goes back up the call stack:

```text
UserServiceImpl.getUserById()  →  returns UserDto
         ↑
UserController.getUserById()   →  wraps in ResponseEntity.ok(userDto)
         ↑
DispatcherServlet              →  calls HttpMessageConverter
         ↑
MappingJackson2HttpMessageConverter  →  serializes UserDto to JSON
```

**What Jackson does:**

1. It inspects the `UserDto` object.
2. For each getter method, it creates a JSON property (using the field name).
3. It converts Java types to JSON types:

```text
UUID      → "a1b2c3d4-e5f6-7890-abcd-ef1234567890"  (JSON string)
String    → "Aditya"                                   (JSON string)
boolean   → true                                       (JSON boolean)
Instant   → "2026-06-27T10:30:00Z"                     (JSON string, ISO-8601)
Provider  → "LOCAL"                                     (JSON string, enum name)
Set<Role> → [ { "id": "...", "name": "ROLE_USER" } ]   (JSON array)
```

4. The final JSON is written to the `HttpServletResponse` body.

**Response headers set by Spring:**

```text
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 27 Jun 2026 15:18:00 GMT
```

---

## 4.1: Failure Flow — User Not Found

When the UUID exists but doesn't match any user in the database:

```text
GET /api/v1/users/99999999-9999-9999-9999-999999999999
```

### Step-by-Step Failure Trace

```text
Step 1: Tomcat → DispatcherServlet → UserController.getUserById("99999999-9999-9999-9999-999999999999")

Step 2: UserController calls userService.getUserById("99999999-9999-9999-9999-999999999999")

Step 3: UserHelper.parseUUID("99999999-9999-9999-9999-999999999999")
        → SUCCESS: Returns valid UUID object (the format is valid)

Step 4: userRepository.findById(UUID)
        → Hibernate SQL:
          SELECT * FROM users WHERE user_id = '99999999-9999-9999-9999-999999999999'
        → Result: No rows found
        → Returns: Optional.empty()

Step 5: .orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"))
        → Optional is EMPTY
        → Lambda EXECUTES
        → Creates: new ResourceNotFoundException("User not found with the given id")
        → THROWS the exception

Step 6: Exception propagates up:
        UserServiceImpl.getUserById() → throws ResourceNotFoundException
        UserController.getUserById()  → does not catch → re-throws
        DispatcherServlet             → catches it → looks for @ExceptionHandler
```

### Step 7: GlobalExceptionHandler Catches the Exception

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception) {
        ErrorResponse internalServerError = new ErrorResponse(
            exception.getMessage(), HttpStatus.NOT_FOUND, 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
    }
}
```

#### Line-by-Line Execution:

**`@RestControllerAdvice`**

- A combination of `@ControllerAdvice` + `@ResponseBody`.
- `@ControllerAdvice` makes this class a **global exception handler** for all controllers.
- `@ResponseBody` means return values are serialized to JSON (same as `@RestController`).
- Spring registers this class as a bean during startup and consults it whenever an unhandled exception escapes a controller.

**`@ExceptionHandler(ResourceNotFoundException.class)`**

- Tells Spring: "When a `ResourceNotFoundException` is thrown by any controller, call THIS method to handle it."
- Spring matches the thrown exception type (`ResourceNotFoundException`) to this handler.

**`ResourceNotFoundException exception`**

- Spring passes the thrown exception object as the parameter.
- `exception.getMessage()` returns `"User not found with the given id"` (the message we set when creating the exception).

**`new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND, 404)`**

- Creates an `ErrorResponse` record:

```java
public record ErrorResponse(String message, HttpStatus status, int statusCode) {}
```

- The record object:

```java
ErrorResponse {
    message    = "User not found with the given id",
    status     = HttpStatus.NOT_FOUND,
    statusCode = 404
}
```

> **What is a Java `record`?** A `record` is an immutable data carrier introduced in Java 16. It automatically generates `equals()`, `hashCode()`, `toString()`, and accessor methods (e.g., `message()`, `status()`, `statusCode()`). It's similar to a `@Value` Lombok class.

**`ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError)`**

- Creates a `ResponseEntity` with:
  - Status: `404 NOT FOUND`
  - Body: the `ErrorResponse` record

**Jackson serializes the `ErrorResponse` to JSON:**

```json
{
    "message": "User not found with the given id",
    "status": "NOT_FOUND",
    "statusCode": 404
}
```

> **Note:** The variable is named `internalServerError` but the actual status is `NOT_FOUND (404)`. This is a misleading variable name in the code. It should be named something like `errorResponse` or `notFoundError`.

---

## 4.2: Failure Flow — Invalid UUID Format

```text
GET /api/v1/users/not-a-valid-uuid
```

### Step-by-Step Trace

```text
Step 1: Tomcat → DispatcherServlet → UserController.getUserById("not-a-valid-uuid")

Step 2: UserController calls userService.getUserById("not-a-valid-uuid")

Step 3: UserHelper.parseUUID("not-a-valid-uuid")
        → UUID.fromString("not-a-valid-uuid")
        → THROWS: IllegalArgumentException("Invalid UUID string: not-a-valid-uuid")

Step 4: Exception propagates up:
        UserHelper.parseUUID()         → throws IllegalArgumentException
        UserServiceImpl.getUserById()  → does not catch → re-throws
        UserController.getUserById()   → does not catch → re-throws
        DispatcherServlet              → catches it → looks for @ExceptionHandler

Step 5: GlobalExceptionHandler does NOT have:
        @ExceptionHandler(IllegalArgumentException.class)

Step 6: Spring's default error handling kicks in:
        → Returns a generic error response
```

**Default Spring Error Response (no custom handler):**

```json
{
    "timestamp": "2026-06-27T15:18:00.000+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "path": "/api/v1/users/not-a-valid-uuid"
}
```

> **⚠️ Improvement Suggestion:** Add an `@ExceptionHandler(IllegalArgumentException.class)` to `GlobalExceptionHandler` to return a clean `400 Bad Request`:
>
> ```java
> @ExceptionHandler(IllegalArgumentException.class)
> public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
>     ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, 400);
>     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
> }
> ```

---

## 5. Every Line of Code — Why It's Needed, What Breaks If Removed

### UserController.java — Impact Analysis

| Line | Purpose | What Breaks If Removed |
|------|---------|----------------------|
| `@RestController` | Registers class as a web controller + enables JSON responses | 404 on all endpoints; no controller exists |
| `@RequestMapping("/api/v1/users")` | Sets base URL path for all methods | Methods map to root path; URL changes |
| `@AllArgsConstructor` | Generates constructor for dependency injection | Spring can't inject `UserService`; app won't start |
| `private final UserService userService` | Holds reference to the service layer | Can't call any business logic; compilation error |
| `@GetMapping("/{userId}")` | Maps GET requests with a path variable | This specific endpoint doesn't exist; 404 |
| `@PathVariable("userId") String userId` | Extracts UUID string from URL path | Spring can't bind path segment to parameter; error |
| `ResponseEntity.ok(...)` | Wraps response with 200 status | No proper HTTP status code in response |
| `userService.getUserById(userId)` | Delegates to service layer | No business logic executes; nothing happens |

### UserServiceImpl.java — Impact Analysis

| Line | Purpose | What Breaks If Removed |
|------|---------|----------------------|
| `@Service` | Registers as Spring bean | `UserService` bean not found; app won't start |
| `@RequiredArgsConstructor` | Generates constructor for DI | Can't inject `UserRepository` and `ModelMapper` |
| `private final UserRepository userRepository` | Access to database operations | Can't query the database; compilation error |
| `private final ModelMapper modelMapper` | Entity-to-DTO conversion | Can't convert `User` to `UserDto`; compilation error |
| `UserHelper.parseUUID(userId)` | Converts String to UUID | Can't pass correct type to `findById()`; type mismatch |
| `userRepository.findById(...)` | Queries database by primary key | No database query; can't retrieve user data |
| `.orElseThrow(...)` | Handles missing user case | `Optional` not unwrapped; type mismatch (Optional vs User) |
| `new ResourceNotFoundException(...)` | Creates meaningful error | Generic exception or `NoSuchElementException` instead |
| `modelMapper.map(user, UserDto.class)` | Converts entity to DTO | Compilation error (no return value) or entity leaks to API |

### UserHelper.java — Impact Analysis

| Line | Purpose | What Breaks If Removed |
|------|---------|----------------------|
| `public static UUID parseUUID(String uuid)` | Centralized UUID parsing | Must use `UUID.fromString()` directly everywhere |
| `UUID.fromString(uuid)` | Actual String→UUID conversion | Cannot create UUID object; no database lookup possible |

---

## 6. Data Flow Tracking — Transformations at Each Layer

### Complete Data Transformation Journey (Success Path)

```text
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 1: HTTP Request (Raw)                                     │
│                                                                 │
│ GET /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890         │
│                                                                 │
│ Data Type: Raw HTTP text in URL path                            │
│ Value:     "a1b2c3d4-e5f6-7890-abcd-ef1234567890" (in URL)     │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓ Spring extracts path variable
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 2: Controller (@PathVariable)                             │
│                                                                 │
│ Data Type: String                                               │
│ Variable:  userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"     │
│ Action:    Pass to service layer                                │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓ userService.getUserById(userId)
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 3: Service — UUID Parsing (UserHelper)                    │
│                                                                 │
│ Input:     String "a1b2c3d4-e5f6-7890-abcd-ef1234567890"       │
│ Operation: UserHelper.parseUUID() → UUID.fromString()           │
│ Output:    UUID object (mostSigBits + leastSigBits)             │
│                                                                 │
│ Data Type CHANGES: String → java.util.UUID                      │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓ userRepository.findById(uuid)
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 4: Repository — Database Query                            │
│                                                                 │
│ Input:     UUID object                                          │
│ Operation: Hibernate generates SELECT SQL                       │
│ SQL:       SELECT * FROM users WHERE user_id = ?                │
│            + SELECT * FROM user_roles/roles (EAGER fetch)       │
│ Output:    Optional<User> — either Optional.of(user) or empty   │
│                                                                 │
│ Data Type CHANGES: UUID → SQL parameter → ResultSet → User      │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓ .orElseThrow() unwraps Optional
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 5: Service — Optional Unwrapping                          │
│                                                                 │
│ Input:     Optional<User>                                       │
│ Operation: .orElseThrow() extracts the User from the Optional   │
│ Output:    User entity object (with all fields populated)       │
│                                                                 │
│ Data Type CHANGES: Optional<User> → User                        │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓ modelMapper.map(user, UserDto.class)
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 6: Service — Entity to DTO Mapping                        │
│                                                                 │
│ Input:     User entity (JPA-managed, potentially Hibernate-     │
│            proxied, linked to persistence context)              │
│ Operation: ModelMapper copies all matching fields               │
│ Output:    UserDto (plain POJO, no JPA context)                 │
│                                                                 │
│ Data Type CHANGES: User (Entity) → UserDto (DTO)                │
│                                                                 │
│ Field-by-field copy:                                            │
│   User.id        (UUID)     → UserDto.id        (UUID)          │
│   User.email     (String)   → UserDto.email     (String)        │
│   User.name      (String)   → UserDto.name      (String)        │
│   User.password  (String)   → UserDto.password  (String)        │
│   User.image     (String)   → UserDto.image     (String)        │
│   User.enable    (boolean)  → UserDto.enable    (boolean)       │
│   User.createdAt (Instant)  → UserDto.createdAt (Instant)       │
│   User.updatedAt (Instant)  → UserDto.updatedAt (Instant)       │
│   User.provider  (Provider) → UserDto.provider  (Provider)      │
│   User.roles     (Set<Role>)→ UserDto.roles     (Set<Role>)     │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓ return to Controller
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 7: Controller — Response Wrapping                         │
│                                                                 │
│ Input:     UserDto object                                       │
│ Operation: ResponseEntity.ok(userDto)                           │
│ Output:    ResponseEntity<UserDto> { status=200, body=userDto } │
│                                                                 │
│ Data Type CHANGES: UserDto → ResponseEntity<UserDto>            │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓ Jackson serialization
┌─────────────────────────────────────────────────────────────────┐
│ LAYER 8: HTTP Response (Serialization)                          │
│                                                                 │
│ Input:     ResponseEntity<UserDto>                              │
│ Operation: MappingJackson2HttpMessageConverter.write()           │
│ Output:    JSON string in HTTP response body                    │
│                                                                 │
│ Data Type CHANGES: UserDto (Java object) → JSON (text)          │
│                                                                 │
│ {                                                               │
│   "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",               │
│   "email": "aditya@gmail.com",                                 │
│   "name": "Aditya",                                            │
│   "password": "$2a$10$hashedPasswordValueHere",                 │
│   "image": "https://example.com/aditya.jpg",                   │
│   "enable": true,                                               │
│   "createdAt": "2026-06-27T10:30:00Z",                         │
│   "updatedAt": "2026-06-27T10:30:00Z",                         │
│   "provider": "LOCAL",                                          │
│   "roles": [{"id":"f47ac10b-...","name":"ROLE_USER"}]           │
│ }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

### Data Transformation Summary Table

| Layer | Input Type | Output Type | Transformation |
|-------|-----------|-------------|---------------|
| HTTP → Controller | URL path segment | `String` | Spring extracts `{userId}` from URL |
| Controller → Service | `String` | `String` | Passed as-is |
| Service → Helper | `String` | `UUID` | `UUID.fromString()` parses hex string |
| Service → Repository | `UUID` | `Optional<User>` | SQL query, Hibernate entity hydration |
| Service (orElseThrow) | `Optional<User>` | `User` | Unwraps or throws exception |
| Service (ModelMapper) | `User` entity | `UserDto` DTO | Reflection-based field copying |
| Controller (wrap) | `UserDto` | `ResponseEntity<UserDto>` | Adds HTTP 200 status |
| Spring (serialize) | `ResponseEntity<UserDto>` | JSON bytes | Jackson ObjectMapper serialization |

---

## 7. Annotation Explanations in Context

Every annotation that participates in this API's execution, explained exactly where it appears:

### Controller Layer Annotations

| Annotation | Location | Purpose in This Flow |
|-----------|----------|---------------------|
| `@RestController` | `UserController` class | Registers as controller + auto-serializes return values to JSON |
| `@RequestMapping("/api/v1/users")` | `UserController` class | Sets `/api/v1/users` as base path for all methods |
| `@AllArgsConstructor` | `UserController` class | Generates constructor for `UserService` injection |
| `@GetMapping("/{userId}")` | `getUserById()` method | Maps `GET /api/v1/users/{userId}` to this method |
| `@PathVariable("userId")` | `userId` parameter | Binds URL path segment to method parameter |

### Service Layer Annotations

| Annotation | Location | Purpose in This Flow |
|-----------|----------|---------------------|
| `@Service` | `UserServiceImpl` class | Registers as Spring bean; injectable into controllers |
| `@RequiredArgsConstructor` | `UserServiceImpl` class | Generates constructor for `UserRepository` and `ModelMapper` injection |
| `@Override` | `getUserById()` method | Compile-time check that this method implements the interface contract |

### Entity Layer Annotations

| Annotation | Location | Purpose in This Flow |
|-----------|----------|---------------------|
| `@Entity` | `User` class | Tells Hibernate this class maps to a database table |
| `@Table(name="users")` | `User` class | Specifies the exact table name is `users` |
| `@Id` | `User.id` field | Marks `id` as the primary key — used by `findById()` |
| `@GeneratedValue(strategy=UUID)` | `User.id` field | Auto-generates UUID on insert (not relevant for GET, but defines the PK type) |
| `@Column(name="user_id")` | `User.id` field | Maps Java field `id` to DB column `user_id` — critical for the WHERE clause |
| `@Column(name="user_email", unique=true, length=300)` | `User.email` field | Maps `email` to `user_email` column |
| `@Enumerated(EnumType.STRING)` | `User.provider` field | Stores enum as text ("LOCAL") not ordinal (0) |
| `@ManyToMany(fetch=FetchType.EAGER)` | `User.roles` field | Roles are loaded immediately with the user (no lazy loading) |
| `@JoinTable(...)` | `User.roles` field | Defines the `user_roles` junction table for the many-to-many relationship |
| `@Getter` / `@Setter` | `User` class | Generates getters/setters — ModelMapper and Jackson rely on these |
| `@NoArgsConstructor` | `User` class | Generates no-arg constructor — required by Hibernate for entity instantiation |
| `@AllArgsConstructor` | `User` class | Generates all-args constructor — used by `@Builder` |
| `@Builder` | `User` class | Generates builder pattern — not used in GET flow but available |

### Exception Handling Annotations

| Annotation | Location | Purpose in This Flow |
|-----------|----------|---------------------|
| `@RestControllerAdvice` | `GlobalExceptionHandler` class | Global exception handler for all controllers + JSON responses |
| `@ExceptionHandler(ResourceNotFoundException.class)` | `handleResourceNotFoundException()` | Catches `ResourceNotFoundException` and returns 404 |

### Configuration Annotations

| Annotation | Location | Purpose in This Flow |
|-----------|----------|---------------------|
| `@Configuration` | `ProjectConfig` class | Marks as Spring configuration class; methods create beans |
| `@Bean` | `modelMapper()` method | Registers `ModelMapper` instance as a Spring bean |

### DTO Annotations

| Annotation | Location | Purpose in This Flow |
|-----------|----------|---------------------|
| `@Getter` / `@Setter` | `UserDto` class | Jackson needs getters for serialization; ModelMapper needs setters for population |
| `@NoArgsConstructor` | `UserDto` class | ModelMapper needs this to create a new `UserDto` instance |
| `@AllArgsConstructor` | `UserDto` class | Available for manual construction (not used in this flow) |
| `@Builder` | `UserDto` class | Available for builder pattern (not used in this flow) |

---

## 8. Summary

### 8.1 Execution Flow Summary

```text
SUCCESS PATH:
═══════════════════════════════════════════════════════════════════
HTTP GET Request
    → Tomcat receives request
    → DispatcherServlet routes to UserController
    → UserController.getUserById(String) called
        → userService.getUserById(String) called
            → UserHelper.parseUUID(String) → UUID object
            → userRepository.findById(UUID) → Optional<User>
            → .orElseThrow() → unwraps to User entity
            → modelMapper.map(User, UserDto.class) → UserDto
        → Returns UserDto to Controller
    → ResponseEntity.ok(UserDto) → ResponseEntity<UserDto>
    → Jackson serializes to JSON
    → HTTP 200 OK + JSON body sent to client
═══════════════════════════════════════════════════════════════════

FAILURE PATH (User Not Found):
═══════════════════════════════════════════════════════════════════
HTTP GET Request
    → Tomcat → DispatcherServlet → UserController → UserService
        → UserHelper.parseUUID() → UUID (valid format)
        → userRepository.findById(UUID) → Optional.empty()
        → .orElseThrow() → throws ResourceNotFoundException
    → Exception propagates to DispatcherServlet
    → GlobalExceptionHandler.handleResourceNotFoundException()
        → Creates ErrorResponse(message, NOT_FOUND, 404)
        → Returns ResponseEntity with 404 status
    → Jackson serializes ErrorResponse to JSON
    → HTTP 404 NOT FOUND + error JSON sent to client
═══════════════════════════════════════════════════════════════════

FAILURE PATH (Invalid UUID):
═══════════════════════════════════════════════════════════════════
HTTP GET Request
    → Tomcat → DispatcherServlet → UserController → UserService
        → UserHelper.parseUUID("invalid") → throws IllegalArgumentException
    → Exception propagates to DispatcherServlet
    → No matching @ExceptionHandler found
    → Spring's default error handler returns 500 Internal Server Error
═══════════════════════════════════════════════════════════════════
```

### 8.2 Objects Created During Execution

| Object | Created By | When | Purpose |
|--------|-----------|------|---------|
| `HttpServletRequest` | Tomcat | Request arrives | Represents the raw HTTP request |
| `String userId` | Spring (DispatcherServlet) | Path variable extraction | Holds the UUID string from the URL |
| `UUID` | `UserHelper.parseUUID()` → `UUID.fromString()` | Service layer | Type-safe UUID for database query |
| `User` (entity) | Hibernate | After SQL query | Hydrated entity from database row |
| `Set<Role>` | Hibernate | EAGER fetch | Roles associated with the user |
| `Optional<User>` | `SimpleJpaRepository.findById()` | Repository layer | Wraps the nullable query result |
| `UserDto` | `ModelMapper.map()` | Service layer | DTO copy of the entity for API response |
| `ResponseEntity<UserDto>` | `ResponseEntity.ok()` | Controller layer | Wraps DTO with HTTP 200 status |
| JSON string | Jackson `ObjectMapper` | Serialization | Final response body |

### 8.3 Methods Called (in Order)

| # | Method | Called On | Input | Output |
|---|--------|----------|-------|--------|
| 1 | `doDispatch()` | `DispatcherServlet` | `HttpServletRequest` | Routes to handler |
| 2 | `getUserById()` | `UserController` | `String "a1b2c3d4-..."` | `ResponseEntity<UserDto>` |
| 3 | `getUserById()` | `UserServiceImpl` | `String "a1b2c3d4-..."` | `UserDto` |
| 4 | `parseUUID()` | `UserHelper` (static) | `String "a1b2c3d4-..."` | `UUID` |
| 5 | `fromString()` | `UUID` (static) | `String "a1b2c3d4-..."` | `UUID` |
| 6 | `findById()` | `UserRepository` (proxy) | `UUID` | `Optional<User>` |
| 7 | `orElseThrow()` | `Optional<User>` | `Supplier<Exception>` | `User` or throws |
| 8 | `map()` | `ModelMapper` | `User, UserDto.class` | `UserDto` |
| 9 | `ok()` | `ResponseEntity` (static) | `UserDto` | `ResponseEntity<UserDto>` |
| 10 | `writeValueAsString()` | Jackson `ObjectMapper` | `UserDto` | JSON string |

### 8.4 Database Operations

| # | Operation | SQL Generated | Table(s) Hit | Result |
|---|-----------|--------------|-------------|--------|
| 1 | `findById(UUID)` | `SELECT u.* FROM users u WHERE u.user_id = ?` | `users` | 0 or 1 row |
| 2 | EAGER fetch roles | `SELECT r.* FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?` | `user_roles`, `roles` | 0+ rows |

**Total queries:** 2 (one for user, one for roles due to EAGER loading)

### 8.5 Final Response

**Success (200 OK):**

```json
{
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "aditya@gmail.com",
    "name": "Aditya",
    "password": "$2a$10$hashedPasswordValueHere",
    "image": "https://example.com/aditya.jpg",
    "enable": true,
    "createdAt": "2026-06-27T10:30:00Z",
    "updatedAt": "2026-06-27T10:30:00Z",
    "provider": "LOCAL",
    "roles": [
        {
            "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            "name": "ROLE_USER"
        }
    ]
}
```

**Failure — User Not Found (404):**

```json
{
    "message": "User not found with the given id",
    "status": "NOT_FOUND",
    "statusCode": 404
}
```

**Failure — Invalid UUID (500):**

```json
{
    "timestamp": "2026-06-27T15:18:00.000+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "path": "/api/v1/users/not-a-valid-uuid"
}
```

### 8.6 Key Interview Talking Points

1. **Why use `String` instead of `UUID` for `@PathVariable`?**
   - Gives manual control over UUID parsing. You can handle `IllegalArgumentException` explicitly in the service or helper layer. If you use `UUID` directly in `@PathVariable`, Spring's type conversion handles it, and a conversion failure results in a `400 Bad Request` with a less controlled error message.

2. **Why use `UserHelper.parseUUID()` instead of `UUID.fromString()` directly?**
   - Centralized parsing allows future enhancements (custom validation, logging, custom error messages) in one place. It's a form of the **Single Responsibility Principle** — the helper is responsible for UUID concerns.

3. **Why use `Optional.orElseThrow()` instead of `Optional.get()`?**
   - `Optional.get()` throws a generic `NoSuchElementException` if the Optional is empty. `orElseThrow()` lets you throw a **custom, meaningful exception** (`ResourceNotFoundException`) that the `GlobalExceptionHandler` can catch and convert to a proper `404 NOT FOUND` response.

4. **Why convert `User` entity to `UserDto`?**
   - **Decoupling:** Changes to the database schema (entity) don't automatically break the API contract (DTO).
   - **Security:** You can exclude sensitive fields from the DTO (though `password` is still included here — a potential improvement).
   - **Flexibility:** Different endpoints can return different DTOs from the same entity.

5. **Why is `FetchType.EAGER` used for roles?**
   - Roles are always needed when displaying a user. EAGER loading fetches them in a single database round-trip (or a second query). Without EAGER, accessing `user.getRoles()` outside a transaction boundary would throw a `LazyInitializationException`.

6. **What's the risk of returning the password in the DTO?**
   - Even though it's hashed, exposing it in the API response is a security anti-pattern. Attackers can use the hash for offline brute-force attacks. Best practice: exclude it from the response DTO.

---

*Document generated for interview preparation. Every line of code in the GET /api/v1/users/{userId} flow has been traced and explained.*
