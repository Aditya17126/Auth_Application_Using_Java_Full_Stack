# 📘 GET ALL USERS API — Complete Execution Trace

## Table of Contents
1. [API Endpoint Details](#1-api-endpoint-details)
2. [Real-World Example Scenario](#2-real-world-example-scenario)
3. [Security Filter Chain Execution](#3-security-filter-chain-execution)
4. [Complete Step-by-Step Execution Flow](#4-complete-step-by-step-execution-flow)
5. [Line-by-Line Code Explanation](#5-line-by-line-code-explanation)
6. [Data Flow Tracking](#6-data-flow-tracking)
7. [Annotation Explanations in Context](#7-annotation-explanations-in-context)
8. [Summary](#8-summary)

---

## 1. API Endpoint Details

| Property          | Value                                        |
|-------------------|----------------------------------------------|
| **HTTP Method**   | `GET`                                        |
| **URL**           | `/api/v1/users`                              |
| **Request Body**  | _None_                                       |
| **Path Variables**| _None_                                       |
| **Query Params**  | _None_                                       |
| **Authentication**| _None (Spring Security is commented out)_    |
| **Success Status**| `200 OK`                                     |
| **Response Type** | `Iterable<UserDto>` (serialised as JSON array)|

### Request

```
GET /api/v1/users HTTP/1.1
Host: localhost:8083
```

No headers, no body, no path variables — this is the simplest possible HTTP request the application handles.

### Successful Response (200 OK)

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "aditya@example.com",
    "name": "Aditya",
    "password": "hashed_password_1",
    "image": "aditya.png",
    "enable": true,
    "createdAt": "2026-06-25T10:00:00Z",
    "updatedAt": "2026-06-25T10:00:00Z",
    "provider": "LOCAL",
    "roles": [
      { "id": "r1r1r1r1-aaaa-bbbb-cccc-dddddddddddd", "name": "ROLE_USER" }
    ]
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "email": "priya@example.com",
    "name": "Priya",
    "password": "hashed_password_2",
    "image": "priya.png",
    "enable": true,
    "createdAt": "2026-06-26T14:30:00Z",
    "updatedAt": "2026-06-26T14:30:00Z",
    "provider": "GOOGLE",
    "roles": [
      { "id": "r1r1r1r1-aaaa-bbbb-cccc-dddddddddddd", "name": "ROLE_USER" },
      { "id": "r2r2r2r2-eeee-ffff-0000-111111111111", "name": "ROLE_ADMIN" }
    ]
  }
]
```

---

## 2. Real-World Example Scenario

> **Scenario:** An admin dashboard frontend calls `GET /api/v1/users` to populate a table showing every registered user. The database currently has two users — **Aditya** (LOCAL provider, ROLE_USER) and **Priya** (GOOGLE provider, ROLE_USER + ROLE_ADMIN). The API must fetch both rows, convert them to DTOs, and return a JSON array.

### Database State Before the Request

**`users` table**

| user_id | user_email | name | password | image | enable | createdAt | updatedAt | provider |
|---------|------------|------|----------|-------|--------|-----------|-----------|----------|
| a1b2c3d4-e5f6-7890-abcd-ef1234567890 | aditya@example.com | Aditya | hashed_password_1 | aditya.png | true | 2026-06-25T10:00:00Z | 2026-06-25T10:00:00Z | LOCAL |
| b2c3d4e5-f6a7-8901-bcde-f12345678901 | priya@example.com | Priya | hashed_password_2 | priya.png | true | 2026-06-26T14:30:00Z | 2026-06-26T14:30:00Z | GOOGLE |

**`user_roles` join table**

| user_id | role_id |
|---------|---------|
| a1b2c3d4-... | r1r1r1r1-... |
| b2c3d4e5-... | r1r1r1r1-... |
| b2c3d4e5-... | r2r2r2r2-... |

**`roles` table**

| id | name |
|----|------|
| r1r1r1r1-aaaa-bbbb-cccc-dddddddddddd | ROLE_USER |
| r2r2r2r2-eeee-ffff-0000-111111111111 | ROLE_ADMIN |

---

## 3. Security Filter Chain Execution

> [!IMPORTANT]
> Spring Security is **completely commented out** in this project. There is no `SecurityFilterChain` bean, no `JwtAuthFilter`, and no `OncePerRequestFilter` configured. Every endpoint — including this one — is accessible without authentication.

### What Happens Without Security

```
Client sends GET /api/v1/users
        ↓
Tomcat receives the request on port 8083
        ↓
Spring's DispatcherServlet receives the request
        ↓
NO SecurityFilterChain intercepts the request
        ↓
DispatcherServlet maps the request directly to the controller method
```

Because there is no security configuration:

1. **No `JwtAuthFilter`** — No JWT token is extracted or validated.
2. **No `AuthenticationManager`** — No credentials are checked.
3. **No `SecurityContext`** — `SecurityContextHolder.getContext().getAuthentication()` would return `null`.
4. **No role-based access** — Anyone, including unauthenticated clients, can hit this endpoint.
5. **No CORS filter** — Unless configured elsewhere, default CORS rules apply.

> [!WARNING]
> In production, this endpoint should be secured (e.g., restricted to `ROLE_ADMIN`) to prevent unauthorized enumeration of all users.

---

## 4. Complete Step-by-Step Execution Flow

### High-Level Architecture Flow

```
HTTP GET /api/v1/users
        ↓
┌─────────────────────────────────────┐
│         TOMCAT (port 8083)          │
│  Receives raw HTTP request          │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│        DispatcherServlet            │
│  Routes to correct controller       │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│   UserController.getAllUsers()       │
│   @GetMapping on /api/v1/users      │
│   Returns ResponseEntity<Iterable>  │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│   UserServiceImpl.getAllUsers()      │
│   @Transactional                    │
│   Fetches all users, maps to DTOs   │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│   UserRepository.findAll()          │
│   JpaRepository inherited method    │
│   SQL: SELECT * FROM users          │
│   + eager-fetches roles via JOIN    │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│   ModelMapper.map(user, UserDto)    │
│   Called per User entity            │
│   Maps each User → UserDto         │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│   Jackson (HttpMessageConverter)    │
│   Serialises List<UserDto> → JSON   │
└──────────────┬──────────────────────┘
               ↓
        HTTP 200 OK + JSON Array
```

### Detailed Request Lifecycle

#### Step 1: Tomcat Receives the Request

The embedded Tomcat server (configured on port **8083** via `application-dev.yml`) listens for incoming connections. A raw HTTP request arrives:

```
GET /api/v1/users HTTP/1.1
Host: localhost:8083
Accept: application/json
```

Tomcat parses this into an `HttpServletRequest` object and hands it to Spring's `DispatcherServlet`.

#### Step 2: DispatcherServlet Routes the Request

Spring's `DispatcherServlet` (the front-controller) performs the following:

1. **HandlerMapping lookup** — Scans all `@RequestMapping`-annotated classes. Finds `UserController` mapped to `/api/v1/users`.
2. **Method matching** — Within `UserController`, looks for a method annotated with `@GetMapping` and no additional path segments. Finds `getAllUsers()`.
3. **HandlerAdapter selection** — Selects `RequestMappingHandlerAdapter` to invoke the method.
4. **Argument resolution** — The method has no parameters, so no argument resolvers are needed.

#### Step 3: Controller Method Executes

```java
@GetMapping
public ResponseEntity<Iterable<UserDto>> getAllUsers(){
   return ResponseEntity.ok(userService.getAllUsers());
}
```

Spring calls `getAllUsers()` on the `UserController` instance. The controller delegates immediately to `userService.getAllUsers()`.

#### Step 4: Service Method Executes

```java
@Override
@Transactional()
public Iterable<UserDto> getAllUsers() {
    return userRepository
    .findAll()
    .stream()
    .map(user  -> modelMapper.map(user, UserDto.class))
    .toList();
}
```

1. Spring's `@Transactional` proxy opens a database transaction.
2. `userRepository.findAll()` executes SQL to fetch all users.
3. The resulting `List<User>` is streamed.
4. Each `User` entity is mapped to a `UserDto` via `ModelMapper`.
5. `.toList()` collects the stream into a `List<UserDto>`.
6. The transaction commits (read-only, no changes to persist).

#### Step 5: Repository Executes SQL

`findAll()` is inherited from `JpaRepository<User, UUID>`. Hibernate generates:

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
```

Because the `roles` field on `User` is `@ManyToMany(fetch = FetchType.EAGER)`, Hibernate also executes a second query (or a JOIN, depending on the fetch strategy):

```sql
SELECT
    r.id,
    r.name
FROM user_roles ur
JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id IN (?, ?)
```

The `?` placeholders are filled with the UUIDs of Aditya and Priya.

#### Step 6: ModelMapper Converts Entities to DTOs

For **each** `User` entity in the list, `modelMapper.map(user, UserDto.class)` is called. ModelMapper uses reflection to match field names:

| User Entity Field | UserDto Field | Value (Aditya) |
|---|---|---|
| `id` (UUID) | `id` (UUID) | `a1b2c3d4-...` |
| `email` (String) | `email` (String) | `aditya@example.com` |
| `name` (String) | `name` (String) | `Aditya` |
| `password` (String) | `password` (String) | `hashed_password_1` |
| `image` (String) | `image` (String) | `aditya.png` |
| `enable` (boolean) | `enable` (boolean) | `true` |
| `createdAt` (Instant) | `createdAt` (Instant) | `2026-06-25T10:00:00Z` |
| `updatedAt` (Instant) | `updatedAt` (Instant) | `2026-06-25T10:00:00Z` |
| `provider` (Provider) | `provider` (Provider) | `LOCAL` |
| `roles` (Set\<Role\>) | `roles` (Set\<Role\>) | `[{ROLE_USER}]` |

The same mapping runs for Priya.

#### Step 7: Response Serialisation

Jackson's `MappingJackson2HttpMessageConverter` serialises the `List<UserDto>` into a JSON array. The `ResponseEntity.ok(...)` wraps it with HTTP status `200 OK`.

#### Step 8: Response Sent to Client

Tomcat writes the HTTP response back over the socket:

```
HTTP/1.1 200 OK
Content-Type: application/json

[
  { "id": "a1b2c3d4-...", "email": "aditya@example.com", ... },
  { "id": "b2c3d4e5-...", "email": "priya@example.com", ... }
]
```

---

## 5. Line-by-Line Code Explanation

### 5.1 UserController.java — Full Class

```java
package com.substring.auth.auth_app.controllers;
```
- **Why**: Declares the Java package. All classes in `controllers` package are Spring MVC controllers.
- **What breaks if removed**: Compilation error — every Java class must declare its package (or be in the default package, which is bad practice and breaks component scanning).

```java
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
```
- **Why**: Imports the annotations that make this class a REST controller with a base URL path.
- **What breaks if removed**: Compilation error — `@RestController` and `@RequestMapping` would be unresolvable.

```java
import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.services.UserService;
```
- **Why**: Imports the DTO class used in the response and the service interface for business logic.
- **What breaks if removed**: Compilation error — `UserDto` and `UserService` types would be unresolvable.

```java
import lombok.AllArgsConstructor;
```
- **Why**: Imports Lombok's `@AllArgsConstructor` which generates a constructor with all fields, enabling **constructor-based dependency injection**.
- **What breaks if removed**: The `@AllArgsConstructor` annotation below would not compile. Spring would not be able to inject `UserService` into the controller.

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
```
- **Why**: Imports `HttpStatus` (used by other methods like `createUser`) and `ResponseEntity` (the wrapper that lets us control HTTP status codes and response bodies).
- **What breaks if removed**: Compilation error on all methods returning `ResponseEntity`.

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
```
- **Why**: Imports HTTP method mapping annotations and parameter-binding annotations used across various methods.
- **What breaks if removed**: Whichever annotation is missing would cause a compilation error on the method that uses it. For our `getAllUsers()` flow, removing `GetMapping` would be fatal.

```java
@RestController
```
- **Who calls it**: Spring's component scanner during application startup.
- **What it does**: Combines `@Controller` + `@ResponseBody`. Tells Spring: (1) this class is a Spring bean, (2) every method's return value should be serialised directly to the HTTP response body (via Jackson), not resolved as a view name.
- **What breaks if removed**: Spring would not register this class as a controller. `GET /api/v1/users` would return **404 Not Found**.

```java
@RequestMapping("/api/v1/users")
```
- **Who calls it**: Spring's `RequestMappingHandlerMapping` during startup.
- **What it does**: Sets the **base URL path** for every method in this controller. All `@GetMapping`, `@PostMapping`, etc. paths are appended to `/api/v1/users`.
- **What breaks if removed**: Every method's URL would need to declare the full path individually, or they'd be mapped to `/` which would cause conflicts and unexpected routing.

```java
@AllArgsConstructor
```
- **Who calls it**: Lombok's annotation processor at compile time.
- **What it does**: Generates the following constructor:
  ```java
  public UserController(UserService userService) {
      this.userService = userService;
  }
  ```
  Spring sees this single constructor and automatically injects the `UserService` bean (which is `UserServiceImpl`).
- **What breaks if removed**: No constructor → Spring cannot inject `UserService` → `BeanCreationException` at startup. The application would **fail to start**.

```java
public class UserController {
```
- **Why**: Declares the controller class. The class name is arbitrary; Spring relies on annotations, not naming conventions.

```java
private final UserService userService;
```
- **Who sets it**: Spring's DI container, via the Lombok-generated constructor.
- **What it holds**: A reference to `UserServiceImpl` (the only bean implementing `UserService`).
- **Why `final`**: Ensures the field is set exactly once (in the constructor) and cannot be reassigned. This is a best practice for dependency injection — it makes the dependency immutable and thread-safe.
- **What breaks if removed**: The constructor (generated by `@AllArgsConstructor`) would have no parameters. Spring would create a `UserController` with no service. Every method calling `userService.xxx()` would throw `NullPointerException`.

---

#### The `getAllUsers()` Method — The Focus of This Document

```java
@GetMapping
public ResponseEntity<Iterable<UserDto>> getAllUsers(){
   return ResponseEntity.ok(userService.getAllUsers());
}
```

Let's break this down token by token:

##### `@GetMapping`

- **Who processes it**: `RequestMappingHandlerMapping` during application startup.
- **What it does**: Maps HTTP `GET` requests to this method. Since no path is specified inside `@GetMapping`, it inherits the class-level path `/api/v1/users` as-is.
- **Equivalent to**: `@RequestMapping(method = RequestMethod.GET)`.
- **How Spring resolves it**: When a `GET /api/v1/users` request arrives, Spring checks:
  1. Class-level `@RequestMapping("/api/v1/users")` → matches the URL prefix ✓
  2. Method-level `@GetMapping` (no additional path) → matches the exact URL ✓
  3. HTTP method is `GET` → matches `@GetMapping` ✓
  4. **Result**: Route to `getAllUsers()`.
- **What breaks if removed**: `GET /api/v1/users` returns **405 Method Not Allowed** (if other methods on the same URL exist) or **404 Not Found**. The endpoint simply does not exist.

##### `public`

- **Why**: Spring MVC uses reflection to invoke controller methods; while `private` could work with `setAccessible(true)`, the convention is `public` for clarity and to satisfy the method handler's expectations.
- **What breaks if removed (set to `private`)**: Spring may fail to invoke the method depending on the JVM's reflection access policy, especially with module security in Java 17+.

##### `ResponseEntity<Iterable<UserDto>>`

- **What it is**: The return type. `ResponseEntity` is Spring's wrapper that gives the developer full control over:
  - HTTP status code (here: `200 OK`)
  - HTTP headers (here: defaults)
  - Response body (here: `Iterable<UserDto>`)
- **Why `Iterable<UserDto>`**: This is the declared generic type. In practice, `userService.getAllUsers()` returns a `List<UserDto>` (which implements `Iterable`). Using `Iterable` in the signature provides abstraction.
- **What breaks if removed (changed to `void`)**: No response body is sent. The client receives a `200 OK` with an empty body.

##### `getAllUsers()`

- **What it is**: The method name. It is arbitrary — Spring does not use method names for routing; it uses annotations.
- **Parameters**: None. This is a GET-all endpoint; no request body, no path variables, no query parameters.

##### `return ResponseEntity.ok(userService.getAllUsers());`

This single line does **three things** — let's trace each one:

**Step A — `userService.getAllUsers()` is called**

- **`userService`**: This is the `UserService` interface field. At runtime, it holds a reference to a **Spring CGLIB proxy** wrapping `UserServiceImpl` (because `@Transactional` is on the method, so Spring creates a transactional proxy).
- **What happens**: The proxy intercepts the call → opens a transaction → calls the real `UserServiceImpl.getAllUsers()` → the method runs → proxy commits the transaction → returns the `List<UserDto>`.
- **Return value**: `List<UserDto>` containing two UserDto objects (Aditya and Priya).

**Step B — `ResponseEntity.ok(...)` wraps the result**

- **What it does**: Static factory method that creates a `ResponseEntity` with:
  - Status: `200 OK`
  - Body: the `List<UserDto>` passed as argument
  - Headers: default (Content-Type will be set to `application/json` by Jackson)
- **Equivalent to**: `new ResponseEntity<>(body, HttpStatus.OK)`

**Step C — `return` sends the ResponseEntity back to Spring**

- Spring's `RequestMappingHandlerAdapter` receives the `ResponseEntity`.
- It passes the body (`List<UserDto>`) to `MappingJackson2HttpMessageConverter`.
- Jackson serialises the list to a JSON array.
- The HTTP response is written to the `HttpServletResponse`.

---

### 5.2 UserService.java — Interface

```java
package com.substring.auth.auth_app.services;
```
- **Why**: Package declaration for the service layer.

```java
import com.substring.auth.auth_app.dtos.UserDto;
```
- **Why**: The interface methods use `UserDto` as parameter/return types.

```java
public interface UserService {
```
- **Why**: Defines the contract for user-related business operations. Using an interface allows:
  1. **Loose coupling** — The controller depends on the interface, not the implementation.
  2. **Proxy creation** — Spring creates a CGLIB/JDK proxy for `@Transactional` support.
  3. **Testability** — Easy to mock in unit tests.
- **What breaks if removed**: `UserController`'s `private final UserService userService` field would not compile. The entire application architecture collapses.

```java
Iterable<UserDto> getAllUsers();
```
- **Why**: Declares the method signature for fetching all users.
- **Return type `Iterable<UserDto>`**: Provides abstraction — the implementation can return a `List`, `Set`, or any `Iterable`.
- **What breaks if removed**: `UserController.getAllUsers()` calls `userService.getAllUsers()`, which would cause a compilation error.

---

### 5.3 UserServiceImpl.java — The Core Logic

#### Class-Level Annotations and Fields

```java
package com.substring.auth.auth_app.services;
```

```java
import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.entities.Provider;
import com.substring.auth.auth_app.entities.User;
import com.substring.auth.auth_app.exceptions.ResourceNotFoundException;
import com.substring.auth.auth_app.helpers.UserHelper;
import com.substring.auth.auth_app.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.UUID;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
```
- **Why**: Imports all dependencies. For `getAllUsers()`, the critical imports are: `UserDto`, `User`, `UserRepository`, `Transactional`, `ModelMapper`, and `Service`.

```java
@Service
```
- **Who processes it**: Spring's component scanner during startup.
- **What it does**: Marks this class as a **Spring-managed service bean**. It's a specialisation of `@Component` that communicates intent: "this is a business logic class."
- **What Spring does with it**:
  1. Creates an instance of `UserServiceImpl`.
  2. Injects `UserRepository` and `ModelMapper` via the constructor.
  3. Wraps it in a **CGLIB proxy** (because `@Transactional` is present on methods).
  4. Registers the proxy in the application context as a bean of type `UserService`.
- **What breaks if removed**: Spring never creates this bean. `UserController` tries to inject `UserService` but no bean is found → `NoSuchBeanDefinitionException` → **application fails to start**.

```java
@RequiredArgsConstructor
```
- **Who processes it**: Lombok's annotation processor at compile time.
- **What it does**: Generates a constructor for all `final` fields:
  ```java
  public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
      this.userRepository = userRepository;
      this.modelMapper = modelMapper;
  }
  ```
- **Why `@RequiredArgsConstructor` instead of `@AllArgsConstructor`**: `@RequiredArgsConstructor` only includes `final` and `@NonNull` fields. If a non-final field were added later, it would not be included in the constructor, preventing accidental injection issues.
- **What breaks if removed**: No constructor → Spring cannot inject dependencies → `BeanCreationException` at startup.

```java
public class UserServiceImpl implements UserService {
```
- **Why `implements UserService`**: This class provides the concrete implementation of the `UserService` interface. Spring's DI resolves `UserService` to this class because it's the only bean implementing the interface.
- **What breaks if removed (`implements UserService`)**: Spring wouldn't recognise this as a `UserService` bean. `UserController` would get `NoSuchBeanDefinitionException`.

```java
private final UserRepository userRepository;
```
- **What it holds**: A Spring Data JPA proxy implementing `JpaRepository<User, UUID>`. This proxy provides CRUD operations including `findAll()`.
- **Who sets it**: Spring, via the Lombok-generated constructor.
- **Why `final`**: Immutability and thread safety.
- **What breaks if removed**: `userRepository.findAll()` in `getAllUsers()` would cause a compilation error.

```java
private final ModelMapper modelMapper;
```
- **What it holds**: An instance of `ModelMapper`, created by the `@Bean` method in `ProjectConfig.java`.
- **Who sets it**: Spring, via the Lombok-generated constructor.
- **What it does**: Provides object-to-object mapping using reflection. Maps `User` entity → `UserDto`.
- **What breaks if removed**: `modelMapper.map(user, UserDto.class)` in `getAllUsers()` would cause a compilation error.

---

#### The `getAllUsers()` Method — Line by Line

```java
@Override
```
- **What it does**: Tells the compiler: "this method implements a method declared in a supertype (`UserService` interface)."
- **What breaks if removed**: No runtime effect, but you lose compile-time verification. If `UserService` changed the method signature, the compiler would no longer warn you that this method doesn't match.

```java
@Transactional()
```
- **Who processes it**: Spring's `TransactionInterceptor` (part of the CGLIB proxy wrapping `UserServiceImpl`).
- **What it does — step by step**:
  1. **Before** `getAllUsers()` executes: Spring opens a database transaction by obtaining a JDBC connection from the `DataSource` and calling `connection.setAutoCommit(false)`.
  2. The method body runs (all DB queries happen within this transaction).
  3. **After** `getAllUsers()` returns successfully: Spring calls `connection.commit()`.
  4. **If an exception occurs**: Spring calls `connection.rollback()` (for unchecked exceptions by default).
- **Why it matters for a read operation**: While `@Transactional` is often associated with writes, it serves important purposes for reads:
  1. **Consistent snapshot**: All queries within the method see the same database state (depends on isolation level).
  2. **Lazy loading safety**: If any associations were `FetchType.LAZY`, they could be loaded within the transaction. (In this case, roles are `EAGER`, so this is not strictly necessary.)
  3. **Connection management**: Ensures a single connection is used for all queries in the method.
- **What breaks if removed**: For this specific method with `EAGER` fetch, it would still work. However, if roles were changed to `LAZY`, a `LazyInitializationException` would occur when accessing `user.getRoles()` outside a transaction.

```java
public Iterable<UserDto> getAllUsers() {
```
- **Return type**: `Iterable<UserDto>` — matches the interface declaration.
- **Actual return**: `List<UserDto>` (from `.toList()`), which is a subtype of `Iterable<UserDto>`.

```java
return userRepository
```
- **What `userRepository` is**: A Spring Data JPA proxy implementing `UserRepository extends JpaRepository<User, UUID>`.
- **What happens**: The method chain starts. `userRepository` is the entry point.

```java
.findAll()
```
- **Who provides this method**: `JpaRepository<User, UUID>` — inherited through Spring Data JPA's repository hierarchy: `UserRepository → JpaRepository → PagingAndSortingRepository → CrudRepository`.
- **What it does internally**:
  1. Spring Data's `SimpleJpaRepository.findAll()` is called.
  2. It calls `entityManager.createQuery("SELECT u FROM User u", User.class).getResultList()`.
  3. Hibernate translates this JPQL to native SQL:
     ```sql
     SELECT u.user_id, u.user_email, u.name, u.password, u.image,
            u.enable, u.created_at, u.updated_at, u.provider
     FROM users u
     ```
  4. Because `roles` has `FetchType.EAGER`, Hibernate also fetches roles. Depending on the Hibernate version and batching configuration, it might issue:
     ```sql
     SELECT ur.user_id, r.id, r.name
     FROM user_roles ur
     JOIN roles r ON ur.role_id = r.id
     WHERE ur.user_id IN (
         'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
         'b2c3d4e5-f6a7-8901-bcde-f12345678901'
     )
     ```
  5. Hibernate creates `User` entity objects from the result set.
- **Return value**: `List<User>` containing two fully-hydrated `User` entities (with their `Set<Role>` populated).
- **What the returned objects look like**:

  ```
  User[0]: {
      id = a1b2c3d4-e5f6-7890-abcd-ef1234567890,
      email = "aditya@example.com",
      name = "Aditya",
      password = "hashed_password_1",
      image = "aditya.png",
      enable = true,
      createdAt = 2026-06-25T10:00:00Z,
      updatedAt = 2026-06-25T10:00:00Z,
      provider = Provider.LOCAL,
      roles = Set[Role{id=r1r1..., name="ROLE_USER"}]
  }

  User[1]: {
      id = b2c3d4e5-f6a7-8901-bcde-f12345678901,
      email = "priya@example.com",
      name = "Priya",
      password = "hashed_password_2",
      image = "priya.png",
      enable = true,
      createdAt = 2026-06-26T14:30:00Z,
      updatedAt = 2026-06-26T14:30:00Z,
      provider = Provider.GOOGLE,
      roles = Set[Role{id=r1r1..., name="ROLE_USER"}, Role{id=r2r2..., name="ROLE_ADMIN"}]
  }
  ```

- **What breaks if removed**: The entire method chain breaks — there's no data source. Without `findAll()`, you cannot get users from the database.

```java
.stream()
```
- **Who provides this**: `List.stream()` — a method from Java's `Collection` interface.
- **What it does**: Converts the `List<User>` into a `Stream<User>`, enabling functional-style operations (map, filter, collect, etc.).
- **What it returns**: `Stream<User>` containing two `User` elements.
- **What breaks if removed**: `.map()` cannot be called on a `List` directly (in this chain). You'd need to use a for-loop instead, or call `list.stream()` explicitly.

```java
.map(user -> modelMapper.map(user, UserDto.class))
```
- **What it does**: Transforms each `User` entity in the stream into a `UserDto`.
- **`user`**: The lambda parameter — represents one `User` entity at a time.
- **`modelMapper.map(user, UserDto.class)`**: Calls ModelMapper's `map` method, which:
  1. Creates a new `UserDto` instance using the no-args constructor (generated by Lombok's `@NoArgsConstructor`).
  2. Uses reflection to find matching fields by name between `User` and `UserDto`.
  3. Copies each field value from the `User` entity to the `UserDto`.
  4. Returns the populated `UserDto`.

**Detailed mapping for Aditya (first iteration)**:

| Source (`User` entity) | Target (`UserDto`) | Value Copied |
|---|---|---|
| `user.getId()` → UUID | `userDto.setId(UUID)` | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| `user.getEmail()` → String | `userDto.setEmail(String)` | `"aditya@example.com"` |
| `user.getName()` → String | `userDto.setName(String)` | `"Aditya"` |
| `user.getPassword()` → String | `userDto.setPassword(String)` | `"hashed_password_1"` |
| `user.getImage()` → String | `userDto.setImage(String)` | `"aditya.png"` |
| `user.isEnable()` → boolean | `userDto.setEnable(boolean)` | `true` |
| `user.getCreatedAt()` → Instant | `userDto.setCreatedAt(Instant)` | `2026-06-25T10:00:00Z` |
| `user.getUpdatedAt()` → Instant | `userDto.setUpdatedAt(Instant)` | `2026-06-25T10:00:00Z` |
| `user.getProvider()` → Provider | `userDto.setProvider(Provider)` | `Provider.LOCAL` |
| `user.getRoles()` → Set\<Role\> | `userDto.setRoles(Set<Role>)` | `Set[Role{ROLE_USER}]` |

**Detailed mapping for Priya (second iteration)**:

| Source (`User` entity) | Target (`UserDto`) | Value Copied |
|---|---|---|
| `user.getId()` → UUID | `userDto.setId(UUID)` | `b2c3d4e5-f6a7-8901-bcde-f12345678901` |
| `user.getEmail()` → String | `userDto.setEmail(String)` | `"priya@example.com"` |
| `user.getName()` → String | `userDto.setName(String)` | `"Priya"` |
| `user.getPassword()` → String | `userDto.setPassword(String)` | `"hashed_password_2"` |
| `user.getImage()` → String | `userDto.setImage(String)` | `"priya.png"` |
| `user.isEnable()` → boolean | `userDto.setEnable(boolean)` | `true` |
| `user.getCreatedAt()` → Instant | `userDto.setCreatedAt(Instant)` | `2026-06-26T14:30:00Z` |
| `user.getUpdatedAt()` → Instant | `userDto.setUpdatedAt(Instant)` | `2026-06-26T14:30:00Z` |
| `user.getProvider()` → Provider | `userDto.setProvider(Provider)` | `Provider.GOOGLE` |
| `user.getRoles()` → Set\<Role\> | `userDto.setRoles(Set<Role>)` | `Set[Role{ROLE_USER}, Role{ROLE_ADMIN}]` |

- **What breaks if removed**: Raw `User` entities would be returned. This leaks JPA-managed objects to the controller/client layer, causing:
  1. Potential `LazyInitializationException` if any lazy fields are accessed outside the transaction.
  2. Tight coupling between API response and database schema.
  3. Risk of infinite recursion in Jackson serialisation if bidirectional relationships exist.

```java
.toList();
```
- **Who provides this**: `Stream.toList()` — a Java 16+ method on `Stream`.
- **What it does**: Collects all elements from the `Stream<UserDto>` into an **unmodifiable** `List<UserDto>`.
- **What it returns**: `List<UserDto>` with two elements — the Aditya DTO and the Priya DTO.
- **Alternative**: `.collect(Collectors.toList())` — returns a mutable list. `.toList()` is more concise and returns an immutable list.
- **What breaks if removed**: The stream is never terminated. The method would return a `Stream<UserDto>` instead of a `List<UserDto>`, causing a type mismatch with the declared `Iterable<UserDto>` return type. (Actually, `Stream` does not implement `Iterable`, so this would be a compile error.)

---

### 5.4 UserRepository.java

```java
package com.substring.auth.auth_app.repositories;
```

```java
import com.substring.auth.auth_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
```

```java
public interface UserRepository extends JpaRepository<User, UUID> {
```
- **What it does**: Declares a Spring Data JPA repository for the `User` entity with `UUID` as the primary key type.
- **What Spring does at startup**:
  1. Detects this interface extends `JpaRepository`.
  2. Creates a **proxy class** (using `SimpleJpaRepository` as the base implementation) at runtime.
  3. Registers the proxy as a Spring bean.
  4. The proxy provides implementations for all inherited methods: `findAll()`, `findById()`, `save()`, `delete()`, `count()`, `existsById()`, etc.
- **`JpaRepository<User, UUID>`**:
  - `User` — the entity type this repository manages.
  - `UUID` — the type of the entity's `@Id` field.

```java
Optional<User> findByEmail(String email);
```
- **Not used in `getAllUsers()` flow**, but declared as a Spring Data query method.

```java
boolean existsByEmail(String email);
```
- **Not used in `getAllUsers()` flow**, but declared as a Spring Data query method.

**For the `getAllUsers()` flow, the critical inherited method is:**

```java
// Inherited from JpaRepository → ListCrudRepository → CrudRepository
List<User> findAll();
```

This method is **not explicitly declared** in `UserRepository` — it's inherited. Spring Data JPA's `SimpleJpaRepository` provides the implementation:

```java
// Inside SimpleJpaRepository (Spring Data JPA source)
@Override
public List<T> findAll() {
    return getQuery(null, Sort.unsorted()).getResultList();
}
```

---

### 5.5 User.java — Entity

```java
@Getter
@Setter
```
- **Who processes**: Lombok at compile time.
- **What they do**: Generate getter and setter methods for every field. `findAll()` → Hibernate uses these to populate entity fields. `ModelMapper` uses these to read values.
- **What breaks if removed**: Hibernate could still use field-level access (depending on `@Access` settings), but ModelMapper relies on getters/setters. Without them, `modelMapper.map()` would produce a `UserDto` with all fields `null`.

```java
@AllArgsConstructor
@NoArgsConstructor
```
- **What they do**: Generate an all-args constructor and a no-args constructor.
- **Why `@NoArgsConstructor`**: JPA specification **requires** a no-arg constructor on entities. Hibernate uses it to instantiate `User` objects via reflection when loading from the database.
- **Why `@AllArgsConstructor`**: Convenience for creating `User` objects programmatically.
- **What breaks if `@NoArgsConstructor` removed**: Hibernate cannot instantiate `User` → `InstantiationException` at runtime when `findAll()` tries to create entity objects.

```java
@Builder
```
- **What it does**: Generates a builder pattern class (`User.builder().name("Aditya").email("...").build()`).
- **Not directly used in `getAllUsers()` flow**, but available for constructing `User` objects elsewhere.

```java
@Entity
```
- **Who processes**: Hibernate (JPA provider) during startup.
- **What it does**: Marks this class as a JPA entity — a class that maps to a database table. Hibernate scans for classes annotated with `@Entity` and creates metadata for ORM operations.
- **What breaks if removed**: Hibernate doesn't know about the `User` class. `UserRepository` fails at startup because JPA cannot resolve the entity type → `IllegalArgumentException: Not a managed type: User`.

```java
@Table(name="users")
```
- **What it does**: Specifies the database table name. Without it, JPA would default to the class name (`User`) as the table name.
- **What breaks if removed**: Hibernate would look for a table named `user` instead of `users`. If the table is actually named `users`, the query fails with `SQLSyntaxErrorException`.

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(name = "user_id")
private UUID id;
```
- **`@Id`**: Marks this field as the primary key. JPA requires exactly one `@Id` per entity.
- **`@GeneratedValue(strategy = GenerationType.UUID)`**: Hibernate auto-generates a UUID when a new entity is persisted. Not relevant for `findAll()` (reading, not writing).
- **`@Column(name = "user_id")`**: Maps the `id` Java field to the `user_id` column in the database.
- **What breaks if `@Id` removed**: JPA cannot identify the primary key → `AnnotationException: No identifier specified for entity: User`. Application fails to start.
- **What breaks if `@Column` removed**: Hibernate defaults to column name `id`, which may not match the actual database column `user_id` → SQL error during queries.

```java
@Column(name = "user_email", unique = true, length = 300)
private String email;
```
- **`name = "user_email"`**: Maps to column `user_email` in the DB.
- **`unique = true`**: DDL constraint — Hibernate adds a UNIQUE index on `user_email` when `ddl-auto: update` runs.
- **`length = 300`**: DDL constraint — column length is `VARCHAR(300)`.
- **For `findAll()`**: Hibernate reads the `user_email` column and sets `user.email` via the setter.

```java
private String name;
private String password;
private String image;
private boolean enable = true;
private Instant createdAt = Instant.now();
private Instant updatedAt = Instant.now();
```
- **Why defaults (`= true`, `= Instant.now()`)**: These are Java-level defaults for new objects. When Hibernate loads from the database, it **overwrites** these defaults with the actual DB values.
- **For `findAll()`**: Hibernate reads each column and calls the corresponding setter.

```java
@Enumerated(EnumType.STRING)
private Provider provider = Provider.LOCAL;
```
- **`@Enumerated(EnumType.STRING)`**: Tells Hibernate to store/read the enum as a **String** (e.g., `"LOCAL"`, `"GOOGLE"`) rather than an ordinal integer (0, 1, 2...).
- **For `findAll()`**: Hibernate reads the `provider` column (a VARCHAR containing `"LOCAL"` or `"GOOGLE"`) and converts it to the corresponding `Provider` enum constant.
- **What breaks if removed**: Hibernate defaults to `EnumType.ORDINAL`, storing/reading integers. If existing data has strings, it fails with a type conversion error.

```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles = new HashSet<>();
```
- **`@ManyToMany`**: Defines a many-to-many relationship between `User` and `Role`. One user can have many roles; one role can belong to many users.
- **`fetch = FetchType.EAGER`**: **Critical for `findAll()`**. Tells Hibernate to immediately load the associated `Role` entities whenever a `User` is fetched. Without this, roles would be loaded lazily (only when `getRoles()` is called), which would fail outside a transaction/session.
- **`@JoinTable(name = "user_roles")`**: Specifies the join table name in the database.
  - `joinColumns = @JoinColumn(name = "user_id")`: The FK column in `user_roles` pointing to `users.user_id`.
  - `inverseJoinColumns = @JoinColumn(name = "role_id")`: The FK column in `user_roles` pointing to `roles.id`.
- **For `findAll()`**: Hibernate fetches roles by querying the `user_roles` join table and joining with `roles` table, then populates each `User`'s `roles` set.
- **What breaks if `EAGER` changed to `LAZY`**: Roles would NOT be loaded during `findAll()`. Since `getAllUsers()` has `@Transactional`, the session is still open when `.map()` accesses `user.getRoles()`, so it would actually **still work** in this case. But if `@Transactional` were removed from `getAllUsers()`, it would throw `LazyInitializationException`.

```java
@PrePersist
protected void onCreate(){
   Instant now = Instant.now();
   if(createdAt == null) createdAt = now;
   updatedAt = now;
}

@PreUpdate
protected void onUpdate(){
   updatedAt = Instant.now();
}
```
- **`@PrePersist`**: JPA lifecycle callback, runs before a new entity is inserted into the database.
- **`@PreUpdate`**: JPA lifecycle callback, runs before an existing entity is updated.
- **For `findAll()`**: Neither callback fires — `findAll()` only reads data; it doesn't persist or update entities.

---

### 5.6 UserDto.java

```java
@Getter
@Setter
```
- **Who uses them in `getAllUsers()`**: `ModelMapper` uses setters to populate the DTO. Jackson uses getters to serialise the DTO to JSON.
- **What breaks if `@Getter` removed**: Jackson cannot read the field values → JSON response would be an empty object `{}` or throw an error.
- **What breaks if `@Setter` removed**: ModelMapper cannot write field values → all fields in the DTO would retain their default values.

```java
@AllArgsConstructor
@NoArgsConstructor
```
- **`@NoArgsConstructor`**: Generates `public UserDto() {}`. **Required by ModelMapper** — it creates DTO instances using the no-arg constructor before setting fields via setters.
- **What breaks if `@NoArgsConstructor` removed**: ModelMapper cannot instantiate `UserDto` → `MappingException` at runtime.

```java
@Builder
```
- Not used in the `getAllUsers()` flow directly.

```java
private UUID id;
private String email;
private String name;
private String password;
private String image;
private boolean enable = true;
private Instant createdAt = Instant.now();
private Instant updatedAt = Instant.now();
private Provider provider = Provider.LOCAL;
private Set<Role> roles = new HashSet<>();
```
- **Field-by-field**: Each field mirrors a field in the `User` entity. ModelMapper matches them by name.
- **Defaults** (e.g., `enable = true`, `createdAt = Instant.now()`): When ModelMapper creates a `UserDto`, these are the initial values. ModelMapper then **overwrites** them with values from the `User` entity.

> [!NOTE]
> The `password` field is included in the DTO and therefore **exposed in the API response**. In production, this is a security vulnerability — passwords should never be sent to clients.

---

### 5.7 ProjectConfig.java

```java
@Configuration
```
- **Who processes**: Spring's component scanner.
- **What it does**: Marks this class as a source of bean definitions.

```java
@Bean
public ModelMapper modelMapper(){
    return new ModelMapper();
}
```
- **Who calls it**: Spring's IoC container during startup (exactly once).
- **What it does**: Creates a `ModelMapper` instance and registers it as a Spring bean.
- **Who uses it**: `UserServiceImpl` receives this bean via constructor injection.
- **What breaks if removed**: Spring cannot find a `ModelMapper` bean → `NoSuchBeanDefinitionException` when creating `UserServiceImpl` → application fails to start.

---

### 5.8 Role.java — Entity

```java
@Entity
@Table(name = "roles")
public class Role {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(unique = true, nullable = false)
  private String name;
}
```
- **For `findAll()`**: When Hibernate eagerly loads roles for each user, it instantiates `Role` objects from the `roles` table.
- **`@Id private UUID id`**: The primary key. Hibernate uses this to identify role records.
- **`private String name`**: The role name (e.g., `"ROLE_USER"`, `"ROLE_ADMIN"`). This gets serialised into the JSON response as part of each user's `roles` array.

---

### 5.9 Provider.java — Enum

```java
public enum Provider {
   LOCAL,
   GOOGLE,
   GITHUB,
   FACEBOOK
}
```
- **For `findAll()`**: Hibernate reads the `provider` column (e.g., `"LOCAL"`, `"GOOGLE"`) and converts it to the corresponding enum constant using `Provider.valueOf("LOCAL")`.
- Jackson serialises the enum to its string name in the JSON response.

---

## 6. Data Flow Tracking

### Complete Data Transformation Pipeline

```
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 1: MySQL Database                                              │
│                                                                      │
│ users table (2 rows) + user_roles table (3 rows) + roles table       │
│ Raw relational data in rows and columns                              │
└──────────────────────────────┬───────────────────────────────────────┘
                               ↓ SQL SELECT queries
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 2: Hibernate / JPA                                             │
│                                                                      │
│ ResultSet → User entity objects (with Set<Role> populated)           │
│ List<User> = [User(Aditya), User(Priya)]                             │
│ Objects are managed by Hibernate's persistence context               │
└──────────────────────────────┬───────────────────────────────────────┘
                               ↓ .stream().map(modelMapper::map)
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 3: ModelMapper                                                 │
│                                                                      │
│ User entity → UserDto (field-by-field copy via reflection)           │
│ List<UserDto> = [UserDto(Aditya), UserDto(Priya)]                    │
│ Objects are plain POJOs, detached from JPA                           │
└──────────────────────────────┬───────────────────────────────────────┘
                               ↓ ResponseEntity.ok(list)
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 4: Spring MVC (Controller)                                     │
│                                                                      │
│ ResponseEntity<Iterable<UserDto>>                                    │
│ Status: 200 OK                                                       │
│ Body: List<UserDto>                                                  │
└──────────────────────────────┬───────────────────────────────────────┘
                               ↓ Jackson serialisation
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 5: Jackson (HttpMessageConverter)                              │
│                                                                      │
│ List<UserDto> → JSON array string                                    │
│ Each UserDto → JSON object with all fields                           │
│ UUID → "string", Instant → "ISO-8601 string", Enum → "string"       │
└──────────────────────────────┬───────────────────────────────────────┘
                               ↓ HTTP response
┌─────────────────────────────────────────────────────────────────────┐
│ LAYER 6: Tomcat                                                      │
│                                                                      │
│ HTTP/1.1 200 OK                                                      │
│ Content-Type: application/json                                       │
│ Body: [{"id":"a1b2...","email":"aditya@...",...}, {...}]              │
└─────────────────────────────────────────────────────────────────────┘
```

### Object State at Each Layer

#### After `userRepository.findAll()` — Two `User` Entities

```java
// User #1 (Aditya) — JPA-managed entity
User {
    id       = UUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
    email    = "aditya@example.com",
    name     = "Aditya",
    password = "hashed_password_1",
    image    = "aditya.png",
    enable   = true,
    createdAt = Instant.parse("2026-06-25T10:00:00Z"),
    updatedAt = Instant.parse("2026-06-25T10:00:00Z"),
    provider  = Provider.LOCAL,
    roles     = HashSet{ Role(id=r1r1..., name="ROLE_USER") }
}

// User #2 (Priya) — JPA-managed entity
User {
    id       = UUID("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
    email    = "priya@example.com",
    name     = "Priya",
    password = "hashed_password_2",
    image    = "priya.png",
    enable   = true,
    createdAt = Instant.parse("2026-06-26T14:30:00Z"),
    updatedAt = Instant.parse("2026-06-26T14:30:00Z"),
    provider  = Provider.GOOGLE,
    roles     = HashSet{ Role(id=r1r1..., name="ROLE_USER"), Role(id=r2r2..., name="ROLE_ADMIN") }
}
```

#### After `.map(user -> modelMapper.map(user, UserDto.class))` — Two `UserDto` Objects

```java
// UserDto #1 (Aditya) — plain POJO, no JPA management
UserDto {
    id       = UUID("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),  // same UUID value, different object
    email    = "aditya@example.com",
    name     = "Aditya",
    password = "hashed_password_1",
    image    = "aditya.png",
    enable   = true,
    createdAt = Instant.parse("2026-06-25T10:00:00Z"),
    updatedAt = Instant.parse("2026-06-25T10:00:00Z"),
    provider  = Provider.LOCAL,
    roles     = HashSet{ Role(id=r1r1..., name="ROLE_USER") }
}

// UserDto #2 (Priya) — plain POJO
UserDto {
    id       = UUID("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
    email    = "priya@example.com",
    name     = "Priya",
    password = "hashed_password_2",
    image    = "priya.png",
    enable   = true,
    createdAt = Instant.parse("2026-06-26T14:30:00Z"),
    updatedAt = Instant.parse("2026-06-26T14:30:00Z"),
    provider  = Provider.GOOGLE,
    roles     = HashSet{ Role(id=r1r1..., name="ROLE_USER"), Role(id=r2r2..., name="ROLE_ADMIN") }
}
```

#### After Jackson Serialisation — JSON String

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "aditya@example.com",
    "name": "Aditya",
    "password": "hashed_password_1",
    "image": "aditya.png",
    "enable": true,
    "createdAt": "2026-06-25T10:00:00Z",
    "updatedAt": "2026-06-25T10:00:00Z",
    "provider": "LOCAL",
    "roles": [
      {
        "id": "r1r1r1r1-aaaa-bbbb-cccc-dddddddddddd",
        "name": "ROLE_USER"
      }
    ]
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "email": "priya@example.com",
    "name": "Priya",
    "password": "hashed_password_2",
    "image": "priya.png",
    "enable": true,
    "createdAt": "2026-06-26T14:30:00Z",
    "updatedAt": "2026-06-26T14:30:00Z",
    "provider": "GOOGLE",
    "roles": [
      {
        "id": "r1r1r1r1-aaaa-bbbb-cccc-dddddddddddd",
        "name": "ROLE_USER"
      },
      {
        "id": "r2r2r2r2-eeee-ffff-0000-111111111111",
        "name": "ROLE_ADMIN"
      }
    ]
  }
]
```

### Jackson Serialisation Rules

| Java Type | JSON Type | Example |
|-----------|-----------|---------|
| `UUID` | `string` | `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"` |
| `String` | `string` | `"Aditya"` |
| `boolean` | `boolean` | `true` |
| `Instant` | `string` (ISO-8601) | `"2026-06-25T10:00:00Z"` |
| `Provider` (enum) | `string` (enum name) | `"LOCAL"` |
| `Set<Role>` | `array` of objects | `[{"id":"...","name":"ROLE_USER"}]` |
| `List<UserDto>` | `array` of objects | `[{...}, {...}]` |

---

## 7. Annotation Explanations in Context

This section explains every annotation encountered during the `GET /api/v1/users` execution flow, **in the order they are processed**.

### Startup-Time Annotations (processed once when the application boots)

| Annotation | Class | Purpose | When Processed |
|---|---|---|---|
| `@Configuration` | `ProjectConfig` | Marks the class as a bean definition source | Component scanning at startup |
| `@Bean` | `ProjectConfig.modelMapper()` | Registers the returned object as a Spring bean | Bean definition phase at startup |
| `@Entity` | `User`, `Role` | Registers the class as a JPA entity with Hibernate | Hibernate metadata scanning at startup |
| `@Table(name=...)` | `User`, `Role` | Specifies the database table name | Hibernate metadata scanning at startup |
| `@Id` | `User.id`, `Role.id` | Marks the primary key field | Hibernate metadata scanning at startup |
| `@GeneratedValue(...)` | `User.id` | Configures PK auto-generation strategy | Hibernate metadata scanning at startup |
| `@Column(...)` | Various fields | Maps field to specific column with constraints | Hibernate metadata scanning at startup |
| `@Enumerated(EnumType.STRING)` | `User.provider` | Store enum as string, not ordinal | Hibernate metadata scanning at startup |
| `@ManyToMany(fetch=EAGER)` | `User.roles` | Defines relationship + fetch strategy | Hibernate metadata scanning at startup |
| `@JoinTable(...)` | `User.roles` | Configures the join table for M:N relationship | Hibernate metadata scanning at startup |
| `@Service` | `UserServiceImpl` | Registers as a Spring service bean | Component scanning at startup |
| `@RestController` | `UserController` | Registers as a controller + response body | Component scanning at startup |
| `@RequestMapping("/api/v1/users")` | `UserController` | Sets base URL for all methods | Handler mapping registration at startup |
| `@GetMapping` | `UserController.getAllUsers()` | Maps GET requests to this method | Handler mapping registration at startup |
| `@AllArgsConstructor` | `UserController` | Generates constructor for DI | Lombok at compile time |
| `@RequiredArgsConstructor` | `UserServiceImpl` | Generates constructor for final fields | Lombok at compile time |
| `@Getter`, `@Setter` | `User`, `UserDto`, `Role` | Generate getter/setter methods | Lombok at compile time |
| `@NoArgsConstructor` | `User`, `UserDto`, `Role` | Generate no-arg constructor | Lombok at compile time |
| `@Builder` | `User`, `UserDto`, `Role` | Generate builder pattern | Lombok at compile time |

### Runtime Annotations (processed during each request)

| Annotation | When Triggered | What Happens |
|---|---|---|
| `@GetMapping` | DispatcherServlet routing | Matches `GET /api/v1/users` to `getAllUsers()` |
| `@Transactional` | Proxy intercepts `getAllUsers()` | Opens DB transaction before, commits after |
| `@PrePersist` | _(not triggered)_ | Would run before entity insert — irrelevant for GET |
| `@PreUpdate` | _(not triggered)_ | Would run before entity update — irrelevant for GET |

---

## 8. Summary

### 8.1 Execution Flow Summary

```
GET /api/v1/users
    → Tomcat (port 8083) receives HTTP request
    → DispatcherServlet routes to UserController.getAllUsers()
    → Controller calls userService.getAllUsers()
    → Spring's @Transactional proxy opens DB transaction
    → UserServiceImpl.getAllUsers() executes
    → userRepository.findAll() → Hibernate generates SQL
    → MySQL executes SELECT on users, user_roles, roles tables
    → Hibernate hydrates List<User> with Set<Role> populated
    → Stream created from List<User>
    → .map() transforms each User → UserDto via ModelMapper
    → .toList() collects into List<UserDto>
    → @Transactional proxy commits transaction
    → Controller wraps List<UserDto> in ResponseEntity (200 OK)
    → Jackson serialises List<UserDto> to JSON array
    → Tomcat sends HTTP 200 OK with JSON body
```

### 8.2 Data Flow Summary

```
MySQL rows (users + user_roles + roles)
    → JDBC ResultSet
    → Hibernate entity objects: List<User>    [JPA-managed, with Set<Role>]
    → Java Stream<User>                       [same objects, streaming]
    → ModelMapper: Stream<UserDto>            [new plain POJOs]
    → List<UserDto>                           [collected, immutable]
    → ResponseEntity<Iterable<UserDto>>       [wrapped with 200 OK]
    → JSON array string                       [serialised by Jackson]
    → HTTP response bytes                     [sent by Tomcat]
```

### 8.3 Objects Created During This Request

| Object | Created By | Purpose | Count |
|--------|-----------|---------|-------|
| `HttpServletRequest` | Tomcat | Represents the incoming HTTP request | 1 |
| `HttpServletResponse` | Tomcat | Represents the outgoing HTTP response | 1 |
| `User` entity (Aditya) | Hibernate | Represents first DB row as Java object | 1 |
| `User` entity (Priya) | Hibernate | Represents second DB row as Java object | 1 |
| `Role` entity (ROLE_USER) | Hibernate | Loaded eagerly with users | 1 (shared) |
| `Role` entity (ROLE_ADMIN) | Hibernate | Loaded eagerly with Priya | 1 |
| `List<User>` | `findAll()` | Contains all User entities | 1 |
| `Stream<User>` | `.stream()` | Functional pipeline over users | 1 |
| `UserDto` (Aditya) | ModelMapper | DTO copy of Aditya's data | 1 |
| `UserDto` (Priya) | ModelMapper | DTO copy of Priya's data | 1 |
| `List<UserDto>` | `.toList()` | Final collection of DTOs | 1 |
| `ResponseEntity` | `ResponseEntity.ok()` | Wraps body + HTTP status | 1 |

### 8.4 Methods Called (in Order)

| # | Method | Class | Input | Output |
|---|--------|-------|-------|--------|
| 1 | `getAllUsers()` | `UserController` | _(none)_ | `ResponseEntity<Iterable<UserDto>>` |
| 2 | `getAllUsers()` | Transactional Proxy → `UserServiceImpl` | _(none)_ | `List<UserDto>` |
| 3 | `findAll()` | `SimpleJpaRepository` (via `UserRepository` proxy) | _(none)_ | `List<User>` (2 entities) |
| 4 | `stream()` | `ArrayList<User>` | _(none)_ | `Stream<User>` |
| 5 | `map(lambda)` | `Stream<User>` | Lambda: `user → modelMapper.map(user, UserDto.class)` | `Stream<UserDto>` |
| 6 | `modelMapper.map(user1, UserDto.class)` | `ModelMapper` | `User(Aditya)`, `UserDto.class` | `UserDto(Aditya)` |
| 7 | `modelMapper.map(user2, UserDto.class)` | `ModelMapper` | `User(Priya)`, `UserDto.class` | `UserDto(Priya)` |
| 8 | `toList()` | `Stream<UserDto>` | _(none)_ | `List<UserDto>` (2 DTOs) |
| 9 | `ResponseEntity.ok(list)` | `ResponseEntity` (static) | `List<UserDto>` | `ResponseEntity<Iterable<UserDto>>` with status 200 |
| 10 | Jackson serialisation | `MappingJackson2HttpMessageConverter` | `List<UserDto>` | JSON string |

### 8.5 Database Operations

| # | Operation | SQL Generated | Rows Affected | Purpose |
|---|-----------|--------------|---------------|---------|
| 1 | SELECT all users | `SELECT * FROM users` | 2 rows read | Fetch all user records |
| 2 | SELECT roles for users | `SELECT ... FROM user_roles JOIN roles WHERE user_id IN (?, ?)` | 3 rows read | Eager-fetch roles for both users |

> [!NOTE]
> This is a **read-only** operation. No `INSERT`, `UPDATE`, or `DELETE` queries are executed. The `@Transactional` wrapping ensures a consistent read snapshot and is committed as a no-op (no dirty entities to flush).

### 8.6 Final HTTP Response

```
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 27 Jun 2026 15:17:00 GMT

[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "aditya@example.com",
    "name": "Aditya",
    "password": "hashed_password_1",
    "image": "aditya.png",
    "enable": true,
    "createdAt": "2026-06-25T10:00:00Z",
    "updatedAt": "2026-06-25T10:00:00Z",
    "provider": "LOCAL",
    "roles": [
      { "id": "r1r1r1r1-aaaa-bbbb-cccc-dddddddddddd", "name": "ROLE_USER" }
    ]
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "email": "priya@example.com",
    "name": "Priya",
    "password": "hashed_password_2",
    "image": "priya.png",
    "enable": true,
    "createdAt": "2026-06-26T14:30:00Z",
    "updatedAt": "2026-06-26T14:30:00Z",
    "provider": "GOOGLE",
    "roles": [
      { "id": "r1r1r1r1-aaaa-bbbb-cccc-dddddddddddd", "name": "ROLE_USER" },
      { "id": "r2r2r2r2-eeee-ffff-0000-111111111111", "name": "ROLE_ADMIN" }
    ]
  }
]
```

### 8.7 Edge Cases and Interview Talking Points

| Scenario | Behaviour |
|----------|-----------|
| **Empty database (0 users)** | `findAll()` returns empty `List<User>`. Stream produces empty `List<UserDto>`. Response: `200 OK` with `[]` (empty JSON array). |
| **1000+ users** | All users are loaded into memory at once. No pagination. This is a potential **performance issue** — in production, use `PagingAndSortingRepository.findAll(Pageable)` to return paginated results. |
| **Database connection failure** | `findAll()` throws `DataAccessException`. Spring returns `500 Internal Server Error`. |
| **Password exposure** | The `UserDto` includes `password`. This is a **security vulnerability**. Fix: use `@JsonIgnore` on the password field or create a separate response DTO without the password. |
| **N+1 query problem** | Avoided here because `FetchType.EAGER` on roles causes Hibernate to batch-fetch roles. However, with many users, eager fetching can still be inefficient. Consider `@EntityGraph` or `JOIN FETCH` JPQL for optimised fetching. |
| **Thread safety** | `UserController` and `UserServiceImpl` are singletons (default Spring scope), but they are stateless (no mutable instance fields), so they are thread-safe. |
| **No security** | Any unauthenticated client can call this endpoint. In production, add `@PreAuthorize("hasRole('ADMIN')")` or configure `SecurityFilterChain`. |

---

> **Document generated for interview preparation. Covers the complete execution trace of `GET /api/v1/users` from HTTP request through every Spring layer to JSON response.**
