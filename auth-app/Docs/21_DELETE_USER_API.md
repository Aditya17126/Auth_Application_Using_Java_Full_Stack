# 🗑️ DELETE USER API — Complete Execution Trace

## Table of Contents
1. [API Endpoint Details](#1-api-endpoint-details)
2. [Real-World Example Scenario](#2-real-world-example-scenario)
3. [Security Filter Chain Execution](#3-security-filter-chain-execution)
4. [Complete Step-by-Step Execution Flow](#4-complete-step-by-step-execution-flow)
5. [Line-by-Line Code Breakdown](#5-line-by-line-code-breakdown)
6. [Data Flow Tracking](#6-data-flow-tracking)
7. [Annotation Explanations in Context](#7-annotation-explanations-in-context)
8. [Summary Section](#8-summary-section)

---

## 1. API Endpoint Details

| Property            | Value                                              |
|---------------------|----------------------------------------------------|
| **HTTP Method**     | `DELETE`                                           |
| **URL**             | `/api/v1/users/{userId}`                           |
| **Path Variable**   | `userId` — a UUID string (e.g., `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"`) |
| **Request Body**    | ❌ None                                            |
| **Request Headers** | `Content-Type` not required (no body)              |
| **Success Response**| `200 OK` with **empty body**                       |
| **Error Response**  | `404 NOT FOUND` with `ErrorResponse` JSON body     |

### Why 200 OK and NOT 204 No Content?

```java
@DeleteMapping("/{userId}")
public void deleteUser(@PathVariable("userId") String userId){
   userService.deleteUser(userId);
}
```

> [!IMPORTANT]
> The controller method returns `void` — **not** `ResponseEntity<Void>`. When a Spring MVC handler method returns `void` and does not write directly to the `HttpServletResponse`, Spring's `RequestResponseBodyMethodProcessor` sets the HTTP status to **200 OK** with an **empty body** by default. If the developer wanted `204 No Content`, they would need to either:
> - Return `ResponseEntity.noContent().build()` (type `ResponseEntity<Void>`)
> - Or annotate the method with `@ResponseStatus(HttpStatus.NO_CONTENT)`

### Success Response

```
HTTP/1.1 200 OK
Content-Length: 0
```
*(Empty body — no JSON, no text, nothing)*

### Error Response (User Not Found)

```json
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "message": "User not found with the given id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

### Error Response (Invalid UUID Format)

```json
HTTP/1.1 500 Internal Server Error
```
*(An `IllegalArgumentException` from `UUID.fromString()` — there is no explicit handler for this in `GlobalExceptionHandler`, so Spring returns a generic 500 unless a fallback handler exists)*

---

## 2. Real-World Example Scenario

### Scenario: Aditya Wants to Delete a User Account

**Context:** Aditya is an administrator of the auth application. A user account with UUID `a1b2c3d4-e5f6-7890-abcd-ef1234567890` needs to be removed from the system. This user has two roles (`ROLE_USER` and `ROLE_ADMIN`) assigned via the `user_roles` join table.

**Database state BEFORE deletion:**

**`users` table:**

| user_id                              | user_email           | name    | password   | enable | provider | created_at          |
|--------------------------------------|----------------------|---------|------------|--------|----------|---------------------|
| a1b2c3d4-e5f6-7890-abcd-ef1234567890 | aditya@example.com   | Aditya  | $2a$10$... | true   | LOCAL    | 2026-06-20T10:00:00Z |

**`user_roles` join table:**

| user_id                              | role_id                              |
|--------------------------------------|--------------------------------------|
| a1b2c3d4-e5f6-7890-abcd-ef1234567890 | 11111111-1111-1111-1111-111111111111 |
| a1b2c3d4-e5f6-7890-abcd-ef1234567890 | 22222222-2222-2222-2222-222222222222 |

**`roles` table (unchanged after deletion):**

| id                                   | name        |
|--------------------------------------|-------------|
| 11111111-1111-1111-1111-111111111111 | ROLE_USER   |
| 22222222-2222-2222-2222-222222222222 | ROLE_ADMIN  |

### What Aditya sends:

```
DELETE http://localhost:8080/api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### What Aditya receives (Success):

```
HTTP/1.1 200 OK
(empty body)
```

### Database state AFTER deletion:

**`users` table:** Row with `a1b2c3d4-e5f6-7890-abcd-ef1234567890` is **DELETED**.

**`user_roles` table:** Both rows for this user are **DELETED** (Hibernate manages this because the `User` entity owns the `@ManyToMany` relationship).

**`roles` table:** **UNCHANGED** — roles `ROLE_USER` and `ROLE_ADMIN` still exist for other users.

---

## 3. Security Filter Chain Execution

> [!NOTE]
> **Spring Security is COMMENTED OUT** in this project. There is no `SecurityFilterChain` bean, no `JwtAuthFilter`, and no authentication/authorization checks active.

### What This Means for the DELETE API

```
Client Request
     ↓
Tomcat Servlet Container
     ↓
Spring's DispatcherServlet (NO security filters intercept)
     ↓
UserController.deleteUser()
```

- **No JWT token is required** in the request headers.
- **No role-based access control** is enforced — anyone can delete any user.
- **No authentication** is checked — anonymous requests are accepted.
- The request flows directly from the servlet container through Spring's `DispatcherServlet` to the controller.

### If Security Were Enabled (What Would Happen)

If Spring Security were active, a typical flow would be:

```
Client Request (with Authorization: Bearer <jwt-token>)
     ↓
JwtAuthFilter.doFilterInternal()
     ↓
Extract JWT → Validate Token → Load UserDetails → Set SecurityContext
     ↓
AuthorizationFilter checks roles/permissions
     ↓
UserController.deleteUser()
```

But since security is commented out, **none of this happens**. The request goes straight through.

---

## 4. Complete Step-by-Step Execution Flow

### High-Level Architecture Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        CLIENT REQUEST                                   │
│  DELETE /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890             │
└───────────────────────────────┬─────────────────────────────────────────┘
                                ↓
┌───────────────────────────────┴─────────────────────────────────────────┐
│                     TOMCAT SERVLET CONTAINER                            │
│  Receives raw HTTP request, creates HttpServletRequest/Response         │
└───────────────────────────────┬─────────────────────────────────────────┘
                                ↓
┌───────────────────────────────┴─────────────────────────────────────────┐
│                     SPRING DispatcherServlet                            │
│  Matches URL pattern → finds UserController.deleteUser()                │
│  Extracts path variable "userId" from URL                               │
└───────────────────────────────┬─────────────────────────────────────────┘
                                ↓
┌───────────────────────────────┴─────────────────────────────────────────┐
│                     UserController (REST Layer)                         │
│  deleteUser("a1b2c3d4-e5f6-7890-abcd-ef1234567890")                    │
│  Delegates to userService.deleteUser(userId)                            │
└───────────────────────────────┬─────────────────────────────────────────┘
                                ↓
┌───────────────────────────────┴─────────────────────────────────────────┐
│                     UserServiceImpl (Service Layer)                      │
│  1. Parse UUID string → UUID object                                     │
│  2. Query DB: SELECT * FROM users WHERE user_id = ?                     │
│  3. If found → Delete user (and join table rows)                        │
│  3. If not found → throw ResourceNotFoundException                      │
└───────────────────────────────┬─────────────────────────────────────────┘
                                ↓
┌───────────────────────────────┴─────────────────────────────────────────┐
│                     UserRepository (Data Layer)                          │
│  findById(UUID) → Hibernate generates SELECT SQL                        │
│  delete(User)   → Hibernate generates DELETE SQL(s)                     │
└───────────────────────────────┬─────────────────────────────────────────┘
                                ↓
┌───────────────────────────────┴─────────────────────────────────────────┐
│                     PostgreSQL / MySQL DATABASE                          │
│  1. SELECT from users table (+ LEFT JOIN user_roles + roles for EAGER)  │
│  2. DELETE FROM user_roles WHERE user_id = ?                            │
│  3. DELETE FROM users WHERE user_id = ?                                 │
└───────────────────────────────┬─────────────────────────────────────────┘
                                ↓
┌───────────────────────────────┴─────────────────────────────────────────┐
│                     RESPONSE                                            │
│  HTTP 200 OK (empty body)                                               │
└─────────────────────────────────────────────────────────────────────────┘
```

### Detailed Step-by-Step

#### Step 1: HTTP Request Arrives at Tomcat

```
DELETE /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890 HTTP/1.1
Host: localhost:8080
```

- Tomcat's `Connector` receives the raw TCP connection.
- Tomcat creates an `HttpServletRequest` object containing:
  - Method: `DELETE`
  - URI: `/api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890`
  - No request body
- Tomcat creates an `HttpServletResponse` object (initially empty).
- The request is passed to Spring's `DispatcherServlet`.

#### Step 2: DispatcherServlet Routes the Request

- `DispatcherServlet.doDispatch()` is called.
- It consults the `HandlerMapping` registry to find a handler for `DELETE /api/v1/users/{userId}`.
- The `RequestMappingHandlerMapping` matches:
  - Class-level: `@RequestMapping("/api/v1/users")` on `UserController`
  - Method-level: `@DeleteMapping("/{userId}")` on `deleteUser()`
- Combined URL pattern: `/api/v1/users/{userId}` matches the incoming URL.
- The path segment `a1b2c3d4-e5f6-7890-abcd-ef1234567890` is extracted as the value for `{userId}`.
- `DispatcherServlet` creates a `HandlerExecutionChain` and invokes the handler via `RequestMappingHandlerAdapter`.

#### Step 3: Argument Resolution

- Spring's `HandlerMethodArgumentResolver` pipeline resolves the method parameter:
  - `@PathVariable("userId") String userId`
  - The `PathVariableMethodArgumentResolver` extracts `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` from the URI template.
  - The value is passed as a `String` (no type conversion needed — the parameter is `String`).

#### Step 4: Controller Method Executes

```java
@DeleteMapping("/{userId}")
public void deleteUser(@PathVariable("userId") String userId){
   userService.deleteUser(userId);
}
```

- `deleteUser("a1b2c3d4-e5f6-7890-abcd-ef1234567890")` is invoked.
- The `userService` field (of type `UserService`, runtime type `UserServiceImpl`) is injected via constructor injection (thanks to `@AllArgsConstructor` on the controller).
- `userService.deleteUser("a1b2c3d4-e5f6-7890-abcd-ef1234567890")` is called.
- The method returns `void` — no return value processing by Spring except setting status 200.

#### Step 5: Service Layer — UUID Parsing

```java
UUID uId = UserHelper.parseUUID(userId);
```

- `UserHelper.parseUUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890")` is called.
- Inside `UserHelper`:

```java
public static UUID parseUUID(String uuid) {
   return UUID.fromString(uuid);
}
```

- `UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")` converts the string to a `UUID` object.
- Returns: `UUID` object → `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- Stored in local variable `uId`.

> [!WARNING]
> If the string is NOT a valid UUID (e.g., `"invalid-uuid"`), `UUID.fromString()` throws `IllegalArgumentException`. There is **no explicit handler** for `IllegalArgumentException` in `GlobalExceptionHandler`, so Spring will return a **500 Internal Server Error** with a stack trace (or a generic error, depending on server config). This is a gap in error handling.

#### Step 6: Service Layer — Database Lookup

```java
User user = userRepository.findById(uId)
    .orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"));
```

This is a compound operation. Let's break it into sub-steps:

**Step 6a: `userRepository.findById(uId)`**

- `userRepository` is of type `UserRepository`, which extends `JpaRepository<User, UUID>`.
- `findById(UUID)` is inherited from `CrudRepository` (parent of `JpaRepository`).
- At runtime, Spring Data JPA's proxy (`SimpleJpaRepository`) calls `EntityManager.find(User.class, uId)`.
- Hibernate generates the following SQL:

```sql
SELECT
    u.user_id, u.user_email, u.name, u.password, u.image,
    u.enable, u.created_at, u.updated_at, u.provider
FROM users u
WHERE u.user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

- Because `roles` is `FetchType.EAGER`, Hibernate **also** eagerly fetches the roles in the same transaction:

```sql
SELECT
    r.id, r.name
FROM user_roles ur
JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

> [!NOTE]
> Hibernate may combine these into a single LEFT JOIN query or issue them as separate SELECTs — this depends on the Hibernate version and fetch strategy. With `FetchType.EAGER` on a `@ManyToMany`, Hibernate typically performs a join fetch or a secondary SELECT.

**Step 6b: If User Exists — Success Path**

- `findById()` returns `Optional<User>` wrapping the found `User` entity.
- `.orElseThrow(...)` unwraps the `Optional`, returning the `User` object.
- The `User` object is stored in local variable `user` with:
  - `id`: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
  - `email`: `"aditya@example.com"`
  - `name`: `"Aditya"`
  - `password`: `"$2a$10$..."`
  - `image`: `null` (or some URL)
  - `enable`: `true`
  - `provider`: `Provider.LOCAL`
  - `roles`: `Set<Role>` containing `{ROLE_USER, ROLE_ADMIN}`
  - `createdAt`: `2026-06-20T10:00:00Z`
  - `updatedAt`: `2026-06-25T15:30:00Z`

**Step 6c: If User Does NOT Exist — Error Path**

- `findById()` returns `Optional.empty()`.
- `.orElseThrow(...)` executes the lambda: `() -> new ResourceNotFoundException("User not found with the given id")`
- A new `ResourceNotFoundException` is created with message `"User not found with the given id"`.
- The exception is thrown and propagates up the call stack.
- Spring's `@RestControllerAdvice` class `GlobalExceptionHandler` catches it:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception){
    ErrorResponse internalServerError = new ErrorResponse(
        exception.getMessage(),    // "User not found with the given id"
        HttpStatus.NOT_FOUND,      // NOT_FOUND enum
        404                         // int status code
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
}
```

- Returns HTTP **404** with JSON body:

```json
{
  "message": "User not found with the given id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

> [!TIP]
> Note the unfortunate variable name `internalServerError` for a 404 response — this is a copy-paste artifact from another handler. It doesn't affect functionality, but in a code review or interview, pointing this out shows attention to detail.

#### Step 7: Service Layer — Deleting the User

```java
userRepository.delete(user);
```

- `delete(User)` is inherited from `CrudRepository`.
- At runtime, Spring Data JPA's `SimpleJpaRepository.delete()` calls:
  1. `EntityManager.contains(user)` — checks if the entity is already managed in the persistence context.
  2. If managed: calls `EntityManager.remove(user)` directly.
  3. If detached: calls `EntityManager.merge(user)` first to re-attach, then `EntityManager.remove(mergedUser)`.

- Since we just loaded the user via `findById()` in the **same transaction**, the entity is **already managed** in the persistence context. Hibernate calls `EntityManager.remove(user)` directly.

**What Hibernate Does on `remove()`:**

Because `User` has a `@ManyToMany` relationship with `Role` and `User` is the **owning side** (it has the `@JoinTable` annotation), Hibernate must:

1. **First**: Delete rows from the **join table** `user_roles`:

```sql
DELETE FROM user_roles
WHERE user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

2. **Then**: Delete the user row from the **`users` table**:

```sql
DELETE FROM users
WHERE user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

> [!IMPORTANT]
> **The order matters.** The `user_roles` table has a foreign key constraint pointing to `users.user_id`. If Hibernate tried to delete the user first, the database would throw a foreign key violation error. Hibernate knows the relationship graph and performs deletions in the correct order: **join table first, then entity table**.

> [!CAUTION]
> The `@ManyToMany` mapping does **NOT** have `CascadeType.REMOVE` or `CascadeType.ALL`. This means:
> - ✅ Rows in `user_roles` (the join table) ARE deleted — Hibernate always manages the join table for the owning side.
> - ❌ Rows in `roles` (the target table) are **NOT** deleted — the `Role` entities survive. This is correct behavior: roles are shared resources, not owned by a single user.
> - If `CascadeType.REMOVE` were added to the `roles` field, deleting a user would **cascade delete the Role entities too**, which would be catastrophic — it would delete roles used by other users.

#### Step 8: Transaction Commits

- Since the service method `deleteUser()` does **not** have `@Transactional`, Spring Data JPA's `SimpleJpaRepository.delete()` runs within its **own** default transaction (provided by the repository proxy's `@Transactional` on the `delete` method in `SimpleJpaRepository`).
- When the repository method completes, the transaction is committed.
- Hibernate flushes the persistence context, executing the two DELETE SQL statements against the database.
- The database permanently removes the rows.

> [!NOTE]
> Although `UserServiceImpl` has `@Transactional` imported (`import jakarta.transaction.Transactional`), the `deleteUser()` method itself is **NOT** annotated with `@Transactional`. The `findById()` and `delete()` calls run in **separate** implicit transactions provided by Spring Data JPA's repository proxy. In this particular flow, this is functionally fine because:
> 1. `findById()` loads the user (Transaction 1 — committed, user entity is now detached).
> 2. `delete()` re-attaches and deletes (Transaction 2 — `SimpleJpaRepository.delete()` has its own `@Transactional`, so it merges the detached entity and removes it).
>
> However, wrapping both in a single `@Transactional` at the service level would be **better practice** — it avoids the overhead of re-attaching a detached entity and ensures atomicity if more operations were added later.

#### Step 9: Response Returned

- `userService.deleteUser(userId)` returns `void`.
- `UserController.deleteUser()` returns `void`.
- Spring's `RequestResponseBodyMethodProcessor` sees no return value.
- It sets the HTTP status to **200 OK** with an empty response body.
- The `HttpServletResponse` is committed and sent back to the client through Tomcat.

```
HTTP/1.1 200 OK
Content-Length: 0
Date: Fri, 27 Jun 2026 15:18:24 GMT
```

---

## 5. Line-by-Line Code Breakdown

### 5.1 UserController.java

```java
@RestController
```
- **What it does:** Combines `@Controller` + `@ResponseBody`. Registers this class as a Spring MVC controller where every method's return value is serialized directly to the HTTP response body (not resolved as a view name).
- **Why needed:** Without it, Spring would try to resolve the return value as a JSP/Thymeleaf view name.
- **If removed:** Spring would not register this class as a request handler. The DELETE endpoint would not exist. Clients would get a 404 for any request to `/api/v1/users/**`.

```java
@RequestMapping("/api/v1/users")
```
- **What it does:** Sets the base URL prefix for all handler methods in this controller.
- **Why needed:** All endpoints in this controller share the `/api/v1/users` prefix. This avoids repeating it on every method.
- **If removed:** The `@DeleteMapping("/{userId}")` would become just `/{userId}`, meaning the DELETE endpoint would be at `DELETE /{userId}` — a root-level path like `DELETE /a1b2c3d4-...`, which is semantically wrong and conflicts with other routes.

```java
@AllArgsConstructor
```
- **What it does:** Lombok annotation that generates a constructor with one parameter for every field in the class.
- **Generated code:**
  ```java
  public UserController(UserService userService) {
      this.userService = userService;
  }
  ```
- **Why needed:** Spring uses this constructor for **dependency injection**. Since there's only one constructor, Spring automatically injects the `UserService` bean without needing `@Autowired`.
- **If removed:** No constructor is generated. The `userService` field would be `null` at runtime. Calling `userService.deleteUser(userId)` would throw a `NullPointerException`.

```java
public class UserController {
```
- **What it does:** Declares the controller class.
- **Spring's interaction:** At startup, Spring's component scan detects `@RestController`, creates a singleton instance of `UserController`, and registers its handler mappings.

```java
private final UserService userService;
```
- **What it does:** Declares a field of type `UserService` (an interface). The `final` keyword ensures it must be initialized in the constructor and cannot be reassigned.
- **Runtime type:** `UserServiceImpl` — Spring injects the concrete implementation because `UserServiceImpl` is annotated with `@Service`, making it the only bean implementing `UserService`.
- **If removed:** The controller has no way to access the service layer. No business logic can be invoked.

```java
@DeleteMapping("/{userId}")
```
- **What it does:** Maps HTTP `DELETE` requests to this method. The `/{userId}` is a URI template variable that captures the path segment after `/api/v1/users/`.
- **Combined with class-level `@RequestMapping`:** Full URL becomes `DELETE /api/v1/users/{userId}`.
- **If removed:** No handler exists for `DELETE /api/v1/users/{userId}`. Clients would get a `405 Method Not Allowed` (if other methods exist at that path) or `404 Not Found`.

```java
public void deleteUser(@PathVariable("userId") String userId){
```
- **What it does:** Declares the handler method with `void` return type. The parameter `userId` is extracted from the URL path.
- **`@PathVariable("userId")`:** Tells Spring to bind the `{userId}` segment from the URL to this parameter. The `"userId"` in the annotation matches the `{userId}` in `@DeleteMapping("/{userId}")`.
- **Parameter type `String`:** The UUID is received as a raw string. No automatic conversion to `UUID` type — the service layer handles conversion.
- **`void` return type:** Spring will return HTTP 200 OK with an empty body (not 204 No Content).
- **If `@PathVariable` removed:** Spring would not extract the path variable. The parameter `userId` would be `null`. `userService.deleteUser(null)` would be called, leading to a `NullPointerException` when `UserHelper.parseUUID(null)` tries to parse `null`.

```java
   userService.deleteUser(userId);
```
- **What it does:** Delegates the entire delete operation to the service layer.
- **Object:** `userService` (runtime: `UserServiceImpl`)
- **Data passed:** `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` (String)
- **Returns:** `void`
- **If removed:** The endpoint would do absolutely nothing — just return 200 OK. No deletion would ever happen.

---

### 5.2 UserServiceImpl.java

```java
@Service
```
- **What it does:** Marks this class as a Spring service component. Spring creates a singleton instance and registers it in the application context.
- **Why needed:** Without it, Spring won't detect this class during component scanning. The `UserService` bean would not exist, and `UserController` would fail to start (dependency injection failure: `No qualifying bean of type 'UserService'`).

```java
@RequiredArgsConstructor
```
- **What it does:** Lombok generates a constructor for all `final` fields.
- **Generated code:**
  ```java
  public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
      this.userRepository = userRepository;
      this.modelMapper = modelMapper;
  }
  ```
- **Why needed:** Enables constructor-based dependency injection for `userRepository` and `modelMapper`.
- **If removed:** Both fields would be `null` at runtime → `NullPointerException` on first use.

```java
public class UserServiceImpl implements UserService {
```
- **What it does:** Declares the class and implements the `UserService` interface.
- **Why `implements UserService`:** Allows Spring to inject this as a `UserService` type in the controller. Follows the Interface Segregation Principle — the controller depends on the abstraction, not the implementation.

```java
private final UserRepository userRepository;
```
- **What it does:** Declares the repository dependency. `final` ensures immutability after construction.
- **Runtime type:** A Spring Data JPA proxy class that implements `UserRepository` with auto-generated query implementations.
- **Used in `deleteUser`:** For `findById()` and `delete()` calls.

```java
private final ModelMapper modelMapper;
```
- **What it does:** Declares the ModelMapper dependency, injected from the `ProjectConfig` bean.
- **Used in `deleteUser`:** **NOT used.** The `deleteUser` method never maps between DTO and entity. It's present because `UserServiceImpl` has other methods that need it.
- **If removed:** Would break other methods in the class that use `modelMapper` (e.g., `getUser`, `createUser`). Would NOT affect `deleteUser`.

```java
@Override
public void deleteUser(String userId) {
```
- **`@Override`:** Compile-time check that this method correctly overrides a method declared in the `UserService` interface.
- **`void` return type:** The method performs a side-effect (deletion) and returns nothing.
- **Parameter `String userId`:** Receives the raw UUID string from the controller.

```java
   UUID uId = UserHelper.parseUUID(userId);
```
- **What it does:** Converts the `String` UUID to a `java.util.UUID` object.
- **Object calling:** `UserHelper` (static utility class) — no instance created.
- **Input:** `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` (String)
- **Output:** `UUID` object representing `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- **Stored in:** Local variable `uId`
- **Why needed:** `JpaRepository<User, UUID>` expects a `UUID` as the ID type. `findById()` requires a `UUID`, not a `String`.
- **If removed:** You cannot call `userRepository.findById(userId)` because `findById` expects `UUID`, not `String`. Compilation error: `incompatible types: String cannot be converted to UUID`.
- **What breaks with bad input:** If `userId` = `"not-a-uuid"`, `UUID.fromString()` throws `IllegalArgumentException: Invalid UUID string: not-a-uuid`. This is **unhandled** in the current `GlobalExceptionHandler`.

```java
   User user = userRepository.findById(uId)
       .orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"));
```

Breaking this compound statement into parts:

**Part 1: `userRepository.findById(uId)`**
- **What it does:** Queries the database for a `User` with the given primary key.
- **Object calling:** `userRepository` (Spring Data JPA proxy)
- **Input:** `UUID` object `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- **Output:** `Optional<User>` — either `Optional.of(user)` or `Optional.empty()`
- **SQL generated:**
  ```sql
  SELECT * FROM users WHERE user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
  ```
  Plus eager fetch of roles:
  ```sql
  SELECT r.* FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = 'a1b2c3d4-...'
  ```
- **Why needed:** We must verify the user exists before attempting to delete. Also, JPA's `delete()` method requires a managed/known entity instance — not just an ID.
- **If removed:** We cannot get a `User` entity to pass to `delete()`. We could use `userRepository.deleteById(uId)` instead, but that method throws `EmptyResultDataAccessException` (not our custom `ResourceNotFoundException`) if the user doesn't exist (in older Spring Data versions), or silently does nothing (in Spring Data 3.x+).

**Part 2: `.orElseThrow(() -> new ResourceNotFoundException("User not found with the given id"))`**
- **What it does:** If the `Optional` is empty, executes the `Supplier` lambda and throws the returned exception.
- **If `Optional` has value:** Unwraps and returns the `User` object.
- **If `Optional` is empty:** Creates `new ResourceNotFoundException("User not found with the given id")` and throws it.
- **Why a lambda `() ->`:** `orElseThrow` takes a `Supplier<? extends Throwable>`. The lambda is a `Supplier` that lazily creates the exception only when needed. If the user exists, the exception object is never created — saving memory.
- **If removed (just used `.get()`):** `Optional.get()` throws `NoSuchElementException` if empty — a generic, unclear error. Using `orElseThrow` gives a domain-specific error message.

```java
   userRepository.delete(user);
```
- **What it does:** Deletes the `User` entity from the database.
- **Object calling:** `userRepository` (Spring Data JPA proxy → `SimpleJpaRepository`)
- **Input:** `User` entity object (the one loaded from `findById`)
- **Returns:** `void`
- **Internal flow of `SimpleJpaRepository.delete()`:**
  ```java
  @Transactional
  public void delete(T entity) {
      Assert.notNull(entity, "Entity must not be null");
      if (entityInformation.isNew(entity)) {
          return; // New, unsaved entity — nothing to delete
      }
      Class<?> type = ProxyUtils.getUserClass(entity);
      T existing = (T) entityManager.find(type, entityInformation.getId(entity));
      if (existing == null) {
          return; // Entity not in DB — nothing to delete
      }
      entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
  }
  ```
- **SQL generated (in order):**
  ```sql
  -- 1. Remove join table entries
  DELETE FROM user_roles WHERE user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'

  -- 2. Remove user row
  DELETE FROM users WHERE user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
  ```
- **Why needed:** This is the core operation — the reason this API exists.
- **If removed:** The API would find the user, verify it exists, then do nothing. It would still return 200 OK, but the user would remain in the database. A silent no-op.

---

### 5.3 UserHelper.java

```java
public class UserHelper {
```
- **What it does:** A plain Java utility class. Not a Spring component — no `@Component` or `@Service`. Methods are `static`.

```java
   public static UUID parseUUID(String uuid) {
      return UUID.fromString(uuid);
   }
```
- **What it does:** Wraps `UUID.fromString()` in a named helper method.
- **Why it exists:** Centralizes UUID parsing logic. If the team later wants to add input validation (e.g., trimming, null-check, custom error message), they only modify this one method.
- **Input:** `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` (String)
- **Output:** `UUID` object
- **If removed:** The service would need to call `UUID.fromString(userId)` directly — functionally equivalent but loses centralization.

---

### 5.4 ResourceNotFoundException.java

```java
public class ResourceNotFoundException extends RuntimeException{
```
- **What it does:** Custom unchecked exception (extends `RuntimeException`).
- **Why `RuntimeException`:** Unchecked exceptions don't need to be declared in method signatures (`throws` clause). They propagate automatically up the call stack to Spring's exception handler.

```java
   public ResourceNotFoundException(String message){ super(message); }
```
- **What it does:** Constructor that accepts a custom message and passes it to `RuntimeException`.
- **In our flow:** Message is `"User not found with the given id"`.
- **Accessed by:** `GlobalExceptionHandler` via `exception.getMessage()`.

---

### 5.5 GlobalExceptionHandler.java

```java
@RestControllerAdvice
```
- **What it does:** Combines `@ControllerAdvice` + `@ResponseBody`. Makes this class a global exception handler for all `@RestController` classes. Return values are serialized to JSON.
- **If removed:** No global exception handling. `ResourceNotFoundException` would propagate to Spring Boot's default error handler, returning a generic `WhiteLabel Error Page` or Spring Boot's default JSON error structure with a 500 status.

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception){
```
- **What it does:** Catches any `ResourceNotFoundException` thrown from any controller method.
- **Trigger:** When `deleteUser` throws `ResourceNotFoundException` because the user was not found.
- **Parameter:** Spring passes the caught exception object to this method.

```java
    ErrorResponse internalServerError = new ErrorResponse(
        exception.getMessage(),     // "User not found with the given id"
        HttpStatus.NOT_FOUND,       // NOT_FOUND
        404                          // 404
    );
```
- **What it does:** Creates an `ErrorResponse` record with the error details.
- **`ErrorResponse` is a Java `record`:** Automatically generates constructor, getters (`message()`, `status()`, `statusCode()`), `equals()`, `hashCode()`, and `toString()`.
- **The variable name `internalServerError`:** Misleading — this is a 404, not a 500. Copy-paste issue from another handler.

```java
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
```
- **What it does:** Creates a `ResponseEntity` with:
  - Status: `404 NOT_FOUND`
  - Body: `ErrorResponse` object (serialized to JSON by Jackson)
- **Output JSON:**
  ```json
  {
    "message": "User not found with the given id",
    "status": "NOT_FOUND",
    "statusCode": 404
  }
  ```

---

## 6. Data Flow Tracking

### Success Scenario — Complete Data Transformation Chain

```
Step 1: CLIENT
────────────────────────────────────────────────────────
Input:  DELETE /api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Data:   URL path contains UUID string
Body:   (none)

         ↓ Tomcat extracts URI path

Step 2: DispatcherServlet
────────────────────────────────────────────────────────
Input:  HttpServletRequest with URI "/api/v1/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
Action: Pattern matching → extracts path variable
Output: String "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

         ↓ PathVariableMethodArgumentResolver

Step 3: UserController.deleteUser()
────────────────────────────────────────────────────────
Input:  String userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
Action: Delegates to service
Output: (void — nothing returned)

         ↓ userService.deleteUser(userId)

Step 4: UserServiceImpl.deleteUser() — UUID Parsing
────────────────────────────────────────────────────────
Input:  String "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
Action: UserHelper.parseUUID() → UUID.fromString()
Output: UUID object a1b2c3d4-e5f6-7890-abcd-ef1234567890

  ┌─────────────────────────────────────────────┐
  │ Data Transformation:                        │
  │   String → java.util.UUID                   │
  │   "a1b2c3d4-..." → UUID(a1b2c3d4-...)      │
  └─────────────────────────────────────────────┘

         ↓ userRepository.findById(uId)

Step 5: UserRepository.findById()
────────────────────────────────────────────────────────
Input:  UUID a1b2c3d4-e5f6-7890-abcd-ef1234567890
Action: Hibernate → SQL SELECT
Output: Optional<User> containing User entity

  ┌─────────────────────────────────────────────┐
  │ Data Transformation:                        │
  │   UUID → SQL parameter → DB row → User      │
  │   entity → Optional<User>                   │
  │                                             │
  │   User {                                    │
  │     id: a1b2c3d4-...,                       │
  │     email: "aditya@example.com",            │
  │     name: "Aditya",                         │
  │     password: "$2a$10$...",                  │
  │     enable: true,                           │
  │     provider: LOCAL,                        │
  │     roles: [ROLE_USER, ROLE_ADMIN]           │
  │   }                                         │
  └─────────────────────────────────────────────┘

         ↓ .orElseThrow() unwraps Optional

Step 6: Optional.orElseThrow()
────────────────────────────────────────────────────────
Input:  Optional<User> (present)
Action: Unwraps
Output: User entity

  ┌─────────────────────────────────────────────┐
  │ Data Transformation:                        │
  │   Optional<User> → User                    │
  └─────────────────────────────────────────────┘

         ↓ userRepository.delete(user)

Step 7: UserRepository.delete()
────────────────────────────────────────────────────────
Input:  User entity (managed in persistence context)
Action: Hibernate → SQL DELETE (join table then users)
Output: (void)

  ┌─────────────────────────────────────────────┐
  │ Database Operations:                        │
  │   1. DELETE FROM user_roles                 │
  │      WHERE user_id = 'a1b2c3d4-...'         │
  │   2. DELETE FROM users                      │
  │      WHERE user_id = 'a1b2c3d4-...'         │
  └─────────────────────────────────────────────┘

         ↓ void returned through all layers

Step 8: Response
────────────────────────────────────────────────────────
Output: HTTP 200 OK (empty body)
```

### Failure Scenario — User Not Found

```
Steps 1-4: Same as above (URL parsing, UUID conversion)

         ↓ userRepository.findById(uId)

Step 5: UserRepository.findById()
────────────────────────────────────────────────────────
Input:  UUID a1b2c3d4-e5f6-7890-abcd-ef1234567890
Action: Hibernate → SQL SELECT
Output: Optional.empty() (no row found)

         ↓ .orElseThrow() triggers exception

Step 6: Optional.orElseThrow()
────────────────────────────────────────────────────────
Input:  Optional.empty()
Action: Executes Supplier lambda → throws exception
Output: THROWS ResourceNotFoundException("User not found with the given id")

         ↓ Exception propagates up call stack

Step 7: GlobalExceptionHandler.handleResourceNotFoundException()
────────────────────────────────────────────────────────
Input:  ResourceNotFoundException (message: "User not found with the given id")
Action: Creates ErrorResponse record, wraps in ResponseEntity
Output: ResponseEntity<ErrorResponse> (404 NOT_FOUND)

  ┌─────────────────────────────────────────────┐
  │ Data Transformation:                        │
  │   Exception → ErrorResponse record →        │
  │   ResponseEntity → JSON serialization       │
  └─────────────────────────────────────────────┘

         ↓ Jackson serializes to JSON

Step 8: Response
────────────────────────────────────────────────────────
Output: HTTP 404 NOT FOUND
{
  "message": "User not found with the given id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

### Failure Scenario — Invalid UUID Format

```
Steps 1-3: Same as above (URL parsing, controller invoked)

         ↓ UserHelper.parseUUID("not-a-valid-uuid")

Step 4: UserHelper.parseUUID()
────────────────────────────────────────────────────────
Input:  String "not-a-valid-uuid"
Action: UUID.fromString("not-a-valid-uuid")
Output: THROWS IllegalArgumentException("Invalid UUID string: not-a-valid-uuid")

         ↓ Exception propagates up (NO handler in GlobalExceptionHandler)

Step 5: Spring Boot Default Error Handler
────────────────────────────────────────────────────────
Output: HTTP 500 Internal Server Error (default Spring Boot error page)

  ┌─────────────────────────────────────────────┐
  │ ⚠️ This is a GAP in error handling!         │
  │ Should return 400 Bad Request instead.      │
  │ Fix: Add @ExceptionHandler for             │
  │ IllegalArgumentException in                 │
  │ GlobalExceptionHandler                      │
  └─────────────────────────────────────────────┘
```

---

## 7. Annotation Explanations in Context

### Controller Layer Annotations

| Annotation | Location | Purpose | What Breaks If Removed |
|---|---|---|---|
| `@RestController` | `UserController` class | Registers class as REST controller; auto-serializes return values to JSON | Class not detected as controller → 404 for all endpoints |
| `@RequestMapping("/api/v1/users")` | `UserController` class | Base URL prefix for all methods | Method-level mappings lose their prefix → wrong URL routing |
| `@AllArgsConstructor` | `UserController` class | Generates constructor for all fields → enables dependency injection | `userService` is `null` → `NullPointerException` |
| `@DeleteMapping("/{userId}")` | `deleteUser()` method | Maps `DELETE` HTTP method + URI pattern to this handler | No handler for DELETE requests → `404` or `405` |
| `@PathVariable("userId")` | `deleteUser()` parameter | Extracts `{userId}` segment from URL into method parameter | `userId` parameter is `null` → `NullPointerException` in `parseUUID(null)` |

### Service Layer Annotations

| Annotation | Location | Purpose | What Breaks If Removed |
|---|---|---|---|
| `@Service` | `UserServiceImpl` class | Registers as Spring bean (detected by component scan) | `UserService` bean missing → application fails to start (injection error) |
| `@RequiredArgsConstructor` | `UserServiceImpl` class | Generates constructor for `final` fields | Fields are `null` → `NullPointerException` |
| `@Override` | `deleteUser()` method | Compile-time check that method matches interface contract | No runtime effect; removes compile-time safety check |

### Entity Layer Annotations

| Annotation | Location | Purpose | What Breaks If Removed |
|---|---|---|---|
| `@Entity` | `User` class | Marks as JPA entity (mapped to DB table) | Hibernate doesn't recognize this class → no table mapping |
| `@Table(name="users")` | `User` class | Specifies table name | Hibernate defaults to class name `User` as table name (may or may not match your schema) |
| `@Id` | `User.id` field | Marks as primary key | JPA error: no identifier specified → application fails to start |
| `@GeneratedValue(strategy = GenerationType.UUID)` | `User.id` field | Auto-generates UUID for new entities | Must manually set ID before save; not relevant to delete flow |
| `@Column(name = "user_id")` | `User.id` field | Maps field to specific column name | Hibernate uses field name `id` → column name mismatch |
| `@ManyToMany(fetch = FetchType.EAGER)` | `User.roles` field | Defines many-to-many relationship; loads roles immediately with user | Roles not loaded with user query (but not needed for delete itself) |
| `@JoinTable(...)` | `User.roles` field | Defines the join table structure and FK columns | Hibernate can't manage the relationship → join table not recognized |
| `@Getter @Setter` | `User` class | Lombok generates getters/setters for all fields | No getters/setters → Hibernate can't access fields (depending on access strategy) |
| `@Builder` | `User` class | Generates builder pattern (not used in delete flow) | No effect on delete flow |
| `@PrePersist` | `User.onCreate()` method | Lifecycle callback before first save | Not triggered during delete |
| `@PreUpdate` | `User.onUpdate()` method | Lifecycle callback before update | Not triggered during delete |

### Exception Handler Annotations

| Annotation | Location | Purpose | What Breaks If Removed |
|---|---|---|---|
| `@RestControllerAdvice` | `GlobalExceptionHandler` class | Global exception handling for all REST controllers | Exceptions handled by Spring Boot defaults (generic 500 errors) |
| `@ExceptionHandler(ResourceNotFoundException.class)` | `handleResourceNotFoundException()` | Catches `ResourceNotFoundException` across all controllers | `ResourceNotFoundException` is unhandled → 500 Internal Server Error |

---

## 8. Summary Section

### 8.1 Execution Flow (Condensed)

```
Client sends DELETE /api/v1/users/{userId}
    → Tomcat receives HTTP request
    → DispatcherServlet routes to UserController.deleteUser()
    → Controller extracts path variable userId (String)
    → Controller calls userService.deleteUser(userId)
    → Service parses userId String to UUID via UserHelper.parseUUID()
    → Service calls userRepository.findById(UUID)
    → Hibernate SELECTs user from database (with eager-loaded roles)
    → Optional unwrapped via orElseThrow (throws 404 if empty)
    → Service calls userRepository.delete(User)
    → Hibernate DELETEs from user_roles join table first
    → Hibernate DELETEs from users table second
    → void returned through service → controller → DispatcherServlet
    → HTTP 200 OK with empty body sent to client
```

### 8.2 Data Flow (Condensed)

```
URL path segment "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    → String (via @PathVariable)
    → UUID (via UserHelper.parseUUID / UUID.fromString)
    → SQL parameter (via Hibernate parameterized query)
    → Database row found → Java User entity (via Hibernate ORM mapping)
    → Optional<User> (via JpaRepository.findById)
    → User entity (via .orElseThrow() unwrap)
    → Passed to delete() → Hibernate issues DELETE SQL
    → void → void → HTTP 200 OK (empty body)
```

### 8.3 Objects Created During Execution

| Object | Type | Created By | Purpose |
|---|---|---|---|
| `HttpServletRequest` | `RequestFacade` | Tomcat | Wraps incoming HTTP request |
| `HttpServletResponse` | `ResponseFacade` | Tomcat | Wraps outgoing HTTP response |
| `UUID uId` | `java.util.UUID` | `UUID.fromString()` | Typed representation of user ID |
| `Optional<User>` | `java.util.Optional` | `findById()` return | Wraps nullable DB result |
| `User user` | `com.substring.auth...User` | Hibernate ORM mapping | Entity loaded from DB |
| `Set<Role>` | `java.util.HashSet` | Hibernate (eager fetch) | User's roles collection |
| (Error path) `ResourceNotFoundException` | Custom exception | Lambda in `orElseThrow` | Signals user not found |
| (Error path) `ErrorResponse` | Java record | `GlobalExceptionHandler` | Error response DTO |
| (Error path) `ResponseEntity<ErrorResponse>` | Spring | `GlobalExceptionHandler` | HTTP response wrapper |

### 8.4 Methods Called (In Order)

| # | Method | Class | Input | Output |
|---|---|---|---|---|
| 1 | `doDispatch()` | `DispatcherServlet` | `HttpServletRequest` | Routes to handler |
| 2 | `deleteUser()` | `UserController` | `String userId` | `void` |
| 3 | `deleteUser()` | `UserServiceImpl` | `String userId` | `void` |
| 4 | `parseUUID()` | `UserHelper` (static) | `String uuid` | `UUID` |
| 5 | `fromString()` | `java.util.UUID` (static) | `String name` | `UUID` |
| 6 | `findById()` | `UserRepository` (proxy) | `UUID id` | `Optional<User>` |
| 7 | `orElseThrow()` | `Optional<User>` | `Supplier<Exception>` | `User` or throws |
| 8 | `delete()` | `UserRepository` (proxy) | `User entity` | `void` |
| 9 | `contains()` | `EntityManager` | `User entity` | `boolean` |
| 10 | `remove()` | `EntityManager` | `User entity` | `void` |

### 8.5 Database Operations

| # | Operation | SQL | Table | Purpose |
|---|---|---|---|---|
| 1 | SELECT | `SELECT * FROM users WHERE user_id = ?` | `users` | Find user by primary key |
| 2 | SELECT (eager) | `SELECT r.* FROM user_roles ur JOIN roles r ON ...` | `user_roles` + `roles` | Eagerly load user's roles |
| 3 | DELETE | `DELETE FROM user_roles WHERE user_id = ?` | `user_roles` | Remove join table entries (must happen before user delete due to FK) |
| 4 | DELETE | `DELETE FROM users WHERE user_id = ?` | `users` | Remove the user row |

> [!IMPORTANT]
> **Total DB operations: 2 SELECTs + 2 DELETEs = 4 SQL statements** for a successful delete of a user with roles. The eager fetch of roles adds an extra SELECT even though the roles themselves are not needed for deletion — this is the cost of `FetchType.EAGER`.

### 8.6 Final Response

**Success (User Found and Deleted):**

```
HTTP/1.1 200 OK
Content-Length: 0
```

**Failure (User Not Found):**

```json
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "message": "User not found with the given id",
  "status": "NOT_FOUND",
  "statusCode": 404
}
```

**Failure (Invalid UUID):**

```
HTTP/1.1 500 Internal Server Error
(Spring Boot default error response — no custom handler)
```

### 8.7 Key Interview Discussion Points

1. **Why does the delete return 200 instead of 204?**
   The controller method returns `void` without `@ResponseStatus`. Spring defaults `void` methods to `200 OK`. To return `204`, use `ResponseEntity.noContent().build()` or `@ResponseStatus(HttpStatus.NO_CONTENT)`.

2. **Why does `findById` happen before `delete`?**
   To validate the user exists and throw a meaningful `ResourceNotFoundException` (404). Using `deleteById()` directly would either silently do nothing (Spring Data 3.x+) or throw a generic Spring exception (older versions).

3. **What happens to the `roles` table when a user is deleted?**
   Nothing. Only the `user_roles` join table rows are removed. The `Role` entities persist because there's no `CascadeType.REMOVE`. This is correct — roles are shared across users.

4. **Why is `@Transactional` missing on the service method?**
   Each repository call (`findById`, `delete`) runs in its own transaction. This works but isn't ideal. If the service had multiple write operations, they should be wrapped in a single transaction for atomicity. Adding `@Transactional` to `deleteUser()` would ensure `findById` and `delete` share the same transaction and persistence context.

5. **What if two requests try to delete the same user simultaneously?**
   Without `@Transactional` on the service: both calls do `findById` (both succeed), then both call `delete`. The second `delete` would silently succeed since `SimpleJpaRepository.delete()` checks `entityManager.find()` internally and returns early if the entity no longer exists. With `@Transactional` + optimistic locking (`@Version`), the second request would get a `StaleStateException`.

6. **What about the invalid UUID handling gap?**
   `UUID.fromString()` throws `IllegalArgumentException` which has no handler in `GlobalExceptionHandler`. The application returns a 500 error instead of a proper 400 Bad Request. This should be fixed by adding an `@ExceptionHandler(IllegalArgumentException.class)` that returns a 400 response.

---

*Documentation generated for the DELETE USER API — `DELETE /api/v1/users/{userId}`*
*Project: Auth Application Using Java Full Stack*
