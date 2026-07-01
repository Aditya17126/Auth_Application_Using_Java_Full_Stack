# 📘 GET USER BY EMAIL API — Complete Execution Trace

## API Endpoint: `GET /api/v1/users/email/{emailId}`

---

## 1. API Endpoint Details

| Property          | Value                                        |
|-------------------|----------------------------------------------|
| **HTTP Method**   | `GET`                                        |
| **URL**           | `/api/v1/users/email/{emailId}`              |
| **Path Variable** | `emailId` — the email address of the user    |
| **Request Body**  | ❌ None (GET request)                        |
| **Auth Required** | ❌ No (Spring Security is commented out)     |
| **Success Code**  | `200 OK`                                     |
| **Failure Code**  | `404 NOT FOUND` (if email not in database)   |

### Success Response (200 OK)
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "aditya@gmail.com",
  "name": "Aditya",
  "password": "$2a$10$hashedPasswordValue",
  "image": "https://example.com/aditya.png",
  "enable": true,
  "createdAt": "2026-06-27T15:00:00Z",
  "updatedAt": "2026-06-27T15:00:00Z",
  "provider": "LOCAL",
  "roles": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "name": "ROLE_USER"
    }
  ]
}
```

### Failure Response (404 NOT FOUND)
```json
{
  "message": "User not found with the give email Id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

---

## 2. Real-World Example Scenario

> **Aditya** is building a user management dashboard. He wants to look up a user's profile details by their email address. He types `aditya@gmail.com` into the search bar and the frontend fires a GET request to the backend.

### The HTTP Request
```
GET /api/v1/users/email/aditya@gmail.com HTTP/1.1
Host: localhost:8080
Accept: application/json
```

- There is **no request body** — the email is embedded directly in the URL path.
- There are **no authentication headers** — Spring Security is commented out.

### Two Possible Outcomes

| Scenario | Condition | HTTP Status | What Happens |
|----------|-----------|-------------|--------------|
| ✅ **Success** | `aditya@gmail.com` exists in the `users` table | `200 OK` | User entity is found, mapped to DTO, returned as JSON |
| ❌ **Failure** | `aditya@gmail.com` does NOT exist in the `users` table | `404 NOT FOUND` | `ResourceNotFoundException` thrown → caught by `GlobalExceptionHandler` → `ErrorResponse` returned |

---

## 3. Security Filter Chain Execution

> [!NOTE]
> Spring Security is **completely commented out** in this project. There is no `SecurityFilterChain`, no `JwtAuthFilter`, no authentication or authorization logic. Every request flows directly to Spring MVC's `DispatcherServlet` without any security interception.

### What This Means for This API

```
Client Request (GET /api/v1/users/email/aditya@gmail.com)
       │
       ▼
 ┌─────────────────────────┐
 │   Tomcat Receives Req   │
 └────────────┬────────────┘
              │
              ▼
 ┌─────────────────────────┐
 │  No Security Filters    │  ← Spring Security is commented out
 │  (Bypass entirely)      │
 └────────────┬────────────┘
              │
              ▼
 ┌─────────────────────────┐
 │   DispatcherServlet     │  ← Spring MVC takes over immediately
 └────────────┬────────────┘
              │
              ▼
 ┌─────────────────────────┐
 │   HandlerMapping        │  ← Finds UserController.getUserByEmail()
 └─────────────────────────┘
```

- **No `OncePerRequestFilter`** runs.
- **No JWT token** is extracted or validated.
- **No `SecurityContext`** is populated.
- **No `AuthenticationManager`** is invoked.
- The request goes straight from Tomcat → DispatcherServlet → Controller.

---

## 4. Complete Step-by-Step Execution Flow

### 🔷 PHASE 1: Spring MVC Request Routing

#### Step 1 — Tomcat Receives the HTTP Request

```
GET /api/v1/users/email/aditya@gmail.com HTTP/1.1
```

- Embedded Tomcat listens on port `8080`.
- It receives the raw HTTP request and wraps it into an `HttpServletRequest` object.
- The request is handed to Spring's **`DispatcherServlet`** — the front controller for all Spring MVC applications.

#### Step 2 — DispatcherServlet Invokes HandlerMapping

```
DispatcherServlet → HandlerMapping.getHandler(request)
```

- `DispatcherServlet` asks `RequestMappingHandlerMapping`: _"Which controller method handles `GET /api/v1/users/email/aditya@gmail.com`?"_
- `RequestMappingHandlerMapping` scans all `@RequestMapping` annotations registered at startup.
- It finds a match:

| Annotation Level | Annotation | Contributes |
|---|---|---|
| **Class level** on `UserController` | `@RequestMapping("/api/v1/users")` | Base path: `/api/v1/users` |
| **Method level** on `getUserByEmail()` | `@GetMapping("/email/{emailId}")` | Sub-path: `/email/{emailId}` |
| **Combined** | | Full path: `/api/v1/users/email/{emailId}` |

- The `{emailId}` placeholder is matched against the URL segment `aditya@gmail.com`.
- Spring resolves: `emailId = "aditya@gmail.com"`.

#### Step 3 — HandlerAdapter Prepares Method Invocation

- `DispatcherServlet` selects `RequestMappingHandlerAdapter` to invoke the controller method.
- The adapter uses **argument resolvers** to convert URL data into Java method parameters:
  - `@PathVariable("emailId") String email` → resolved by `PathVariableMethodArgumentResolver`
  - It extracts the URI template variable `emailId` and assigns its value `"aditya@gmail.com"` to the `email` parameter.

---

### 🔷 PHASE 2: Controller Layer — `UserController.getUserByEmail()`

```java
@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
   private final UserService userService;

   @GetMapping("/email/{emailId}")
   public ResponseEntity<UserDto> getUserByEmail(@PathVariable("emailId") String email){
     return ResponseEntity.ok(userService.getUserByEmail(email));
   }
}
```

#### Line-by-Line Execution

---

##### `@RestController`
```java
@RestController
```
- **What it is:** A meta-annotation combining `@Controller` + `@ResponseBody`.
- **Why it executes:** At application startup, Spring's component scan detects this annotation and registers `UserController` as a Spring bean in the application context. During request handling, `@ResponseBody` tells Spring to serialize the return value directly into the HTTP response body (as JSON via Jackson), instead of resolving a view name.
- **What breaks if removed:** Spring would not register this class as a controller. The endpoint `/api/v1/users/email/{emailId}` would not exist. The client would receive a `404 Not Found` or a `Whitelabel Error Page`.

---

##### `@RequestMapping("/api/v1/users")`
```java
@RequestMapping("/api/v1/users")
```
- **What it is:** A class-level annotation that sets the **base URL path** for all handler methods in this controller.
- **Why it executes:** At startup, Spring registers this prefix. Every `@GetMapping`, `@PostMapping`, etc. in this class will have their paths prepended with `/api/v1/users`.
- **What breaks if removed:** The endpoint path would become just `/email/{emailId}` instead of `/api/v1/users/email/{emailId}`. The client's request to `/api/v1/users/email/aditya@gmail.com` would return `404`.

---

##### `@AllArgsConstructor`
```java
@AllArgsConstructor
```
- **What it is:** A Lombok annotation that generates a constructor with one parameter for every field in the class.
- **What it generates at compile time:**
  ```java
  public UserController(UserService userService) {
      this.userService = userService;
  }
  ```
- **Why it executes:** Spring uses **constructor injection** to inject the `UserService` bean (which is `UserServiceImpl`, since it's the only `@Service` implementing `UserService`) into this controller when creating the `UserController` bean.
- **What breaks if removed:** There would be no constructor to accept `UserService`. Spring would use the default no-arg constructor, and `userService` would remain `null`. Calling `userService.getUserByEmail(email)` would throw a `NullPointerException`.

---

##### `private final UserService userService;`
```java
private final UserService userService;
```
- **What it is:** A field declaration of type `UserService` (an interface).
- **Why `final`:** Makes the field immutable after construction. Combined with `@AllArgsConstructor`, this is the idiomatic way to do constructor-based dependency injection in Spring.
- **What Spring injects:** At startup, Spring finds the bean `UserServiceImpl` (annotated with `@Service`) which implements `UserService`, and injects it here.
- **What breaks if removed:** No `userService` field → Lombok won't generate a constructor parameter for it → No DI happens → No way to call service methods → Compilation errors inside `getUserByEmail()`.

---

##### `@GetMapping("/email/{emailId}")`
```java
@GetMapping("/email/{emailId}")
```
- **What it is:** A shortcut for `@RequestMapping(value = "/email/{emailId}", method = RequestMethod.GET)`.
- **What `{emailId}` means:** It's a **URI template variable** (path variable placeholder). Spring extracts the actual value from the URL and makes it available for binding.
- **Combined path:** Class-level `"/api/v1/users"` + method-level `"/email/{emailId}"` = `/api/v1/users/email/{emailId}`.
- **In our example:** The URL `/api/v1/users/email/aditya@gmail.com` matches this pattern, and `emailId` is bound to `"aditya@gmail.com"`.
- **What breaks if removed:** This method would no longer be mapped to any URL. The GET request to `/api/v1/users/email/aditya@gmail.com` would return `404`.

---

##### `public ResponseEntity<UserDto> getUserByEmail(@PathVariable("emailId") String email)`
```java
public ResponseEntity<UserDto> getUserByEmail(@PathVariable("emailId") String email)
```

| Component | Purpose |
|-----------|---------|
| `ResponseEntity<UserDto>` | Return type. Wraps the response body (`UserDto`) with HTTP status code and headers. |
| `getUserByEmail` | Method name. Descriptive, but not used by Spring for routing. |
| `@PathVariable("emailId")` | Binds the URI template variable `{emailId}` to the parameter `email`. The `"emailId"` string must match exactly what's in `@GetMapping`. |
| `String email` | The Java parameter that receives the extracted value. |

- **At this moment:** `email = "aditya@gmail.com"`.
- **What breaks if `@PathVariable` is removed:** Spring would try to resolve `email` as a query parameter (`?email=...`), which doesn't exist in this URL. The parameter would be `null`, and the service would search for a user with email `null`.
- **What breaks if `"emailId"` doesn't match `{emailId}`:** Spring throws `IllegalStateException` at startup: _"Could not find @PathVariable 'xxx' in @RequestMapping"_.

---

##### `return ResponseEntity.ok(userService.getUserByEmail(email));`
```java
return ResponseEntity.ok(userService.getUserByEmail(email));
```

This single line does **three things** in sequence:

1. **`userService.getUserByEmail(email)`** — Calls the service layer with `email = "aditya@gmail.com"`. This either returns a `UserDto` object or throws a `ResourceNotFoundException`.
2. **`ResponseEntity.ok(...)`** — A static factory method that creates a `ResponseEntity` with:
   - HTTP status `200 OK`
   - The `UserDto` as the response body
3. **`return`** — Returns the `ResponseEntity<UserDto>` to the `HandlerAdapter`, which then uses `HttpMessageConverter` (Jackson's `MappingJackson2HttpMessageConverter`) to serialize the `UserDto` into JSON.

- **What breaks if `ResponseEntity.ok()` is replaced with just `return userService.getUserByEmail(email);`:** The method return type is `ResponseEntity<UserDto>`, so a raw `UserDto` return would cause a **compilation error**. If you changed the return type to `UserDto` (and kept `@RestController`), it would still work but you'd lose fine-grained control over HTTP status codes.

---

### 🔷 PHASE 3: Service Layer — `UserServiceImpl.getUserByEmail()`

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final ModelMapper modelMapper;

    @Override
    public UserDto getUserByEmail(String email) {
       User user = userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with the give email Id"));
        return modelMapper.map(user, UserDto.class);
    }
}
```

#### Line-by-Line Execution

---

##### `@Service`
```java
@Service
```
- **What it is:** A Spring stereotype annotation. It's a specialization of `@Component`.
- **Why it executes:** During component scanning at startup, Spring detects this annotation and registers `UserServiceImpl` as a singleton bean in the application context. When `UserController` needs a `UserService`, Spring injects this implementation.
- **What breaks if removed:** `UserServiceImpl` won't be registered as a bean. Spring can't find any `UserService` implementation to inject into `UserController`. Application startup fails with: `NoSuchBeanDefinitionException: No qualifying bean of type 'UserService'`.

---

##### `@RequiredArgsConstructor`
```java
@RequiredArgsConstructor
```
- **What it is:** A Lombok annotation that generates a constructor for all `final` fields and any fields annotated with `@NonNull`.
- **What it generates at compile time:**
  ```java
  public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
      this.userRepository = userRepository;
      this.modelMapper = modelMapper;
  }
  ```
- **Why it executes:** Spring uses this generated constructor to inject the `UserRepository` proxy and the `ModelMapper` bean.
- **What breaks if removed:** No constructor is generated → Spring uses default no-arg constructor → `userRepository` and `modelMapper` remain `null` → `NullPointerException` at runtime.

---

##### `private final UserRepository userRepository;`
```java
private final UserRepository userRepository;
```
- **What it is:** A reference to the repository interface.
- **What Spring injects:** Spring Data JPA automatically creates a **proxy implementation** of `UserRepository` at startup. This proxy knows how to translate method names like `findByEmail()` into SQL queries. That proxy is injected here.
- **What breaks if removed:** No way to query the database → compilation errors in `getUserByEmail()`.

---

##### `private final ModelMapper modelMapper;`
```java
private final ModelMapper modelMapper;
```
- **What it is:** A reference to the `ModelMapper` bean, which handles object-to-object mapping (Entity ↔ DTO).
- **Where the bean comes from:** The `ProjectConfig` class:
  ```java
  @Configuration
  public class ProjectConfig {
    @Bean
    public ModelMapper modelMapper(){ return new ModelMapper(); }
  }
  ```
- **What breaks if removed:** No way to convert `User` entity to `UserDto` → compilation errors.

---

##### `@Override`
```java
@Override
```
- **What it is:** A standard Java annotation indicating this method implements/overrides a method from the `UserService` interface.
- **What it does at runtime:** Nothing. It's a compile-time-only annotation.
- **What breaks if removed:** Nothing at runtime. But the compiler won't warn you if the method signature doesn't match the interface.

---

##### `public UserDto getUserByEmail(String email)`
```java
public UserDto getUserByEmail(String email)
```
- **Parameter received:** `email = "aditya@gmail.com"` (passed from the controller).
- **Return type:** `UserDto` — the DTO representation of the user.

---

##### `User user = userRepository.findByEmail(email)`
```java
User user = userRepository
  .findByEmail(email)
```

This line initiates a **database query**. Let's trace exactly what happens:

1. **`userRepository`** is a Spring Data JPA proxy (of type `SimpleJpaRepository` wrapped in a dynamic proxy).
2. **`.findByEmail(email)`** is a **derived query method**. Spring Data JPA parses the method name:
   - `findBy` → SELECT query
   - `Email` → WHERE clause on the `email` field of the `User` entity
3. **The generated SQL:**
   ```sql
   SELECT u.* FROM users u WHERE u.user_email = ?
   ```
   - `user_email` is used because the `User` entity has `@Column(name = "user_email")` on the `email` field.
   - The parameter `?` is bound to `"aditya@gmail.com"`.

4. **Return type:** `Optional<User>` — can be empty (no user found) or contain a `User` entity.

5. **Behind the scenes:**
   - Spring opens a database connection from the connection pool (HikariCP).
   - Hibernate translates the method into a JPQL query, then to native SQL.
   - The SQL is executed against the database.
   - If a row is found, Hibernate hydrates a `User` entity object from the result set.
   - The `Optional` is created: `Optional.of(user)` if found, `Optional.empty()` if not found.
   - The connection is returned to the pool.

- **What breaks if removed:** No database query happens → no `User` object → everything after this line fails.

---

##### `.orElseThrow(() -> new ResourceNotFoundException("User not found with the give email Id"))`
```java
.orElseThrow(() -> new ResourceNotFoundException("User not found with the give email Id"));
```

This is where the **two execution paths diverge**:

---

###### ✅ SUCCESS PATH: User Found in Database

- `findByEmail("aditya@gmail.com")` returned `Optional.of(userEntity)`.
- `.orElseThrow(...)` sees the Optional is **present**.
- It **unwraps** the Optional and returns the `User` entity.
- The lambda `() -> new ResourceNotFoundException(...)` is **never executed** — it's not needed.
- `user` now holds a fully populated `User` entity:

```java
User {
    id       = UUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
    email    = "aditya@gmail.com",
    name     = "Aditya",
    password = "$2a$10$hashedPasswordValue",
    image    = "https://example.com/aditya.png",
    enable   = true,
    createdAt = Instant("2026-06-27T15:00:00Z"),
    updatedAt = Instant("2026-06-27T15:00:00Z"),
    provider  = Provider.LOCAL,
    roles     = Set<Role>{ Role{id=UUID, name="ROLE_USER"} }
}
```

- **Execution continues** to the next line → `modelMapper.map(...)`.

---

###### ❌ FAILURE PATH: User NOT Found in Database

- `findByEmail("aditya@gmail.com")` returned `Optional.empty()`.
- `.orElseThrow(...)` sees the Optional is **empty**.
- It **executes the lambda**: `() -> new ResourceNotFoundException("User not found with the give email Id")`.
- A new `ResourceNotFoundException` object is created with message `"User not found with the give email Id"`.
- The exception is **thrown**.
- **Execution of `getUserByEmail()` stops immediately.**
- **Execution of the controller method `getUserByEmail()` also stops.**
- The exception **propagates up** the call stack to the `DispatcherServlet`.
- The `DispatcherServlet` hands it to the `GlobalExceptionHandler`.

> **Jump to [Phase 5: Exception Handling (Failure Path)](#-phase-5-exception-handling-failure-path-404)** for the failure flow continuation.

- **What breaks if `.orElseThrow()` is replaced with `.get()`:** If the user doesn't exist, `Optional.get()` throws `NoSuchElementException` instead of your custom `ResourceNotFoundException`. The `GlobalExceptionHandler` does NOT handle `NoSuchElementException`, so the client gets a generic `500 Internal Server Error` instead of a meaningful `404 Not Found`.

---

##### `return modelMapper.map(user, UserDto.class);` *(Success Path Only)*
```java
return modelMapper.map(user, UserDto.class);
```

This line converts the `User` entity into a `UserDto` object using **ModelMapper**.

1. **`modelMapper`** — the `ModelMapper` bean injected from `ProjectConfig`.
2. **`.map(user, UserDto.class)`** — tells ModelMapper:
   - **Source:** the `User` entity (with all its populated fields).
   - **Target type:** `UserDto.class`.
3. **How ModelMapper works internally:**
   - It uses **reflection** to inspect both `User` and `UserDto`.
   - It matches fields by **name and type**:

| User Entity Field | UserDto Field | Type | Mapped? |
|---|---|---|---|
| `id` | `id` | `UUID` | ✅ Yes |
| `email` | `email` | `String` | ✅ Yes |
| `name` | `name` | `String` | ✅ Yes |
| `password` | `password` | `String` | ✅ Yes |
| `image` | `image` | `String` | ✅ Yes |
| `enable` | `enable` | `boolean` | ✅ Yes |
| `createdAt` | `createdAt` | `Instant` | ✅ Yes |
| `updatedAt` | `updatedAt` | `Instant` | ✅ Yes |
| `provider` | `provider` | `Provider` | ✅ Yes |
| `roles` | `roles` | `Set<Role>` | ✅ Yes |

4. **Result:** A new `UserDto` object is created with all fields copied from the `User` entity:

```java
UserDto {
    id        = UUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
    email     = "aditya@gmail.com",
    name      = "Aditya",
    password  = "$2a$10$hashedPasswordValue",
    image     = "https://example.com/aditya.png",
    enable    = true,
    createdAt = Instant("2026-06-27T15:00:00Z"),
    updatedAt = Instant("2026-06-27T15:00:00Z"),
    provider  = Provider.LOCAL,
    roles     = Set<Role>{ Role{id=UUID, name="ROLE_USER"} }
}
```

5. **`return`** — this `UserDto` is returned to the controller.

- **What breaks if removed:** The method would try to return a `User` entity. But the return type is `UserDto`, causing a **compilation error**. Even if you changed the return type to `User`, you'd be exposing your JPA entity directly to the client — a bad practice because:
  - Entity changes would break your API contract.
  - Lazy-loaded collections could throw `LazyInitializationException`.
  - You might accidentally expose sensitive fields.

---

### 🔷 PHASE 4: Response Serialization (Success Path — 200 OK)

After `userService.getUserByEmail(email)` returns the `UserDto`, execution returns to the controller:

```java
return ResponseEntity.ok(userService.getUserByEmail(email));
```

#### Step-by-Step Response Construction

1. **`ResponseEntity.ok(userDto)`** creates:
   ```java
   ResponseEntity<UserDto> {
       status = 200 (HttpStatus.OK),
       headers = {},
       body = UserDto { ... }
   }
   ```

2. **`HandlerAdapter`** receives this `ResponseEntity` from the controller.

3. **`HttpMessageConverter` selection:** Spring iterates through registered message converters. `MappingJackson2HttpMessageConverter` (Jackson) is selected because:
   - The `Accept` header is `application/json`.
   - Jackson is on the classpath (included by `spring-boot-starter-web`).

4. **Jackson serializes the `UserDto`** into JSON:
   - Jackson uses **reflection** to read all getter methods on `UserDto`.
   - Lombok's `@Getter` generated getters like `getId()`, `getEmail()`, `getName()`, etc.
   - Each getter's return value becomes a JSON property.
   - `Provider.LOCAL` enum is serialized as the string `"LOCAL"`.
   - `Instant` is serialized as ISO-8601 string (e.g., `"2026-06-27T15:00:00Z"`).
   - `Set<Role>` is serialized as a JSON array of objects.

5. **Final HTTP Response:**
   ```
   HTTP/1.1 200 OK
   Content-Type: application/json
   
   {
     "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     "email": "aditya@gmail.com",
     "name": "Aditya",
     "password": "$2a$10$hashedPasswordValue",
     "image": "https://example.com/aditya.png",
     "enable": true,
     "createdAt": "2026-06-27T15:00:00Z",
     "updatedAt": "2026-06-27T15:00:00Z",
     "provider": "LOCAL",
     "roles": [
       {
         "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
         "name": "ROLE_USER"
       }
     ]
   }
   ```

---

### 🔷 PHASE 5: Exception Handling (Failure Path — 404)

When `findByEmail("aditya@gmail.com")` returns `Optional.empty()` and `orElseThrow()` throws `ResourceNotFoundException`, the following happens:

#### Step 1 — Exception Propagates Up

```
ResourceNotFoundException("User not found with the give email Id")
       │
       ▲ thrown from UserServiceImpl.getUserByEmail()
       │
       ▲ propagates through UserController.getUserByEmail()
       │
       ▲ caught by DispatcherServlet
       │
       ▼
  HandlerExceptionResolver chain
       │
       ▼
  GlobalExceptionHandler.handleResourceNotFoundException()
```

- The exception is NOT caught in the service layer (no try-catch).
- The exception is NOT caught in the controller layer (no try-catch).
- The `DispatcherServlet` catches it and delegates to registered `HandlerExceptionResolver` beans.
- `ExceptionHandlerExceptionResolver` finds a matching `@ExceptionHandler` in `GlobalExceptionHandler`.

#### Step 2 — GlobalExceptionHandler Catches the Exception

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception){
   ErrorResponse internalServerError = new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND, 404);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
  }
}
```

##### `@RestControllerAdvice`
```java
@RestControllerAdvice
```
- **What it is:** Combines `@ControllerAdvice` + `@ResponseBody`. Makes this class a **global exception handler** for all controllers. Any exception thrown from any controller is eligible to be caught here.
- **What breaks if removed:** No global exception handling. `ResourceNotFoundException` would result in a Spring default error response (generic `500 Internal Server Error` with a Whitelabel error page or Spring's default JSON error structure).

##### `@ExceptionHandler(ResourceNotFoundException.class)`
```java
@ExceptionHandler(ResourceNotFoundException.class)
```
- **What it is:** Tells Spring: _"When a `ResourceNotFoundException` is thrown from any controller, invoke THIS method to handle it."_
- **What breaks if removed:** This method wouldn't be invoked for `ResourceNotFoundException`. Spring would fall back to its default error handling.

##### `ResourceNotFoundException exception`
- **Parameter received:** The `ResourceNotFoundException` object that was thrown.
- `exception.getMessage()` returns `"User not found with the give email Id"`.

##### `ErrorResponse internalServerError = new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND, 404);`
```java
ErrorResponse internalServerError = new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND, 404);
```

- Creates an `ErrorResponse` record:
  ```java
  public record ErrorResponse(String message, HttpStatus status, int statusCode){}
  ```
- The constructed object:
  ```java
  ErrorResponse {
      message    = "User not found with the give email Id",
      status     = HttpStatus.NOT_FOUND,
      statusCode = 404
  }
  ```

> [!NOTE]
> The variable is named `internalServerError` which is misleading — it actually represents a `404 NOT_FOUND` error. This is likely a copy-paste artifact from another handler. The naming doesn't affect functionality.

##### `return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);`
```java
return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
```

- **`ResponseEntity.status(HttpStatus.NOT_FOUND)`** — Sets the HTTP status to `404`.
- **`.body(internalServerError)`** — Attaches the `ErrorResponse` object as the body.
- Result:
  ```java
  ResponseEntity<ErrorResponse> {
      status = 404 (HttpStatus.NOT_FOUND),
      headers = {},
      body = ErrorResponse { message="User not found with the give email Id", status=NOT_FOUND, statusCode=404 }
  }
  ```

#### Step 3 — Jackson Serializes the ErrorResponse

Jackson serializes the `ErrorResponse` record into JSON:

```json
{
  "message": "User not found with the give email Id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

#### Step 4 — Final HTTP Error Response

```
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "message": "User not found with the give email Id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

---

## 5. Line-by-Line Explanation — Repository Layer

### UserRepository.java

```java
package com.substring.auth.auth_app.repositories;
import com.substring.auth.auth_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

##### `public interface UserRepository extends JpaRepository<User, UUID>`
```java
public interface UserRepository extends JpaRepository<User, UUID>
```
- **What it is:** An interface extending Spring Data JPA's `JpaRepository`.
- **`User`** — the entity type this repository manages.
- **`UUID`** — the type of the entity's primary key (`@Id` field in `User`).
- **What happens at startup:** Spring Data JPA creates a **proxy implementation** (using `SimpleJpaRepository` internally) at runtime. You never write the implementation — Spring generates it.
- **What breaks if removed:** No repository → no way to interact with the `users` table → the entire data access layer is gone.

##### `Optional<User> findByEmail(String email)`
```java
Optional<User> findByEmail(String email);
```
- **What it is:** A **derived query method**. Spring Data JPA parses the method name to generate the query.
- **Parsing:** `find` (action) + `By` (separator) + `Email` (property name on `User` entity).
- **Generated JPQL:** `SELECT u FROM User u WHERE u.email = :email`
- **Generated SQL:** `SELECT * FROM users WHERE user_email = ?`
- **Return type `Optional<User>`:** If a matching row exists, returns `Optional.of(user)`. If not, returns `Optional.empty()`.
- **What breaks if removed:** The `getUserByEmail()` service method calls this. Compilation fails.

##### `boolean existsByEmail(String email)`
```java
boolean existsByEmail(String email);
```
- **Not used in this API**, but exists in the repository. Generates a `SELECT COUNT(*) > 0 FROM users WHERE user_email = ?` query. Used in other flows (e.g., user creation to check for duplicates).

---

## 6. Line-by-Line Explanation — Entity Layer

### User.java

```java
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
@Entity @Table(name="users")
public class User {
   @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "user_id")
   private UUID id;
   @Column(name = "user_email", unique = true, length = 300)
   private String email;
   private String name;
   private String password;
   private String image;
   private boolean enable = true;
   private Instant createdAt = Instant.now();
   private Instant updatedAt = Instant.now();
   @Enumerated(EnumType.STRING)
   private Provider provider = Provider.LOCAL;
   @ManyToMany(fetch = FetchType.EAGER)
   @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
   private Set<Role> roles = new HashSet<>();
}
```

##### `@Getter @Setter`
- **Lombok annotations.** Generate getter and setter methods for all fields.
- **Why needed:** ModelMapper uses getters/setters to read source fields and write target fields during `map()`. Jackson uses getters during serialization.
- **What breaks if removed:** ModelMapper can't read `User` fields → mapping fails. Jackson can't serialize → empty JSON `{}`.

##### `@AllArgsConstructor @NoArgsConstructor`
- **Lombok annotations.** Generate an all-args constructor and a no-args constructor.
- **Why `@NoArgsConstructor`:** JPA **requires** a no-arg constructor to instantiate entities when hydrating from database rows.
- **What breaks if `@NoArgsConstructor` removed:** Hibernate throws `InstantiationException` when trying to create `User` objects from query results.

##### `@Builder`
- **Lombok annotation.** Generates a builder pattern for `User`. Not directly used in this GET API, but useful elsewhere for constructing `User` objects cleanly.

##### `@Entity`
- **JPA annotation.** Marks this class as a JPA entity — a Java object that maps to a database table.
- **What breaks if removed:** Hibernate doesn't recognize `User` as a managed entity. `UserRepository` can't function. Application fails at startup.

##### `@Table(name="users")`
- Maps this entity to the `users` table in the database.
- **What breaks if removed:** JPA defaults the table name to the class name (`User`). If your DB table is named `users`, queries would fail with `Table 'User' doesn't exist`.

##### `@Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "user_id")`
```java
@Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "user_id")
private UUID id;
```
- `@Id` — marks this field as the primary key.
- `@GeneratedValue(strategy = GenerationType.UUID)` — Hibernate auto-generates a UUID for new entities (not used in this GET flow, but relevant for inserts).
- `@Column(name = "user_id")` — maps to the `user_id` column in the `users` table.

##### `@Column(name = "user_email", unique = true, length = 300)`
```java
@Column(name = "user_email", unique = true, length = 300)
private String email;
```
- Maps to `user_email` column.
- `unique = true` — database enforces uniqueness (a UNIQUE constraint). This guarantees `findByEmail()` returns at most one result.
- `length = 300` — column is `VARCHAR(300)`.
- **Critical for this API:** The `findByEmail()` query uses `WHERE user_email = ?` because of this column mapping.

##### `@Enumerated(EnumType.STRING)`
```java
@Enumerated(EnumType.STRING)
private Provider provider = Provider.LOCAL;
```
- Stores the `Provider` enum as a **string** in the database (e.g., `"LOCAL"`, `"GOOGLE"`) instead of an integer ordinal.
- **What breaks if removed:** JPA defaults to `EnumType.ORDINAL`, storing `0`, `1`, `2`, `3` instead of readable strings. If enum order changes, existing data becomes corrupt.

##### `@ManyToMany(fetch = FetchType.EAGER)`
```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
private Set<Role> roles = new HashSet<>();
```
- **`@ManyToMany`** — A user can have many roles; a role can belong to many users.
- **`fetch = FetchType.EAGER`** — **Critical for this API.** When a `User` entity is loaded, its `roles` are **immediately fetched** in the same query (via a JOIN or a subsequent SELECT). Without `EAGER`, roles would only be loaded when accessed, and if the Hibernate session is closed by then, you'd get `LazyInitializationException`.
- **`@JoinTable(name = "user_roles")`** — The join table connecting `users` and `roles`.
- **Generated SQL** (with EAGER fetch):
  ```sql
  SELECT u.*, r.* 
  FROM users u 
  LEFT JOIN user_roles ur ON u.user_id = ur.user_id 
  LEFT JOIN roles r ON ur.role_id = r.role_id 
  WHERE u.user_email = ?
  ```

---

## 7. Data Flow Tracking — Complete Transformation Journey

### ✅ Success Scenario

```
┌──────────────────────────────────────────────────────────────────────┐
│  CLIENT REQUEST                                                       │
│  GET /api/v1/users/email/aditya@gmail.com                            │
│  Data: URL path contains email string "aditya@gmail.com"             │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  DISPATCHER SERVLET + HANDLER MAPPING                                 │
│  Extracts path variable: emailId = "aditya@gmail.com"                │
│  Resolves to: UserController.getUserByEmail(String email)            │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER — UserController                                    │
│  Method: getUserByEmail(@PathVariable("emailId") String email)       │
│  Input:  email = "aditya@gmail.com" (String)                         │
│  Action: Calls userService.getUserByEmail("aditya@gmail.com")        │
│  Output: Awaits UserDto from service                                  │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  SERVICE LAYER — UserServiceImpl                                      │
│  Method: getUserByEmail(String email)                                 │
│  Input:  email = "aditya@gmail.com" (String)                         │
│  Action: Calls userRepository.findByEmail("aditya@gmail.com")       │
│  Output: Awaits Optional<User> from repository                       │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  REPOSITORY LAYER — UserRepository (Spring Data JPA Proxy)            │
│  Method: findByEmail(String email)                                    │
│  Input:  email = "aditya@gmail.com" (String)                         │
│  SQL:    SELECT * FROM users WHERE user_email = 'aditya@gmail.com'   │
│  + JOIN: LEFT JOIN user_roles → LEFT JOIN roles (EAGER fetch)        │
│  Output: Optional<User> containing User entity                       │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  DATABASE LAYER                                                       │
│  Table: users (+ user_roles + roles via JOIN)                        │
│  Row found:                                                           │
│    user_id    = a1b2c3d4-e5f6-7890-abcd-ef1234567890                │
│    user_email = aditya@gmail.com                                     │
│    name       = Aditya                                                │
│    password   = $2a$10$hashedPasswordValue                           │
│    image      = https://example.com/aditya.png                       │
│    enable     = true                                                  │
│    created_at = 2026-06-27T15:00:00Z                                 │
│    updated_at = 2026-06-27T15:00:00Z                                 │
│    provider   = LOCAL                                                 │
│    roles      = [{ROLE_USER}]                                        │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  HIBERNATE HYDRATION                                                  │
│  Converts DB row → User entity object                                │
│  Uses @NoArgsConstructor to instantiate, then setters to populate    │
│  Returns: Optional.of(user)                                          │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  SERVICE LAYER — Back in UserServiceImpl                              │
│  .orElseThrow() → Optional is PRESENT → unwraps User entity         │
│  modelMapper.map(user, UserDto.class) →                              │
│  Creates UserDto with all fields copied from User                    │
│  Returns: UserDto object                                              │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER — Back in UserController                            │
│  ResponseEntity.ok(userDto) →                                         │
│  Creates ResponseEntity<UserDto> with status=200, body=userDto       │
│  Returns to DispatcherServlet                                         │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  JACKSON SERIALIZATION                                                │
│  MappingJackson2HttpMessageConverter converts UserDto → JSON          │
│  Reads getters: getId(), getEmail(), getName(), getPassword(), ...   │
│  Serializes Instant as ISO-8601 string                               │
│  Serializes Provider enum as string "LOCAL"                          │
│  Serializes Set<Role> as JSON array                                  │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  HTTP RESPONSE                                                        │
│  Status: 200 OK                                                       │
│  Content-Type: application/json                                       │
│  Body: { "id": "...", "email": "aditya@gmail.com", "name": "..." }  │
└──────────────────────────────────────────────────────────────────────┘
```

### ❌ Failure Scenario (Email Not Found)

```
┌──────────────────────────────────────────────────────────────────────┐
│  CLIENT REQUEST                                                       │
│  GET /api/v1/users/email/aditya@gmail.com                            │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  CONTROLLER → SERVICE → REPOSITORY → DATABASE                        │
│  SQL: SELECT * FROM users WHERE user_email = 'aditya@gmail.com'      │
│  Result: No rows returned                                             │
│  Repository returns: Optional.empty()                                 │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  SERVICE LAYER — UserServiceImpl                                      │
│  .orElseThrow() → Optional is EMPTY                                  │
│  Lambda executes: new ResourceNotFoundException(                      │
│      "User not found with the give email Id")                        │
│  THROWS ResourceNotFoundException  ← 💥 EXCEPTION                    │
└──────────────────────────┬───────────────────────────────────────────┘
                           │  exception propagates up
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER — Exception passes through (no try-catch)           │
└──────────────────────────┬───────────────────────────────────────────┘
                           │  exception propagates up
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  DISPATCHER SERVLET → ExceptionHandlerExceptionResolver               │
│  Finds matching handler: GlobalExceptionHandler                       │
│      .handleResourceNotFoundException()                               │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  GLOBAL EXCEPTION HANDLER                                             │
│  Creates: ErrorResponse(                                              │
│      message    = "User not found with the give email Id",           │
│      status     = HttpStatus.NOT_FOUND,                               │
│      statusCode = 404                                                 │
│  )                                                                    │
│  Returns: ResponseEntity.status(404).body(errorResponse)              │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  JACKSON SERIALIZATION                                                │
│  Converts ErrorResponse record → JSON                                 │
│  { "message": "User not found...", "status": "NOT_FOUND",            │
│    "statusCode": 404 }                                                │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│  HTTP RESPONSE                                                        │
│  Status: 404 Not Found                                                │
│  Content-Type: application/json                                       │
│  Body: { "message": "User not found with the give email Id",         │
│          "status": "NOT_FOUND", "statusCode": 404 }                  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 8. Annotation Reference (In Context of Execution)

| Annotation | Layer | Purpose in This API |
|---|---|---|
| `@RestController` | Controller | Registers `UserController` as a controller + enables JSON serialization of return values |
| `@RequestMapping("/api/v1/users")` | Controller | Sets base URL path for all endpoints |
| `@AllArgsConstructor` | Controller | Generates constructor for DI of `UserService` |
| `@GetMapping("/email/{emailId}")` | Controller | Maps `GET /api/v1/users/email/{emailId}` to `getUserByEmail()` |
| `@PathVariable("emailId")` | Controller | Binds `{emailId}` from URL to method parameter `email` |
| `@Service` | Service | Registers `UserServiceImpl` as a Spring bean |
| `@RequiredArgsConstructor` | Service | Generates constructor for DI of `UserRepository` and `ModelMapper` |
| `@Override` | Service | Compile-time check that method matches interface |
| `@Entity` | Entity | Marks `User` as a JPA managed entity |
| `@Table(name="users")` | Entity | Maps entity to `users` DB table |
| `@Id` | Entity | Marks `id` as primary key |
| `@GeneratedValue(strategy=UUID)` | Entity | Auto-generates UUID for new entities |
| `@Column(name, unique, length)` | Entity | Configures column mapping and constraints |
| `@Enumerated(EnumType.STRING)` | Entity | Stores enum as string, not ordinal |
| `@ManyToMany(fetch=EAGER)` | Entity | Eagerly loads roles with the user |
| `@JoinTable` | Entity | Configures the `user_roles` join table |
| `@Getter @Setter` | Entity/DTO | Generates getters and setters for all fields |
| `@Builder` | Entity/DTO | Generates builder pattern for object creation |
| `@NoArgsConstructor` | Entity/DTO | Required by JPA for entity instantiation |
| `@Configuration` | Config | Marks `ProjectConfig` as a configuration class |
| `@Bean` | Config | Registers `ModelMapper` as a Spring bean |
| `@RestControllerAdvice` | Exception | Registers global exception handler for all controllers |
| `@ExceptionHandler(...)` | Exception | Maps specific exception types to handler methods |

---

## 9. Summary

### 📌 Execution Flow (Method Call Sequence)

```
Client
  → DispatcherServlet.doDispatch()
    → RequestMappingHandlerMapping.getHandler()                    // finds getUserByEmail()
    → RequestMappingHandlerAdapter.handle()
      → PathVariableMethodArgumentResolver.resolveArgument()       // extracts "aditya@gmail.com"
      → UserController.getUserByEmail("aditya@gmail.com")
        → UserServiceImpl.getUserByEmail("aditya@gmail.com")
          → UserRepository.findByEmail("aditya@gmail.com")        // Spring Data JPA proxy
            → Hibernate → SQL → Database                           // actual DB query
          ← Optional<User>                                         // result from DB
          → Optional.orElseThrow(...)                              // unwrap or throw
          → ModelMapper.map(user, UserDto.class)                   // entity → DTO conversion
        ← UserDto                                                  // returned to controller
      → ResponseEntity.ok(userDto)                                 // wrap in 200 OK
    → MappingJackson2HttpMessageConverter.write()                   // serialize to JSON
  ← HTTP 200 OK + JSON body
```

### 📌 Data Flow (Type Transformations)

```
URL String "aditya@gmail.com"
    ↓ @PathVariable extraction
String email = "aditya@gmail.com"
    ↓ passed to service
String email = "aditya@gmail.com"
    ↓ passed to repository
String email = "aditya@gmail.com"
    ↓ bound to SQL parameter
SQL: WHERE user_email = 'aditya@gmail.com'
    ↓ query result
Database Row (raw data)
    ↓ Hibernate hydration
User entity (Java object)
    ↓ wrapped in Optional
Optional<User>
    ↓ .orElseThrow() unwrap
User entity
    ↓ ModelMapper.map()
UserDto (Java object)
    ↓ ResponseEntity.ok()
ResponseEntity<UserDto> (status=200, body=UserDto)
    ↓ Jackson serialization
JSON String
    ↓ HTTP response
Client receives JSON
```

### 📌 Objects Created During Execution

| # | Object | Created By | Purpose |
|---|--------|-----------|---------|
| 1 | `HttpServletRequest` | Tomcat | Wraps the raw HTTP request |
| 2 | `String email` | PathVariableMethodArgumentResolver | Holds extracted email value |
| 3 | `SQL PreparedStatement` | Hibernate/JDBC | Executes the database query |
| 4 | `User` entity | Hibernate (via `@NoArgsConstructor` + setters) | Hydrated from DB result row |
| 5 | `Optional<User>` | Spring Data JPA proxy | Wraps nullable query result |
| 6 | `UserDto` | ModelMapper (via `@NoArgsConstructor` + setters) | DTO copy of User entity |
| 7 | `ResponseEntity<UserDto>` | `ResponseEntity.ok()` | Wraps DTO with HTTP 200 status |
| 8 | JSON String | Jackson `ObjectMapper` | Serialized response body |
| 9 | `HttpServletResponse` | Tomcat | Wraps the raw HTTP response sent to client |

### 📌 Objects Created in Failure Path (additionally)

| # | Object | Created By | Purpose |
|---|--------|-----------|---------|
| 1 | `Optional.empty()` | Spring Data JPA proxy | No user found |
| 2 | `ResourceNotFoundException` | Lambda in `.orElseThrow()` | Exception with message |
| 3 | `ErrorResponse` record | `GlobalExceptionHandler` | Structured error response |
| 4 | `ResponseEntity<ErrorResponse>` | `GlobalExceptionHandler` | Wraps error with HTTP 404 status |

### 📌 Methods Called (In Order)

| # | Method | Class | Input | Output |
|---|--------|-------|-------|--------|
| 1 | `doDispatch()` | `DispatcherServlet` | `HttpServletRequest` | Delegates to handler |
| 2 | `getHandler()` | `RequestMappingHandlerMapping` | Request URL + HTTP method | `HandlerExecutionChain` |
| 3 | `resolveArgument()` | `PathVariableMethodArgumentResolver` | URI template variables | `"aditya@gmail.com"` |
| 4 | `getUserByEmail()` | `UserController` | `String email` | `ResponseEntity<UserDto>` |
| 5 | `getUserByEmail()` | `UserServiceImpl` | `String email` | `UserDto` |
| 6 | `findByEmail()` | `UserRepository` (proxy) | `String email` | `Optional<User>` |
| 7 | `orElseThrow()` | `Optional<User>` | `Supplier<Exception>` | `User` or throws |
| 8 | `map()` | `ModelMapper` | `User, UserDto.class` | `UserDto` |
| 9 | `ok()` | `ResponseEntity` (static) | `UserDto` | `ResponseEntity<UserDto>` |
| 10 | `write()` | `MappingJackson2HttpMessageConverter` | `UserDto` | JSON bytes |

### 📌 Database Operations

| # | Operation | SQL | Table(s) | Result |
|---|-----------|-----|----------|--------|
| 1 | SELECT | `SELECT * FROM users WHERE user_email = ?` | `users` | 0 or 1 row |
| 2 | JOIN (EAGER) | `LEFT JOIN user_roles ON ... LEFT JOIN roles ON ...` | `user_roles`, `roles` | Role data for user |

### 📌 Final Response

#### ✅ Success (200 OK)
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "aditya@gmail.com",
  "name": "Aditya",
  "password": "$2a$10$hashedPasswordValue",
  "image": "https://example.com/aditya.png",
  "enable": true,
  "createdAt": "2026-06-27T15:00:00Z",
  "updatedAt": "2026-06-27T15:00:00Z",
  "provider": "LOCAL",
  "roles": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "name": "ROLE_USER"
    }
  ]
}
```

#### ❌ Failure (404 Not Found)
```json
{
  "message": "User not found with the give email Id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

---

> [!TIP]
> **Interview Tip:** When explaining this API in an interview, emphasize:
> 1. **Spring Data JPA derived queries** — how `findByEmail()` generates SQL from the method name.
> 2. **Optional handling** — the difference between `.orElseThrow()`, `.get()`, and `.orElse()`.
> 3. **Entity-DTO separation** — why we don't return the `User` entity directly.
> 4. **Global exception handling** — how `@RestControllerAdvice` + `@ExceptionHandler` provides centralized error responses.
> 5. **Eager vs. Lazy fetching** — why `FetchType.EAGER` is used on roles and the `LazyInitializationException` problem it solves.

---

*Document generated for interview preparation — covers the complete execution trace of `GET /api/v1/users/email/{emailId}`.*
