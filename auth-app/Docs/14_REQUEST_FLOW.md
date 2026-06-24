# Section 4: The Complete Request Lifecycle — From Button Click to Database and Back

Let's understand what happens when a user interacts with your application.

Every single step.

From the moment they click a button to the moment they see a response on screen.

---

# The Big Picture

```text
User Clicks Button
       ↓
Frontend Creates HTTP Request
       ↓
DNS Resolves Server Address
       ↓
TCP Connection Established
       ↓
HTTP Request Sent Over Network
       ↓
Tomcat Receives Request
       ↓
Spring Security Filter Chain
       ↓
DispatcherServlet Routes Request
       ↓
Controller Handles Request          ← DOES NOT EXIST YET
       ↓
Service Processes Business Logic
       ↓
Repository Talks to Database
       ↓
Hibernate Converts to SQL
       ↓
HikariCP Provides Connection
       ↓
MySQL Executes Query
       ↓
Result Travels Back Up
       ↓
JSON Response Sent to Frontend
       ↓
User Sees the Result
```

This entire journey takes milliseconds.

Let's understand each step.

---

# Part 1: The Frontend — Where It All Begins

## What Happens When a User Clicks a Button?

Imagine a registration page.

The user fills out:

```text
Name:     Aditya
Email:    aditya@gmail.com
Password: Secret123
```

And clicks:

```text
[ Register ]
```

---

## Step 1: JavaScript Collects Form Data

The frontend (React, Angular, or plain HTML) collects the input values.

```javascript
const userData = {
    name: "Aditya",
    email: "aditya@gmail.com",
    password: "Secret123"
};
```

---

## Step 2: Frontend Creates an HTTP Request

The frontend needs to send this data to your Spring Boot server.

It uses something like `fetch` or `axios`:

```javascript
fetch("http://localhost:8083/api/users", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify(userData)
});
```

This creates an HTTP Request.

---

## What Is an HTTP Request?

Think of it like a letter.

```text
HTTP Request = A Letter

Envelope (Headers):
    To:           localhost:8083
    Method:       POST
    Content-Type: application/json

Letter Inside (Body):
    {
        "name": "Aditya",
        "email": "aditya@gmail.com",
        "password": "Secret123"
    }
```

---

## HTTP Methods — What Are They?

| Method | Meaning         | Real-Life Analogy       |
|--------|-----------------|-------------------------|
| GET    | Read data       | "Show me the menu"      |
| POST   | Create data     | "Place a new order"     |
| PUT    | Update data     | "Change my order"       |
| DELETE | Delete data     | "Cancel my order"       |

Our registration uses POST because we are **creating** a new user.

---

# Part 2: Network Communication — How the Request Travels

## Step 3: DNS Resolution

Your browser sees:

```text
localhost:8083
```

Question:

> Where is this server?

For `localhost`, the answer is simple:

```text
localhost → 127.0.0.1 (your own machine)
```

In production, DNS resolves:

```text
api.myapp.com → 52.14.98.73
```

Think of DNS like a phone book:

```text
DNS
------
Name:    api.myapp.com
Address: 52.14.98.73
```

---

## Step 4: TCP Connection (Three-Way Handshake)

Before sending data, the browser establishes a connection.

```text
Browser  → Server:  "SYN  — Hey, can we talk?"
Server   → Browser: "SYN-ACK — Sure, I'm ready."
Browser  → Server:  "ACK — Great, let's go!"
```

This is called the TCP Three-Way Handshake.

Think of it like a phone call:

```text
You:    "Hello?"
Friend: "Hey, I can hear you."
You:    "Perfect, let's talk."
```

Only after this handshake does data flow.

---

## Step 5: HTTP Request Sent Over the Network

Now the actual HTTP request is sent.

```text
POST /api/users HTTP/1.1
Host: localhost:8083
Content-Type: application/json
Content-Length: 89

{
    "name": "Aditya",
    "email": "aditya@gmail.com",
    "password": "Secret123"
}
```

This is raw text traveling over the TCP connection.

---

# Part 3: Tomcat Receives the Request

## What Is Tomcat?

Spring Boot has an embedded web server called Tomcat.

```text
Tomcat = The Receptionist

It receives every incoming request
and passes it to the right handler.
```

Tomcat listens on port `8083` (configured in your application.properties).

---

## What Tomcat Does

```text
Network Request Arrives
        ↓
Tomcat Accepts Connection
        ↓
Creates HttpServletRequest Object
        ↓
Creates HttpServletResponse Object
        ↓
Passes to Filter Chain
```

Tomcat converts the raw HTTP text into Java objects:

```text
Raw HTTP Text
     ↓
HttpServletRequest
     (Java Object)
```

Now Spring can work with it.

---

# Part 4: Spring Security Filter Chain

## Why Does This Matter?

Your `pom.xml` includes:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

The moment this dependency is present, Spring Security activates.

---

## Current State: Default Security Behavior

Since you have NOT configured Spring Security yet:

```text
⚠️ CURRENT STATE:
Spring Security uses DEFAULT configuration.
```

Default behavior:

```text
1. ALL endpoints are protected
2. A default login page appears at /login
3. Username: "user"
4. Password: printed in console logs at startup
5. CSRF protection is enabled
6. Session-based authentication is active
```

This means:

```text
Every request → Must be authenticated first
```

If you try to hit `POST /api/users` without logging in:

```text
HTTP 401 Unauthorized
```

or you get redirected to:

```text
/login
```

---

## What Is a Filter Chain?

Think of it like airport security.

```text
Passenger (Request)
      ↓
Security Gate 1: Check Passport  (Authentication Filter)
      ↓
Security Gate 2: Check Visa      (Authorization Filter)
      ↓
Security Gate 3: Check Luggage   (CSRF Filter)
      ↓
Security Gate 4: Body Scan       (Session Filter)
      ↓
Boarding Gate (DispatcherServlet)
```

Spring Security has approximately 15 default filters.

---

## The Default Filter Chain

```text
Request Arrives
      ↓
SecurityContextPersistenceFilter     → Load security context
      ↓
CsrfFilter                          → Check CSRF token
      ↓
LogoutFilter                         → Handle /logout
      ↓
UsernamePasswordAuthenticationFilter → Handle /login
      ↓
BasicAuthenticationFilter            → Handle Basic Auth
      ↓
AnonymousAuthenticationFilter        → Assign anonymous role
      ↓
ExceptionTranslationFilter           → Handle auth errors
      ↓
FilterSecurityInterceptor            → Check authorization
      ↓
DispatcherServlet                    → Route to controller
```

---

## What SHOULD Be Configured (Future)

For your auth application, you will eventually configure:

```java
// ⚠️ THIS FILE DOES NOT EXIST YET
// File: config/SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

This would:

```text
1. Disable CSRF for API calls
2. Allow /api/users/** without authentication
3. Require authentication for everything else
```

---

# Part 5: DispatcherServlet — The Traffic Controller

## What Is DispatcherServlet?

After passing through Security filters, the request reaches DispatcherServlet.

```text
DispatcherServlet = Traffic Police

It looks at the request URL and HTTP method
and decides which Controller should handle it.
```

---

## How DispatcherServlet Works

```text
Request: POST /api/users
              ↓
DispatcherServlet Receives It
              ↓
Checks Handler Mapping:
    "Which @Controller handles /api/users?"
              ↓
Finds: UserController.createUser()        ← DOES NOT EXIST YET
              ↓
Calls that method
              ↓
Gets response
              ↓
Converts to JSON (via HttpMessageConverter)
              ↓
Sends back to client
```

---

## The Complete DispatcherServlet Flow

```text
HttpServletRequest
        ↓
DispatcherServlet.doDispatch()
        ↓
HandlerMapping.getHandler()
    → Scans @RequestMapping annotations
    → Matches URL pattern to method
        ↓
HandlerAdapter.handle()
    → Deserializes JSON body to Java object
    → Calls the controller method
    → Serializes return value to JSON
        ↓
ViewResolver (for REST APIs, not used)
        ↓
HttpServletResponse
```

---

## What Is Handler Mapping?

DispatcherServlet maintains a map:

```text
URL Pattern           →  Handler Method
──────────────────────────────────────────
POST   /api/users     →  UserController.createUser()
GET    /api/users/{e} →  UserController.getUserByEmail()
PUT    /api/users/{id}→  UserController.updateUser()
DELETE /api/users/{id}→  UserController.deleteUser()
GET    /api/users     →  UserController.getAllUsers()
```

Spring builds this map at startup by scanning all `@RestController` classes.

---

# Part 6: Controller Layer — The Front Door

## Current State

```text
⚠️ CURRENT STATE:
NO Controller class exists.
No REST endpoints are defined.
```

This means:

```text
DispatcherServlet receives request
         ↓
Cannot find any handler
         ↓
Returns: 404 Not Found
    (or 401 because Security blocks it first)
```

---

## What SHOULD Exist

A controller would be the "front door" of your application.

```java
// ⚠️ THIS FILE DOES NOT EXIST YET
// File: controllers/UserController.java

package com.substring.auth.auth_app.controllers;

import com.substring.auth.auth_app.dtos.UserDto;
import com.substring.auth.auth_app.services.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        UserDto created = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}
```

---

## What Do the Annotations Mean?

```text
@RestController
    → "This class handles HTTP requests
       and returns JSON responses."

@RequestMapping("/api/users")
    → "All endpoints in this class
       start with /api/users."

@PostMapping
    → "This method handles POST requests."

@RequestBody
    → "Convert the JSON body into
       a Java object (UserDto)."

@PathVariable
    → "Extract value from the URL path."
```

---

## Real-Life Analogy

```text
Controller = Restaurant Waiter

Customer (Client) tells the waiter what they want.
Waiter (Controller) passes the order to the kitchen.
Kitchen (Service) prepares the food.
Waiter brings the food back.

The waiter does NOT cook.
The controller does NOT contain business logic.
```

---

# Part 7: Service Layer — Business Logic

## Current State

```text
✅ EXISTS: UserServiceImpl.java
⚠️ BUT: All methods return null (STUBS)
```

Your current service:

```java
@Service
public class UserServiceImpl implements UserService {

    @Override
    public UserDto createUser(UserDto userDto) {
        return null;  // ← STUB: Returns nothing
    }

    @Override
    public UserDto getUserByEmail(String email) {
        return null;  // ← STUB: Returns nothing
    }

    // ... other methods also return null
}
```

---

## What the Service Layer SHOULD Do

```text
Controller calls Service
         ↓
Service receives UserDto
         ↓
Validates business rules
         ↓
Converts DTO → Entity
         ↓
Calls Repository to save
         ↓
Converts Entity → DTO
         ↓
Returns DTO to Controller
```

---

## What a Complete Service Would Look Like

```java
// ⚠️ THIS IS THE INTENDED IMPLEMENTATION (not yet coded)

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDto createUser(UserDto userDto) {

        // Step 1: Validate - does email already exist?
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Step 2: Convert DTO → Entity
        User user = User.builder()
            .name(userDto.getName())
            .email(userDto.getEmail())
            .password(userDto.getPassword())  // Should be hashed!
            .image(userDto.getImage())
            .provider(userDto.getProvider())
            .build();

        // Step 3: Save to database
        User savedUser = userRepository.save(user);

        // Step 4: Convert Entity → DTO
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

        return responseDto;
    }
}
```

---

## Why DTO ↔ Entity Conversion?

Question:

> Why not just use the Entity directly?

Answer:

```text
Entity = Database representation
DTO    = What we show to the client
```

Suppose your User entity has:

```text
password: "$2a$10$8a6..."
```

You do NOT want to send the hashed password to the frontend.

```text
Entity (Full Data)
      ↓
Convert
      ↓
DTO (Safe Data — no password)
```

This is called:

```text
Data Transfer Object Pattern
```

---

## Current DTO Issue

Your current `UserDto` includes:

```java
private String password;
```

In a proper implementation, you might have:

```text
UserDto (for responses)  → No password field
UserCreateDto (for input) → Has password field
```

This separation prevents accidentally leaking sensitive data.

---

# Part 8: Repository Layer — Database Gateway

## Current State

```text
✅ EXISTS: UserRepository.java
✅ FULLY FUNCTIONAL (Spring Data JPA auto-implements it)
```

Your repository:

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

## How Does This Work Without Any Code?

This is the magic of Spring Data JPA.

You write an interface.

Spring writes the implementation.

```text
You Write:
    Optional<User> findByEmail(String email);

Spring Generates:
    SELECT * FROM users WHERE user_email = ?
```

---

## Method Name → SQL Query

Spring reads your method name and creates SQL:

| Method Name       | Generated SQL                                    |
|-------------------|--------------------------------------------------|
| `findByEmail`     | `SELECT * FROM users WHERE user_email = ?`       |
| `existsByEmail`   | `SELECT COUNT(*) > 0 FROM users WHERE user_email = ?` |
| `findAll`         | `SELECT * FROM users`                            |
| `findById`        | `SELECT * FROM users WHERE user_id = ?`          |
| `save`            | `INSERT INTO users (...) VALUES (...)`           |
| `deleteById`      | `DELETE FROM users WHERE user_id = ?`            |

---

## JpaRepository Inheritance Chain

```text
UserRepository
      ↓ extends
JpaRepository<User, UUID>
      ↓ extends
PagingAndSortingRepository
      ↓ extends
CrudRepository
      ↓ extends
Repository
```

Each level adds methods:

```text
Repository
    → Marker interface (empty)

CrudRepository
    → save(), findById(), findAll(), deleteById(), count()

PagingAndSortingRepository
    → findAll(Sort), findAll(Pageable)

JpaRepository
    → flush(), saveAndFlush(), deleteInBatch()
```

---

# Part 9: Hibernate — The ORM Engine

## What Happens When Repository.save() Is Called?

```text
userRepository.save(user)
         ↓
Spring Data JPA delegates to
         ↓
EntityManager.persist(user)
         ↓
Hibernate's SessionImpl
         ↓
Hibernate generates SQL:
    INSERT INTO users
    (user_id, user_email, name, password, image, enable, created_at, updated_at, provider)
    VALUES
    (?, ?, ?, ?, ?, ?, ?, ?, ?)
         ↓
Sends SQL to database via JDBC
```

---

## Hibernate's Internal Process

```text
Entity Object (User)
        ↓
Dirty Checking
    "Has this object changed?"
        ↓
SQL Generation
    "What SQL do I need?"
        ↓
JDBC PreparedStatement
    "Fill in the ? values"
        ↓
Execute on Database Connection
```

---

## What Is Dirty Checking?

Hibernate tracks every entity it loads.

```text
Load User from DB
    → Hibernate keeps a snapshot

Modify User in Java
    user.setName("New Name")

Transaction Commits
    → Hibernate compares current state vs snapshot
    → Detects change in 'name' field
    → Generates: UPDATE users SET name = ? WHERE user_id = ?
```

This is called:

```text
Automatic Dirty Checking
```

You don't call save() for updates — Hibernate detects changes automatically.

---

# Part 10: HikariCP — Connection Pooling

## What Is HikariCP?

Every database query needs a connection.

Creating a connection is expensive:

```text
Without Pool:
    Request 1 → Open Connection → Query → Close Connection
    Request 2 → Open Connection → Query → Close Connection
    Request 3 → Open Connection → Query → Close Connection

    Each "Open Connection" takes ~5-10ms
```

With HikariCP:

```text
Application Starts
    → HikariCP creates 10 connections upfront

Request 1 → Borrow Connection → Query → Return Connection
Request 2 → Borrow Connection → Query → Return Connection
Request 3 → Borrow Connection → Query → Return Connection

No time wasted creating connections.
```

---

## How It Works in Your App

Your `application.properties` configures:

```text
spring.datasource.url=jdbc:mysql://localhost:3306/auth_application_java
```

Spring Boot auto-configures HikariCP:

```text
Spring Boot Starts
       ↓
Reads Database URL
       ↓
Creates HikariCP Pool
    → Default: 10 connections
       ↓
Connections Ready
       ↓
Hibernate borrows connections as needed
```

---

## Connection Pool Flow

```text
Hibernate: "I need a connection"
       ↓
HikariCP Pool:
    [Conn1: IDLE] [Conn2: IDLE] [Conn3: BUSY] ...
       ↓
HikariCP: "Here, take Conn1"
       ↓
Hibernate: Executes SQL on Conn1
       ↓
Hibernate: "Done, returning Conn1"
       ↓
HikariCP: Marks Conn1 as IDLE
```

---

# Part 11: MySQL — The Final Destination

## What MySQL Does

```text
SQL Arrives from Hibernate
         ↓
MySQL Parser
    → Checks syntax
         ↓
MySQL Query Optimizer
    → Finds best execution plan
    → Uses indexes if available
         ↓
MySQL Storage Engine (InnoDB)
    → Reads/writes actual data on disk
         ↓
Result Sent Back
```

---

## Your Database Structure

```text
Database: auth_application_java
    ↓
Tables:
    ├── users          (stores user data)
    ├── roles          (stores role data)
    └── user_roles     (join table: connects users ↔ roles)
```

The `users` table:

| Column     | Type         | Notes           |
|------------|--------------|-----------------|
| user_id    | BINARY(16)   | UUID, Primary Key |
| user_email | VARCHAR(300) | Unique          |
| name       | VARCHAR(255) |                 |
| password   | VARCHAR(255) |                 |
| image      | VARCHAR(255) |                 |
| enable     | BIT(1)       | Default: true   |
| created_at | DATETIME(6)  |                 |
| updated_at | DATETIME(6)  |                 |
| provider   | VARCHAR(255) | Enum as STRING  |

---

# Part 12: The Return Journey — Response Goes Back

## Step-by-Step Response Flow

```text
MySQL returns result rows
         ↓
JDBC ResultSet
         ↓
Hibernate maps ResultSet → User Entity
         ↓
Repository returns User to Service
         ↓
Service converts User → UserDto
         ↓
Controller wraps UserDto in ResponseEntity
         ↓
DispatcherServlet detects @RestController
         ↓
HttpMessageConverter (Jackson) converts UserDto → JSON
         ↓
JSON written to HttpServletResponse
         ↓
Tomcat sends HTTP Response over TCP
         ↓
Browser receives response
         ↓
JavaScript processes JSON
         ↓
UI updates with new data
```

---

## What Is Jackson?

Jackson is the JSON library Spring Boot uses.

```text
Java Object → Jackson → JSON String
JSON String → Jackson → Java Object
```

Example:

```text
UserDto Object:
    name: "Aditya"
    email: "aditya@gmail.com"

         ↓ Jackson serializes

JSON:
{
    "name": "Aditya",
    "email": "aditya@gmail.com"
}
```

This happens automatically. You never call Jackson directly.

Spring's `HttpMessageConverter` uses Jackson behind the scenes.

---

## The HTTP Response

```text
HTTP/1.1 201 Created
Content-Type: application/json

{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Aditya",
    "email": "aditya@gmail.com",
    "enable": true,
    "createdAt": "2026-06-20T12:00:00Z",
    "provider": "LOCAL",
    "roles": []
}
```

---

# Part 13: Validation — Checking Input Data

## Current State

```text
✅ Dependency exists: spring-boot-starter-validation
⚠️ BUT: No validation annotations are used
```

---

## What SHOULD Happen

UserDto should have validation:

```java
// ⚠️ INTENDED — not yet implemented

public class UserDto {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
```

And the controller should use `@Valid`:

```java
@PostMapping
public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
    // If validation fails, Spring returns 400 Bad Request automatically
}
```

---

## Validation Flow

```text
JSON arrives
     ↓
Jackson converts JSON → UserDto
     ↓
@Valid triggers validation
     ↓
Check @NotBlank, @Email, @Size
     ↓
If valid → Continue to service
If invalid → Throw MethodArgumentNotValidException
     ↓
Return 400 Bad Request with error details
```

---

# Part 14: Exception Handling

## Current State

```text
⚠️ NO exception handling exists.
If something goes wrong, Spring returns
a generic Whitelabel Error Page or raw stack trace.
```

---

## What SHOULD Exist

A global exception handler:

```java
// ⚠️ THIS FILE DOES NOT EXIST YET
// File: exceptions/GlobalExceptionHandler.java

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
            errors.put(err.getField(), err.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }
}
```

---

## How Exception Handling Works

```text
Exception thrown anywhere
         ↓
Propagates up the call stack
         ↓
DispatcherServlet catches it
         ↓
Checks @RestControllerAdvice classes
         ↓
Finds matching @ExceptionHandler
         ↓
Returns structured error response
```

Without it:

```text
User sees: 500 Internal Server Error
    with ugly stack trace
```

With it:

```text
User sees:
{
    "error": "Email already exists"
}
```

---

# Part 15: Frontend Rendering — Completing the Circle

## What the Frontend Does with the Response

```text
HTTP Response Arrives
         ↓
JavaScript parses JSON
         ↓
Updates application state
         ↓
Re-renders UI components
         ↓
User sees: "Registration Successful!"
```

Example:

```javascript
fetch("http://localhost:8083/api/users", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(userData)
})
.then(response => response.json())
.then(data => {
    // Success: show welcome message
    showMessage("Welcome, " + data.name + "!");
})
.catch(error => {
    // Error: show error message
    showMessage("Registration failed: " + error.message);
});
```

---

# The Complete Sequence Diagram

Here is the entire journey in one diagram:

```text
 ┌──────────┐    ┌──────────┐    ┌────────────┐    ┌────────────────┐    ┌────────────┐    ┌──────────┐    ┌──────────┐    ┌───────┐
 │ Browser  │    │  Tomcat  │    │  Security  │    │ Dispatcher     │    │ Controller │    │ Service  │    │Repository│    │ MySQL │
 │(Frontend)│    │          │    │  Filters   │    │ Servlet        │    │ (MISSING!) │    │(STUBS)   │    │(EXISTS)  │    │       │
 └────┬─────┘    └────┬─────┘    └─────┬──────┘    └───────┬────────┘    └─────┬──────┘    └────┬─────┘    └────┬─────┘    └───┬───┘
      │               │               │                   │                   │                │               │              │
      │  HTTP Request  │               │                   │                   │                │               │              │
      │──────────────→│               │                   │                   │                │               │              │
      │               │  Filter Chain │                   │                   │                │               │              │
      │               │──────────────→│                   │                   │                │               │              │
      │               │               │  Route Request    │                   │                │               │              │
      │               │               │──────────────────→│                   │                │               │              │
      │               │               │                   │  Call Handler     │                │               │              │
      │               │               │                   │──────────────────→│                │               │              │
      │               │               │                   │                   │  Call Service  │               │              │
      │               │               │                   │                   │───────────────→│               │              │
      │               │               │                   │                   │                │  Call Repo    │              │
      │               │               │                   │                   │                │──────────────→│              │
      │               │               │                   │                   │                │               │  SQL Query   │
      │               │               │                   │                   │                │               │─────────────→│
      │               │               │                   │                   │                │               │              │
      │               │               │                   │                   │                │               │  ResultSet   │
      │               │               │                   │                   │                │               │←─────────────│
      │               │               │                   │                   │                │  Entity       │              │
      │               │               │                   │                   │                │←──────────────│              │
      │               │               │                   │                   │  DTO           │               │              │
      │               │               │                   │                   │←───────────────│               │              │
      │               │               │                   │  ResponseEntity   │                │               │              │
      │               │               │                   │←──────────────────│                │               │              │
      │               │               │  JSON Response    │                   │                │               │              │
      │               │               │←──────────────────│                   │                │               │              │
      │               │  HTTP Response│                   │                   │                │               │              │
      │               │←──────────────│                   │                   │                │               │              │
      │  JSON Response │               │                   │                   │                │               │              │
      │←──────────────│               │                   │                   │                │               │              │
      │               │               │                   │                   │                │               │              │
```

---

# Summary: What EXISTS vs What NEEDS to be BUILT

| Layer                 | Status           | File                          |
|-----------------------|------------------|-------------------------------|
| Entity (User)         | ✅ EXISTS        | `entities/User.java`          |
| Entity (Role)         | ✅ EXISTS        | `entities/Role.java`          |
| Enum (Provider)       | ✅ EXISTS        | `entities/Provider.java`      |
| DTO (UserDto)         | ✅ EXISTS        | `dtos/UserDto.java`           |
| DTO (RoleDto)         | ✅ EXISTS        | `dtos/RoleDto.java`           |
| Repository            | ✅ EXISTS        | `repositories/UserRepository.java` |
| Service Interface     | ✅ EXISTS        | `services/UserService.java`   |
| Service Implementation| ⚠️ STUBS ONLY   | `services/UserServiceImpl.java` |
| Controller            | ❌ MISSING       | `controllers/UserController.java` |
| Security Config       | ❌ MISSING       | `config/SecurityConfig.java`  |
| Exception Handler     | ❌ MISSING       | `exceptions/GlobalExceptionHandler.java` |
| Validation            | ❌ NOT APPLIED   | Annotations on DTOs           |
| DTO-Entity Mapper     | ❌ MISSING       | Manual or use MapStruct       |
| JWT/OAuth             | ❌ MISSING       | Future implementation         |

---

# Mental Model

Think of the entire request lifecycle as a relay race:

```text
Runner 1 (Frontend):    Starts the race, passes the baton
Runner 2 (Network):     Carries baton across the track
Runner 3 (Tomcat):      Receives baton, hands to security
Runner 4 (Security):    Checks if runner is authorized
Runner 5 (Dispatcher):  Directs to the right lane
Runner 6 (Controller):  Takes baton, calls the next runner
Runner 7 (Service):     Does the actual work
Runner 8 (Repository):  Gets data from storage
Runner 9 (Hibernate):   Translates between Java and SQL
Runner 10 (HikariCP):   Provides fast road to database
Runner 11 (MySQL):      Stores/retrieves the data
```

Then the baton travels back:

```text
MySQL → HikariCP → Hibernate → Repository → Service → Controller → Dispatcher → Tomcat → Network → Frontend
```

And the user sees the result.

---

# Interview Tip

> "Explain what happens when a user hits a REST API."

Answer using this structure:

```text
1. Client sends HTTP request
2. DNS resolves, TCP handshake happens
3. Tomcat receives request
4. Security filter chain processes authentication/authorization
5. DispatcherServlet routes to correct controller
6. Controller delegates to service layer
7. Service applies business logic
8. Repository interacts with database via Hibernate
9. HikariCP manages database connections
10. Database executes query
11. Response travels back through each layer
12. Jackson converts to JSON
13. Client receives and renders response
```

This shows you understand the FULL picture, not just "Controller calls Service."

---

> "Understanding the complete request lifecycle is what separates a junior developer from a senior developer. A junior knows the code. A senior knows the flow."
