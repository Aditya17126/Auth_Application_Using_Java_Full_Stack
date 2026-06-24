# Section 3: File-by-File Explanation of the Auth Application

Every file in a Spring Boot project has a specific job.

Think of it like a restaurant:

```text
Chef          → Business Logic (Services)
Menu          → DTOs (what the customer sees)
Recipe Book   → Entities (what gets stored)
Storage Room  → Repositories (data access)
Manager       → Controller (handles requests)
Building Plan → Configuration Files (how everything is set up)
```

This document explains EVERY file in our authentication application.

We will cover:

```text
1.  AuthAppApplication.java         → The Starting Point
2.  dtos/RoleDto.java               → Role Data Carrier
3.  dtos/UserDto.java               → User Data Carrier
4.  entities/Provider.java          → Login Provider List
5.  entities/Role.java              → Role Database Object
6.  entities/User.java              → User Database Object
7.  repositories/UserRepository.java → Database Access
8.  services/UserService.java       → Business Rules Contract
9.  services/UserServiceImpl.java   → Business Rules Implementation
10. AuthAppApplicationTests.java    → Testing
11. application.yaml                → Main Configuration
12. application-dev.yml             → Development Settings
13. application-prod.yml            → Production Settings
14. application-qa.yml              → QA Settings
15. pom.xml                         → Project Dependencies
16. .gitignore                      → Git File Exclusions
17. .gitattributes                  → Git Line Endings
18. maven-wrapper.properties        → Maven Wrapper Config
```

---

# How All Files Connect

Before diving into individual files, understand the big picture:

```text
User sends HTTP Request
        ↓
[Controller] (not yet created)
        ↓
[UserService Interface]
        ↓
[UserServiceImpl] ← implements UserService
        ↓
[UserRepository] ← talks to database
        ↓
[User Entity] ← maps to "users" table
        ↓
[Role Entity] ← maps to "roles" table
        ↓
MySQL Database
```

Data flows through layers:

```text
UserDto ←→ User Entity ←→ Database Row
```

DTOs carry data between layers.
Entities carry data to/from the database.

---

# File Categories

Every file belongs to a category:

| File | Category |
|------|----------|
| AuthAppApplication.java | Infrastructure |
| RoleDto.java | Data Transfer |
| UserDto.java | Data Transfer |
| Provider.java | Domain Model |
| Role.java | Data Access (Entity) |
| User.java | Data Access (Entity) |
| UserRepository.java | Data Access (Repository) |
| UserService.java | Business Logic (Contract) |
| UserServiceImpl.java | Business Logic (Implementation) |
| AuthAppApplicationTests.java | Testing |
| application.yaml | Configuration |
| application-dev.yml | Configuration |
| application-prod.yml | Configuration |
| application-qa.yml | Configuration |
| pom.xml | Build / Infrastructure |
| .gitignore | Version Control |
| .gitattributes | Version Control |
| maven-wrapper.properties | Build / Infrastructure |

---

---

# 1. AuthAppApplication.java

## Full Code

```java
package com.substring.auth.auth_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthAppApplication.class, args);
    }
}
```

---

## Why Does This File Exist?

Every Java application needs a starting point.

Every Spring Boot application needs exactly one class annotated with:

```java
@SpringBootApplication
```

This is the entry gate.

Analogy:

```text
Think of a car.
You need a key to start the engine.

AuthAppApplication.java IS that key.
Without it, the engine (Spring Boot) does not start.
```

---

## Category

```text
Infrastructure
```

This file is not business logic.
This file is not data access.
This file is the INFRASTRUCTURE that boots everything else.

---

## Line-by-Line Breakdown

### Package Declaration

```java
package com.substring.auth.auth_app;
```

Tells Java:

```text
This class lives in the folder:
com → substring → auth → auth_app
```

Why does this matter?

```text
Spring Boot scans THIS package and ALL sub-packages
for components, services, repositories, and entities.
```

If your Service is in `com.substring.auth.auth_app.services`:

```text
com.substring.auth.auth_app          ← main class here
com.substring.auth.auth_app.services ← sub-package → SCANNED ✓
```

If your Service is in `com.different.package`:

```text
com.different.package ← NOT a sub-package → NOT SCANNED ✗
```

This is the #1 mistake beginners make:

```text
Putting classes in a package that is NOT
under the main application package.

Result: Spring Boot cannot find them.
Error: "No qualifying bean of type..."
```

---

### Imports

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```

Two imports:

| Import | Purpose |
|--------|---------|
| SpringApplication | Contains the `.run()` method that starts everything |
| @SpringBootApplication | The annotation that enables auto-configuration |

---

### @SpringBootApplication

```java
@SpringBootApplication
```

This single annotation is actually THREE annotations combined:

```text
@SpringBootApplication
    ├── @SpringBootConfiguration
    │       └── Marks this class as a configuration source
    ├── @EnableAutoConfiguration
    │       └── Tells Spring Boot to auto-configure beans
    │           based on dependencies in pom.xml
    └── @ComponentScan
            └── Scans current package + sub-packages
                for @Component, @Service, @Repository, @Controller
```

What does auto-configuration mean?

```text
You added spring-boot-starter-data-jpa in pom.xml.
Spring Boot sees this.
Spring Boot automatically configures:
    → DataSource
    → EntityManagerFactory
    → TransactionManager

You did NOT write any of that code.
Spring Boot did it FOR you.
```

Without @SpringBootApplication:

```text
You would have to:
1. Manually create DataSource beans
2. Manually create EntityManagerFactory
3. Manually scan for components
4. Manually configure everything

That would be 200+ lines of XML or Java config.
```

---

### The main Method

```java
public static void main(String[] args) {
    SpringApplication.run(AuthAppApplication.class, args);
}
```

This is standard Java:

```text
Every Java application starts at main().
```

`SpringApplication.run()` does the following:

```text
SpringApplication.run()
        ↓
1. Creates the Spring ApplicationContext
        ↓
2. Scans for all @Component, @Service, @Repository, @Entity
        ↓
3. Auto-configures based on pom.xml dependencies
        ↓
4. Connects to MySQL using application-dev.yml
        ↓
5. Hibernate creates/updates tables
        ↓
6. Starts embedded Tomcat server on port 8083
        ↓
7. Application is READY
```

The `args` parameter:

```text
Command-line arguments.
Example:
java -jar app.jar --server.port=9090

args would contain: ["--server.port=9090"]
```

---

## What Communicates With This File?

```text
This file → triggers → ALL other files
Nothing calls this file directly.
This is the ROOT.
```

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only job: start the application |
| Open/Closed | ✅ Followed | No reason to modify this file |
| Liskov Substitution | N/A | No inheritance |
| Interface Segregation | N/A | No interfaces |
| Dependency Inversion | ✅ Followed | Relies on Spring framework abstractions |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Putting this class in a deeply nested package | Component scanning misses sibling packages |
| Adding business logic in this file | Violates Single Responsibility |
| Creating multiple @SpringBootApplication classes | Confuses Spring Boot |
| Forgetting `static` on main | JVM cannot start the application |

---

## Interview Tip

> "What does @SpringBootApplication do?"

Answer:

```text
It is a convenience annotation that combines:
1. @SpringBootConfiguration — marks this as a config class
2. @EnableAutoConfiguration — auto-configures beans from classpath
3. @ComponentScan — scans current + sub-packages for Spring beans

It is the entry point annotation for any Spring Boot application.
```

---

---

# 2. dtos/RoleDto.java

## Full Code

```java
package com.substring.auth.auth_app.dtos;

import lombok.*;
import java.util.UUID;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class RoleDto {
    private UUID id = UUID.randomUUID();
    private String name;
}
```

---

## Why Does This File Exist?

Question:

> Should we expose our database Entity directly to the outside world?

Answer:

```text
NO.
```

Why?

```text
Entity (Role.java)
    → Contains JPA annotations (@Entity, @Table, @Id)
    → Contains database-specific details
    → Contains internal structure

If you send Entity directly:
    → You expose database column names
    → You expose internal IDs
    → You create tight coupling between API and database
```

Solution:

```text
Create a separate "carrier" object.
This carrier:
    → Contains only what the outside world needs to see
    → Has no database annotations
    → Can change independently of the Entity
```

This carrier is called a DTO:

```text
DTO = Data Transfer Object
```

Analogy:

```text
Entity = Your passport (contains sensitive info)
DTO    = Your business card (contains only what you want to share)

You don't hand your passport to everyone.
You give them your business card.
```

---

## Category

```text
Data Transfer
```

---

## Field-by-Field Breakdown

### id

```java
private UUID id = UUID.randomUUID();
```

Purpose:

```text
Unique identifier for the role.
```

Why `UUID.randomUUID()` as default?

```text
When creating a new RoleDto,
if no ID is provided, one is generated automatically.
```

This is different from the Role entity where the database generates the ID.
In the DTO, we may pre-generate an ID before sending data to the service layer.

---

### name

```java
private String name;
```

Purpose:

```text
The role name.
Examples: "ADMIN", "USER", "MANAGER"
```

---

## Annotations Breakdown

### @Getter @Setter

```text
Lombok generates all getter and setter methods.
```

Generated code:

```java
public UUID getId() { return id; }
public void setId(UUID id) { this.id = id; }
public String getName() { return name; }
public void setName(String name) { this.name = name; }
```

---

### @AllArgsConstructor

```text
Generates a constructor with ALL fields.
```

Generated code:

```java
public RoleDto(UUID id, String name) {
    this.id = id;
    this.name = name;
}
```

---

### @NoArgsConstructor

```text
Generates an empty constructor.
```

Generated code:

```java
public RoleDto() {
}
```

Why is this needed?

```text
JSON deserialization (Jackson) requires
a no-argument constructor to create objects.

When Spring receives JSON from a client:
1. Jackson calls new RoleDto()
2. Then sets fields using setters

Without @NoArgsConstructor:
    → Jackson cannot create the object
    → Error: "Cannot construct instance of RoleDto"
```

---

### @Builder

```text
Generates the Builder pattern.
```

Usage:

```java
RoleDto role = RoleDto.builder()
    .name("ADMIN")
    .build();
```

Why Builder?

```text
Without Builder:
    new RoleDto(UUID.randomUUID(), "ADMIN")

With Builder:
    RoleDto.builder().name("ADMIN").build()

Builder is more readable when you have many fields.
```

---

## What Communicates With This File?

```text
UserService     → Uses RoleDto to receive/return role data
Controller      → Receives RoleDto from HTTP requests (future)
UserDto         → Contains Set<Role> (note: not Set<RoleDto>)
```

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only carries role data |
| Open/Closed | ✅ Followed | Can extend without modifying |
| Liskov Substitution | N/A | No inheritance |
| Interface Segregation | N/A | No interfaces |
| Dependency Inversion | ✅ Followed | No dependencies on concrete classes |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Adding JPA annotations to DTOs | DTOs should NOT be entities |
| Using DTO as Entity directly | Tight coupling between API and database |
| Forgetting @NoArgsConstructor | JSON deserialization fails |
| Not creating DTOs at all | Exposing internal structure |

---

## Potential Improvement

Currently `UserDto` references `Role` (the entity) instead of `RoleDto`:

```java
// In UserDto.java
private Set<Role> roles = new HashSet<>();
```

Better design:

```java
private Set<RoleDto> roles = new HashSet<>();
```

This would fully decouple the DTO layer from the Entity layer.

---

---

# 3. dtos/UserDto.java

## Full Code

```java
package com.substring.auth.auth_app.dtos;

import com.substring.auth.auth_app.entities.Provider;
import com.substring.auth.auth_app.entities.Role;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class UserDto {
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
}
```

---

## Why Does This File Exist?

Same reason as RoleDto.

```text
UserDto is the "business card" for User data.
```

It carries user information between:

```text
Controller ←→ Service ←→ Response
```

Without exposing the internal Entity structure.

---

## Category

```text
Data Transfer
```

---

## Field-by-Field Breakdown

### id

```java
private UUID id;
```

Note: Unlike RoleDto, this does NOT have a default value.

```text
Why?
Because the database generates the User ID
using @GeneratedValue in the Entity.

The DTO receives the ID AFTER the entity is saved.
```

---

### email

```java
private String email;
```

Purpose:

```text
User's email address.
Used for login and communication.
```

---

### name

```java
private String name;
```

Purpose:

```text
User's display name.
```

---

### password

```java
private String password;
```

Purpose:

```text
User's password.
```

Important consideration:

```text
This DTO carries the password from the client to the server.
BUT when RETURNING data to the client,
you should NEVER include the password.

Currently this DTO is used for BOTH input and output.
A better design would be:

CreateUserRequest  → contains password (input)
UserResponse       → does NOT contain password (output)
```

This is called:

```text
Request/Response DTO Pattern
```

---

### image

```java
private String image;
```

Purpose:

```text
URL or path to the user's profile image.
```

---

### enable

```java
private boolean enable = true;
```

Purpose:

```text
Whether the account is active.
Default: true (new accounts are active).
```

---

### createdAt and updatedAt

```java
private Instant createdAt = Instant.now();
private Instant updatedAt = Instant.now();
```

Purpose:

```text
Audit timestamps.
When was the account created?
When was it last modified?
```

Why Instant?

```text
Instant represents a point in time (UTC).
It is timezone-independent.
It is the recommended type for timestamps in modern Java.
```

---

### provider

```java
private Provider provider = Provider.LOCAL;
```

Purpose:

```text
How did the user register?
LOCAL = email + password
GOOGLE = Google OAuth
GITHUB = GitHub OAuth
FACEBOOK = Facebook OAuth
```

Note: This references the `Provider` enum from the entities package.

---

### roles

```java
private Set<Role> roles = new HashSet<>();
```

Purpose:

```text
What permissions does this user have?
```

Design concern:

```text
This references Role (the Entity), not RoleDto.
This creates coupling between DTO and Entity layers.
```

---

## Annotations

Same as RoleDto:

```text
@Getter @Setter         → Auto-generate getters/setters
@AllArgsConstructor      → Constructor with all fields
@NoArgsConstructor       → Empty constructor (for Jackson)
@Builder                 → Builder pattern
```

---

## What Communicates With This File?

```text
UserService         → Returns UserDto from methods
UserServiceImpl     → Creates and returns UserDto
Controller (future) → Receives UserDto in request body
Controller (future) → Returns UserDto in response body
```

Flow:

```text
Client sends JSON
        ↓
Spring/Jackson converts JSON → UserDto
        ↓
Controller receives UserDto
        ↓
Service processes UserDto
        ↓
Service converts UserDto → User Entity
        ↓
Repository saves User Entity to database
        ↓
Service converts saved User → UserDto
        ↓
Controller returns UserDto as JSON
```

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ⚠️ Partial | Used for both input and output — could be split |
| Open/Closed | ✅ Followed | Can extend without modifying |
| Dependency Inversion | ⚠️ Violated | Directly depends on Role entity |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Returning password in response DTO | Security vulnerability |
| Using single DTO for input + output | Over-exposing data |
| Referencing Entity classes in DTOs | Tight coupling between layers |
| Not initializing collections | NullPointerException when accessing roles |

---

---

# 4. entities/Provider.java

## Full Code

```java
package com.substring.auth.auth_app.entities;

public enum Provider {
    LOCAL, GOOGLE, GITHUB, FACEBOOK
}
```

---

## Why Does This File Exist?

Question:

> How did the user sign up?

Possible answers:

```text
1. Email + Password     → LOCAL
2. Google OAuth         → GOOGLE
3. GitHub OAuth         → GITHUB
4. Facebook OAuth       → FACEBOOK
```

We need a way to store this in Java.

We could use a String:

```java
private String provider = "LOCAL";
```

Problem:

```text
Someone could write:
provider = "local"
provider = "Local"
provider = "LOCALL"  ← typo!
provider = "anything"

No compile-time safety.
```

Solution: Use an Enum.

```java
public enum Provider {
    LOCAL, GOOGLE, GITHUB, FACEBOOK
}
```

Now:

```text
provider = Provider.LOCAL     ✓ Valid
provider = Provider.GOOGLE    ✓ Valid
provider = "LOCALL"           ✗ Compile Error!
```

Analogy:

```text
String  = Writing anything on a blank paper
Enum    = Choosing from a dropdown menu

Dropdown menus prevent typos.
```

---

## Category

```text
Domain Model
```

This file defines the business vocabulary.
It is not a database entity.
It is not infrastructure.
It is a VALUE that the business domain cares about.

---

## Enum Values Explained

| Value | Meaning | When Used |
|-------|---------|-----------|
| LOCAL | User registered with email + password | Default signup |
| GOOGLE | User signed in via Google | OAuth2 Google login |
| GITHUB | User signed in via GitHub | OAuth2 GitHub login |
| FACEBOOK | User signed in via Facebook | OAuth2 Facebook login |

---

## What Communicates With This File?

```text
User.java    → Uses Provider as a field
UserDto.java → Uses Provider as a field
Hibernate    → Stores Provider as a STRING in the database
                (because of @Enumerated(EnumType.STRING) in User.java)
```

---

## How Hibernate Stores This

In User.java:

```java
@Enumerated(EnumType.STRING)
private Provider provider = Provider.LOCAL;
```

Database column stores:

| provider |
|----------|
| LOCAL    |
| GOOGLE   |
| GITHUB   |

As actual strings, not numbers.

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only defines provider types |
| Open/Closed | ⚠️ Watch | Adding a new provider requires modifying this enum |

To add a new provider like APPLE:

```java
public enum Provider {
    LOCAL, GOOGLE, GITHUB, FACEBOOK, APPLE
}
```

This modifies existing code, which technically violates Open/Closed.
But enums are an accepted exception because the values are finite and known.

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Using String instead of Enum | Typos, no type safety |
| Using @Enumerated(EnumType.ORDINAL) | Reordering enum breaks data |
| Not providing a default value | Null provider in database |
| Putting enum in DTO package | Enum is domain logic, not data transfer |

---

---

# 5. entities/Role.java

## Full Code

```java
package com.substring.auth.auth_app.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @AllArgsConstructor @Builder @NoArgsConstructor
@Entity @Table(name = "roles")
public class Role {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(unique = true, nullable = false)
    private String name;
}
```

---

## Why Does This File Exist?

Question:

> Where do we store role information in the database?

Answer:

```text
In a table called "roles".
```

This Java class represents that table.

```text
Role.java Object  ←→  roles Table in MySQL
```

---

## Category

```text
Data Access (Entity)
```

---

## Annotations Breakdown

### @Entity

```java
@Entity
```

Tells Hibernate:

```text
"This class maps to a database table."
```

Without @Entity:

```text
Hibernate ignores this class completely.
No table is created.
```

---

### @Table(name = "roles")

```java
@Table(name = "roles")
```

Tells Hibernate:

```text
"The table name should be 'roles'."
```

Without this:

```text
Hibernate would create a table named "role" (class name, lowercased).
```

With this:

```text
Table name = "roles"
```

---

### @Id

```java
@Id
private UUID id = UUID.randomUUID();
```

Tells JPA:

```text
"This field is the Primary Key."
```

Every database table needs a primary key.
The primary key uniquely identifies each row.

Notice:

```text
No @GeneratedValue annotation here!
```

This means:

```text
The ID is NOT auto-generated by the database.
It is set manually in Java using UUID.randomUUID().
```

Compare with User.java:

```java
// User.java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

User uses database-generated IDs.
Role uses Java-generated IDs.

Why the difference?

```text
Roles are typically predefined:
    ADMIN, USER, MANAGER

They are created once, not frequently.
Manual ID generation is fine for this use case.
```

---

### @Column(unique = true, nullable = false)

```java
@Column(unique = true, nullable = false)
private String name;
```

Two constraints:

| Attribute | Meaning |
|-----------|---------|
| unique = true | No two roles can have the same name |
| nullable = false | Every role MUST have a name |

Database effect:

```sql
CREATE TABLE roles (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);
```

What happens if you violate these?

```text
Trying to insert duplicate "ADMIN":
    → DataIntegrityViolationException

Trying to insert null name:
    → ConstraintViolationException
```

---

## Lombok Annotations

```text
@Getter          → getters for id, name
@Setter          → setters for id, name
@AllArgsConstructor → Role(UUID id, String name)
@NoArgsConstructor  → Role()
@Builder          → Role.builder().name("ADMIN").build()
```

---

## What Communicates With This File?

```text
User.java           → Has @ManyToMany relationship with Role
UserRepository      → Fetches Users who have Roles (via EAGER loading)
Hibernate           → Creates/manages the "roles" table
user_roles table    → Join table connecting users ↔ roles
```

---

## Database Table Created

```text
roles
├── id    (UUID, Primary Key)
└── name  (VARCHAR, Unique, Not Null)
```

Example data:

| id | name |
|----|------|
| 550e8400-... | ADMIN |
| 6ba7b810-... | USER |

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only represents a role |
| Open/Closed | ✅ Followed | Structure rarely changes |
| Dependency Inversion | ✅ Followed | No concrete dependencies |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Forgetting @Entity | Hibernate does not create the table |
| Naming table "user_roles" instead of "roles" | Confuses with the join table |
| Not adding unique constraint on name | Duplicate roles in database |
| Using Long instead of UUID for id | Less secure, predictable IDs |
| Forgetting @NoArgsConstructor | Hibernate cannot instantiate the entity |

---

---

# 6. entities/User.java

## Full Code

```java
package com.substring.auth.auth_app.entities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
@Entity @Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
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
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

---

## Why Does This File Exist?

This is the MOST important entity in an authentication application.

```text
Everything revolves around the User.
Authentication = Who is the user?
Authorization  = What can the user do?
```

This class maps to the `users` table in MySQL.

---

## Category

```text
Data Access (Entity)
```

---

## Annotations Breakdown (Detailed)

### @Entity @Table(name="users")

```text
@Entity  → "This is a database table."
@Table   → "The table is called 'users'."
```

---

### @Id

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(name = "user_id")
private UUID id;
```

Three annotations on one field:

| Annotation | Purpose |
|------------|---------|
| @Id | This is the primary key |
| @GeneratedValue(strategy = GenerationType.UUID) | Database auto-generates UUID |
| @Column(name = "user_id") | Column name is "user_id", not "id" |

Why @GeneratedValue here but not in Role?

```text
Users are created dynamically (user signups).
Hundreds or thousands of users.
Let the database handle ID generation.

Roles are predefined (ADMIN, USER).
Only a few roles exist.
Manual ID generation is fine.
```

---

### @Column on email

```java
@Column(name = "user_email", unique = true, length = 300)
private String email;
```

| Attribute | Meaning |
|-----------|---------|
| name = "user_email" | Column is named "user_email" in database |
| unique = true | No two users can have the same email |
| length = 300 | Maximum 300 characters |

Why 300?

```text
Standard email max length is 254 characters (RFC 5321).
300 gives a safety margin.
```

---

### Fields Without @Column

```java
private String name;
private String password;
private String image;
private boolean enable = true;
private Instant createdAt = Instant.now();
private Instant updatedAt = Instant.now();
```

When @Column is NOT specified:

```text
Hibernate uses the field name as the column name.
    name     → "name" column
    password → "password" column
    image    → "image" column
    enable   → "enable" column
```

Default values:

```text
enable    = true     → New accounts are active
createdAt = now      → Set to current time
updatedAt = now      → Set to current time
```

---

### @Enumerated(EnumType.STRING)

```java
@Enumerated(EnumType.STRING)
private Provider provider = Provider.LOCAL;
```

Tells Hibernate:

```text
Store the enum as its STRING name.
    Provider.LOCAL  → stores "LOCAL"
    Provider.GOOGLE → stores "GOOGLE"
```

NOT as ordinal:

```text
Provider.LOCAL  → would store 0
Provider.GOOGLE → would store 1
(DANGEROUS if you reorder the enum!)
```

---

### @ManyToMany Relationship

```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles = new HashSet<>();
```

This is the most complex part. Let's break it down:

#### @ManyToMany

```text
One User  → can have MANY Roles  (USER, ADMIN)
One Role  → can belong to MANY Users

This is a Many-to-Many relationship.
```

#### fetch = FetchType.EAGER

```text
EAGER = Load roles IMMEDIATELY when loading the user.
LAZY  = Load roles only when you ACCESS user.getRoles().

EAGER:
    Load User → Roles are loaded too (1 query or join)

LAZY:
    Load User → Roles NOT loaded
    Call getRoles() → THEN roles are loaded (separate query)
```

For authentication, EAGER makes sense:

```text
When a user logs in, you ALWAYS need their roles
to check permissions. So load them immediately.
```

#### @JoinTable

```text
Databases cannot store Many-to-Many directly.
They need a third "join" table.
```

```text
users table          user_roles table       roles table
+---------+         +---------+---------+   +---------+
| user_id |  ←───── | user_id | role_id | ─────→ | role_id |
+---------+         +---------+---------+   +---------+
```

| Attribute | Meaning |
|-----------|---------|
| name = "user_roles" | The join table is called "user_roles" |
| joinColumns = @JoinColumn(name = "user_id") | Column in join table pointing to User |
| inverseJoinColumns = @JoinColumn(name = "role_id") | Column in join table pointing to Role |

Example data:

users:

| user_id | email |
|---------|-------|
| AAA | aditya@gmail.com |
| BBB | rahul@gmail.com |

roles:

| role_id | name |
|---------|------|
| 111 | USER |
| 222 | ADMIN |

user_roles:

| user_id | role_id |
|---------|---------|
| AAA | 111 |
| AAA | 222 |
| BBB | 111 |

Reading this:

```text
Aditya (AAA) has roles: USER (111) and ADMIN (222)
Rahul (BBB) has role: USER (111)
```

---

### @PrePersist

```java
@PrePersist
protected void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
}
```

This is a JPA Lifecycle Callback.

```text
"Before saving this entity for the FIRST TIME, run this method."
```

What it does:

```text
1. Gets the current time
2. If createdAt is null, set it to now
3. Always set updatedAt to now
```

Why check `if (createdAt == null)`?

```text
Because the field already has a default value:
    private Instant createdAt = Instant.now();

If the object was created in Java and held for a while
before saving, createdAt would already be set.
The null check is a safety net.
```

---

### @PreUpdate

```java
@PreUpdate
protected void onUpdate() {
    updatedAt = Instant.now();
}
```

```text
"Before UPDATING this entity in the database, run this method."
```

What it does:

```text
Sets updatedAt to the current time.
Every time the user is modified, this timestamp changes.
```

Use case:

```text
User changes their name.
    → updatedAt automatically changes.
    → You can see WHEN the last modification happened.
```

---

## Complete Database Schema Created

Hibernate creates THREE tables from this entity:

```text
1. users table      ← from User.java @Entity
2. roles table      ← from Role.java @Entity
3. user_roles table ← from @JoinTable in User.java
```

users table:

```text
+-----------+--------------+----------+----------+-------+--------+------------+------------+----------+
| user_id   | user_email   | name     | password | image | enable | created_at | updated_at | provider |
+-----------+--------------+----------+----------+-------+--------+------------+------------+----------+
| UUID      | VARCHAR(300) | VARCHAR  | VARCHAR  | VARCHAR| BIT   | TIMESTAMP  | TIMESTAMP  | VARCHAR  |
+-----------+--------------+----------+----------+-------+--------+------------+------------+----------+
```

---

## What Communicates With This File?

```text
UserRepository    → Queries/saves User entities
UserServiceImpl   → Creates/modifies User objects
Hibernate         → Maps User ↔ users table
Role.java         → Connected via @ManyToMany
Provider.java     → Used as enum field
```

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only represents user data |
| Open/Closed | ✅ Followed | Can add fields without changing logic |
| Dependency Inversion | ✅ Followed | Depends on JPA abstractions |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Using FetchType.LAZY without proper session management | LazyInitializationException |
| Forgetting @PrePersist/@PreUpdate | Timestamps not auto-updated |
| Not using @Enumerated(EnumType.STRING) | Data corruption on enum reorder |
| Storing plain-text passwords | Major security vulnerability |
| Forgetting @NoArgsConstructor | Hibernate cannot create proxy objects |
| Using List instead of Set for @ManyToMany | Hibernate generates inefficient queries |

---

---

# 7. repositories/UserRepository.java

## Full Code

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

---

## Why Does This File Exist?

Question:

> How does Java talk to the database?

Traditional approach:

```java
Connection conn = DriverManager.getConnection(url);
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
ps.setString(1, email);
ResultSet rs = ps.executeQuery();
// ... 20 more lines to map results
```

Spring Data JPA approach:

```java
Optional<User> findByEmail(String email);
```

One line. That's it.

Analogy:

```text
Traditional = You cook every meal from scratch
Spring Data JPA = You order from a menu

You say WHAT you want.
Spring Data JPA figures out HOW to get it.
```

---

## Category

```text
Data Access (Repository)
```

---

## The Interface Breakdown

### extends JpaRepository<User, UUID>

```java
public interface UserRepository extends JpaRepository<User, UUID>
```

Two type parameters:

| Parameter | Meaning |
|-----------|---------|
| User | The Entity this repository manages |
| UUID | The type of the Entity's primary key |

What does JpaRepository give you for FREE?

```text
save(User user)          → INSERT or UPDATE
findById(UUID id)        → SELECT by ID
findAll()                → SELECT all users
deleteById(UUID id)      → DELETE by ID
count()                  → COUNT all users
existsById(UUID id)      → Check if user exists
flush()                  → Force pending changes to DB
saveAll(List<User>)      → Batch save
deleteAll()              → Delete everything
```

You wrote ZERO lines of SQL.
You wrote ZERO implementation code.

How?

```text
Spring Data JPA
    ↓
At runtime, creates a PROXY class
that implements UserRepository
    ↓
This proxy generates SQL queries
based on method names
    ↓
Executes queries using EntityManager
    ↓
Returns results
```

---

### Why Is It an Interface, Not a Class?

```text
You do NOT write the implementation.
Spring Data JPA writes it FOR you at runtime.

You define WHAT you want (method signature).
Spring defines HOW to do it (implementation).
```

This is the Repository Pattern:

```text
Service Layer
    ↓
    "I need a user by email"
    ↓
Repository Interface
    ↓
    "I know how to get that" (auto-generated)
    ↓
Database
```

---

### Custom Query Methods

#### findByEmail

```java
Optional<User> findByEmail(String email);
```

Spring Data JPA reads the method name:

```text
find   → SELECT
By     → WHERE
Email  → email column
```

Generated SQL:

```sql
SELECT * FROM users WHERE user_email = ?
```

Why Optional?

```text
The user might not exist.

Without Optional:
    User user = findByEmail("xyz@gmail.com");
    // user could be null
    // user.getName() → NullPointerException!

With Optional:
    Optional<User> user = findByEmail("xyz@gmail.com");
    if (user.isPresent()) {
        // safe to use
    }
```

Optional forces you to handle the "not found" case:

```text
Optional = A box that may or may not contain a value.

Present:  [User]  → user exists
Empty:    [    ]  → user not found
```

---

#### existsByEmail

```java
boolean existsByEmail(String email);
```

Spring Data JPA reads:

```text
exists → SELECT COUNT(*) > 0
By     → WHERE
Email  → email column
```

Generated SQL:

```sql
SELECT COUNT(*) > 0 FROM users WHERE user_email = ?
```

Returns:

```text
true  → email exists in database
false → email does not exist
```

Use case:

```text
Before creating a new user:
    if (userRepository.existsByEmail(email)) {
        throw new RuntimeException("Email already taken!");
    }
```

---

## What Communicates With This File?

```text
UserServiceImpl → Calls repository methods to access database
Spring Boot     → Auto-detects this interface (via @ComponentScan)
                   and creates a proxy implementation
Hibernate       → Executes the generated SQL queries
User.java       → The entity being queried
```

---

## Spring Data JPA Query Method Naming Rules

| Method Name | Generated Query |
|-------------|----------------|
| findByName(String name) | WHERE name = ? |
| findByEmailAndName(String email, String name) | WHERE email = ? AND name = ? |
| findByEnableTrue() | WHERE enable = true |
| findByEnableFalse() | WHERE enable = false |
| findByNameContaining(String text) | WHERE name LIKE '%text%' |
| findByNameStartingWith(String prefix) | WHERE name LIKE 'prefix%' |
| countByProvider(Provider p) | SELECT COUNT(*) WHERE provider = ? |
| deleteByEmail(String email) | DELETE WHERE email = ? |

You just follow the naming convention.
Spring writes the SQL.

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only handles User data access |
| Open/Closed | ✅ Followed | Add new methods without modifying existing |
| Interface Segregation | ✅ Followed | Only user-related methods |
| Dependency Inversion | ✅ Followed | Service depends on interface, not implementation |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Making it a class instead of interface | Spring Data JPA cannot create proxy |
| Wrong method naming | Spring cannot parse the query, startup error |
| Returning User instead of Optional | NullPointerException risk |
| Forgetting to extend JpaRepository | No CRUD methods available |
| Writing manual SQL when method name query suffices | Unnecessary complexity |

---

---

# 8. services/UserService.java

## Full Code

```java
package com.substring.auth.auth_app.services;

import com.substring.auth.auth_app.dtos.UserDto;

public interface UserService {
    UserDto createUser(UserDto userDto);
    UserDto getUserByEmail(String email);
    UserDto updateUSer(UserDto userDto, String userId);
    void deleteUser(String userId);
    UserDto getUserById(String userId);
    Iterable<UserDto> getAllUsers();
}
```

---

## Why Does This File Exist?

Question:

> Why not write business logic directly in the Controller?

Answer:

```text
Separation of Concerns.
```

Analogy:

```text
Restaurant:
    Waiter (Controller)    → Takes the order
    Chef (Service)         → Cooks the food
    Storage (Repository)   → Gets ingredients

The waiter does NOT cook.
The chef does NOT serve tables.
Each has ONE job.
```

The UserService interface defines WHAT the business can do.
It does NOT define HOW.

```text
UserService = The MENU
    "We can create users"
    "We can get users by email"
    "We can update users"
    "We can delete users"

UserServiceImpl = The KITCHEN
    "Here's HOW we create users"
    "Here's HOW we get users by email"
```

---

## Category

```text
Business Logic (Contract)
```

---

## Method-by-Method Breakdown

### createUser

```java
UserDto createUser(UserDto userDto);
```

```text
Input:  UserDto (user data from client)
Output: UserDto (saved user data with generated ID)
```

Purpose:

```text
Register a new user.
```

---

### getUserByEmail

```java
UserDto getUserByEmail(String email);
```

```text
Input:  email address
Output: UserDto of the found user
```

Purpose:

```text
Find a user by their email.
Used during login.
```

---

### updateUSer

```java
UserDto updateUSer(UserDto userDto, String userId);
```

```text
Input:  Updated data + user ID
Output: Updated UserDto
```

Note: There is a TYPO in the method name:

```text
updateUSer  ← should be updateUser
```

This is a code smell. Method names should follow camelCase properly.

---

### deleteUser

```java
void deleteUser(String userId);
```

```text
Input:  user ID
Output: nothing (void)
```

Purpose:

```text
Remove a user from the system.
```

Note: userId is a String, but the entity uses UUID.
The implementation will need to convert:

```java
UUID.fromString(userId)
```

---

### getUserById

```java
UserDto getUserById(String userId);
```

```text
Input:  user ID as String
Output: UserDto
```

---

### getAllUsers

```java
Iterable<UserDto> getAllUsers();
```

```text
Input:  nothing
Output: All users as an Iterable of UserDto
```

Why Iterable instead of List?

```text
Iterable is the most generic type.
List, Set, and arrays all implement Iterable.
This gives the implementation freedom to return any collection.

However, most developers would use List<UserDto> for clarity.
```

---

## Why Use an Interface?

```text
1. Abstraction:
   Controller knows WHAT operations exist.
   Controller does NOT know HOW they work.

2. Testability:
   In tests, you can create a MOCK implementation.
   No need for a real database.

3. Swappability:
   Today: UserServiceImpl (MySQL)
   Tomorrow: UserServiceMongoImpl (MongoDB)
   Controller code does NOT change.
```

This follows Dependency Inversion:

```text
Controller → depends on → UserService (interface)
                               ↑
                    UserServiceImpl (implementation)

Controller does NOT depend on UserServiceImpl directly.
```

---

## What Communicates With This File?

```text
UserServiceImpl   → Implements this interface
Controller (future) → Calls methods on this interface
Spring IoC        → Injects UserServiceImpl when UserService is needed
```

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only defines user operations |
| Open/Closed | ✅ Followed | Add methods without changing implementations |
| Interface Segregation | ✅ Followed | Only user-related methods |
| Dependency Inversion | ✅ Followed | This IS the abstraction |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Not creating an interface | Tight coupling, hard to test |
| Putting too many methods | Fat interface, violates ISP |
| Using Entity as return type instead of DTO | Exposes database internals |
| Typos in method names (updateUSer) | Confusing API, hard to maintain |

---

---

# 9. services/UserServiceImpl.java

## Full Code

```java
package com.substring.auth.auth_app.services;

import com.substring.auth.auth_app.dtos.UserDto;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public UserDto createUser(UserDto userDto) { return null; }

    @Override
    public UserDto getUserByEmail(String email) { return null; }

    @Override
    public UserDto updateUSer(UserDto userDto, String userId) { return null; }

    @Override
    public void deleteUser(String userId) { }

    @Override
    public UserDto getUserById(String userId) { return null; }

    @Override
    public Iterable<UserDto> getAllUsers() { return null; }
}
```

---

## Why Does This File Exist?

The interface (UserService) says WHAT.
This class says HOW.

Currently, all methods return `null` or do nothing.
This is a SKELETON implementation — a placeholder.

```text
This is like a chef who has a recipe list (interface)
but hasn't cooked anything yet (implementation is empty).
```

---

## Category

```text
Business Logic (Implementation)
```

---

## @Service Annotation

```java
@Service
```

This does TWO things:

```text
1. Marks this class as a Spring Bean
   Spring will create ONE instance and manage it.

2. Semantic meaning
   Tells developers: "This is a service layer class."
```

Under the hood:

```text
@Service is a specialization of @Component.

@Component → "I am a Spring-managed bean."
@Service   → "I am a Spring-managed bean in the SERVICE layer."
@Repository → "I am a Spring-managed bean in the DATA ACCESS layer."
@Controller → "I am a Spring-managed bean in the PRESENTATION layer."
```

All four are functionally identical.
The difference is SEMANTIC — it tells humans which layer the class belongs to.

---

## implements UserService

```java
public class UserServiceImpl implements UserService
```

This means:

```text
UserServiceImpl MUST implement ALL methods
defined in UserService.

If you forget one method:
    → Compile Error
```

---

## @Override

```java
@Override
public UserDto createUser(UserDto userDto) { return null; }
```

@Override tells the compiler:

```text
"I am overriding a method from the parent interface.
If the method signature doesn't match, give me a COMPILE ERROR."
```

Without @Override:

```text
If you accidentally write:
    public UserDto creatUser(UserDto userDto)  ← typo!

Java would NOT warn you.
It would create a NEW method instead of overriding.
```

With @Override:

```text
Compiler checks: "Does creatUser exist in UserService?"
Answer: NO.
Result: COMPILE ERROR. Bug caught early!
```

---

## Why All Methods Return null?

```text
This is a stub implementation.
The developer has not yet written the actual logic.
```

What the real implementation SHOULD look like:

```java
@Override
public UserDto createUser(UserDto userDto) {
    // 1. Convert UserDto → User Entity
    // 2. Hash the password
    // 3. Save to database via UserRepository
    // 4. Convert saved User → UserDto
    // 5. Return UserDto
}
```

Returning null is DANGEROUS in production:

```text
Controller calls createUser()
    → Returns null
    → Controller tries to access user.getName()
    → NullPointerException!
```

---

## What's Missing?

This class needs:

### 1. UserRepository Injection

```java
private final UserRepository userRepository;

@Autowired
public UserServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

Or with Lombok:

```java
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
}
```

### 2. Entity-DTO Conversion

```java
// UserDto → User
private User convertToEntity(UserDto dto) {
    return User.builder()
        .email(dto.getEmail())
        .name(dto.getName())
        .password(dto.getPassword())
        .build();
}

// User → UserDto
private UserDto convertToDto(User user) {
    return UserDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .build();
}
```

### 3. Actual Business Logic

```java
@Override
public UserDto createUser(UserDto userDto) {
    User user = convertToEntity(userDto);
    User savedUser = userRepository.save(user);
    return convertToDto(savedUser);
}
```

---

## What Communicates With This File?

```text
UserService interface → This class implements it
UserRepository        → This class SHOULD use it (not yet injected)
Controller (future)   → Will call methods on this via UserService
Spring IoC Container  → Creates and manages this bean (@Service)
```

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only handles user business logic |
| Open/Closed | ✅ Followed | Can add new features by extending |
| Liskov Substitution | ✅ Followed | Can replace with any UserService implementation |
| Dependency Inversion | ⚠️ Partial | Missing repository injection |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Returning null from methods | NullPointerException in callers |
| Not injecting Repository | Cannot access database |
| Putting database queries directly in service | Should go through Repository |
| Not converting between DTO and Entity | Exposing internals or database errors |
| Forgetting @Service | Spring cannot find this bean, injection fails |

---

---

# 10. test/AuthAppApplicationTests.java

## Full Code

```java
package com.substring.auth.auth_app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthAppApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

---

## Why Does This File Exist?

Every Spring Boot project needs at least ONE test:

```text
"Does the application start without errors?"
```

This test answers that question.

---

## Category

```text
Testing
```

---

## Annotations Breakdown

### @SpringBootTest

```java
@SpringBootTest
```

This tells JUnit:

```text
"Start the ENTIRE Spring Boot application context for this test."
```

What happens:

```text
1. Loads all @Configuration classes
2. Creates all beans (@Service, @Repository, etc.)
3. Connects to the database (if configured)
4. Starts the full application context
```

This is an INTEGRATION test, not a unit test:

```text
Unit Test:
    Tests ONE class in isolation.
    Fast. No Spring context.

Integration Test:
    Tests multiple classes together.
    Slow. Requires Spring context.

@SpringBootTest = Integration Test
```

---

### @Test

```java
@Test
void contextLoads() {
}
```

@Test is from JUnit 5 (Jupiter).

```text
"This method is a test case."
```

The method is EMPTY.

Why?

```text
The test is:
    "Does the application context load without errors?"

If @SpringBootTest can start the context successfully:
    → Test PASSES

If there's a configuration error:
    → Test FAILS with an exception
```

Common failures:

```text
- Database connection error
- Missing required bean
- Circular dependency
- Invalid configuration property
```

---

## What Communicates With This File?

```text
JUnit 5         → Runs this test
@SpringBootTest → Loads the entire application context
Maven/Gradle    → Executes tests during build (mvn test)
CI/CD Pipeline  → Runs tests before deployment
```

---

## SOLID Principles

| Principle | Status | Explanation |
|-----------|--------|-------------|
| Single Responsibility | ✅ Followed | Only tests context loading |

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Deleting this test | No validation that the app starts |
| Adding business logic tests here | Mix of integration and unit tests |
| Not having a test database configured | Tests fail in CI/CD |
| Using @SpringBootTest for unit tests | Slow, unnecessary overhead |

---

---

# 11. application.yaml (Main Configuration)

## Full Content

```yaml
spring:
  application:
    name: auth-app
  profiles:
    active: dev

server:
  port: 8082

#configurations
#database configurations

#email configurations

#oauth2 configurations
```

---

## Why Does This File Exist?

Every Spring Boot application needs configuration:

```text
What is the app name?
Which profile is active?
What port should it run on?
```

This file answers those questions.

---

## Category

```text
Configuration
```

---

## Property-by-Property Breakdown

### spring.application.name

```yaml
spring:
  application:
    name: auth-app
```

Sets the application name.

Used in:

```text
- Logging (shows "auth-app" in logs)
- Spring Cloud (service discovery)
- Actuator endpoints
```

---

### spring.profiles.active

```yaml
spring:
  profiles:
    active: dev
```

This is CRITICAL.

```text
Spring Boot supports multiple profiles:
    dev   → Development
    qa    → Quality Assurance / Testing
    prod  → Production
```

Setting `active: dev` means:

```text
Spring Boot will ALSO load:
    application-dev.yml
```

This allows different settings per environment:

```text
Dev:  MySQL on localhost, debug SQL, port 8083
QA:   Test database, port 8081
Prod: Production database, port 8080
```

How profile loading works:

```text
1. Load application.yaml        ← ALWAYS loaded
2. active profile = dev
3. Load application-dev.yml     ← ALSO loaded
4. Dev settings OVERRIDE main settings
```

Port example:

```text
application.yaml:     port = 8082
application-dev.yml:  port = 8083

Result: port = 8083 (dev overrides main)
```

---

### server.port

```yaml
server:
  port: 8082
```

The port Tomcat listens on.

```text
This is the FALLBACK port.
If no profile overrides it, the app runs on 8082.
But since dev profile is active and sets 8083,
the actual port will be 8083.
```

---

### Comments (Placeholders)

```yaml
#configurations
#database configurations
#email configurations
#oauth2 configurations
```

These are placeholder comments.
They indicate future configuration sections:

```text
- Database settings     → Currently in application-dev.yml
- Email settings        → For password reset emails (future)
- OAuth2 settings       → For Google/GitHub login (future)
```

---

## What Communicates With This File?

```text
Spring Boot      → Reads this file at startup
Tomcat           → Uses server.port
Profile System   → Loads additional profile-specific files
All beans        → Can read properties using @Value
```

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Putting secrets (passwords) in application.yaml | Committed to Git, security risk |
| Using .properties instead of .yaml | Works, but YAML is more readable |
| Wrong indentation in YAML | Parse error, app won't start |
| Setting wrong active profile | Wrong database, wrong port |

---

---

# 12. application-dev.yml (Development Profile)

## Full Content

```yaml
#development version of configurations
server:
  port: 8083

# database connections
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_application_java
    username: root
    password: Aditya13325@

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
    show_sql: true
```

---

## Why Does This File Exist?

Development needs different settings than production:

```text
Dev:  Local MySQL, show SQL queries, auto-create tables
Prod: Cloud MySQL, hide SQL, never auto-create tables
```

This file contains ONLY development-specific settings.

---

## Category

```text
Configuration (Profile-specific)
```

---

## Property-by-Property Breakdown

### server.port

```yaml
server:
  port: 8083
```

```text
Development runs on port 8083.
This OVERRIDES the 8082 from application.yaml.
```

---

### Datasource Configuration

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_application_java
    username: root
    password: Aditya13325@
```

| Property | Value | Meaning |
|----------|-------|---------|
| url | jdbc:mysql://localhost:3306/auth_application_java | MySQL on localhost, port 3306, database name auth_application_java |
| username | root | MySQL username |
| password | Aditya13325@ | MySQL password |

URL breakdown:

```text
jdbc:mysql://  → Protocol (JDBC with MySQL driver)
localhost      → Database host (local machine)
:3306          → MySQL default port
/auth_application_java → Database name
```

Security concern:

```text
⚠️ The password is in plain text in this file.
If committed to Git, anyone can see it.

Better approach:
    Use environment variables:
    password: ${DB_PASSWORD}
```

---

### JPA / Hibernate Configuration

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
    show_sql: true
```

#### ddl-auto: update

```text
DDL = Data Definition Language (CREATE, ALTER, DROP)

Options:
    none     → Do nothing
    validate → Check if tables match entities (don't change)
    update   → Create/modify tables to match entities
    create   → Drop and recreate tables every time
    create-drop → Create on start, drop on shutdown
```

`update` means:

```text
When app starts:
    1. Hibernate checks existing tables
    2. Compares with @Entity classes
    3. ADDS missing columns/tables
    4. Does NOT delete existing data

Example:
    You add a new field "phone" to User.
    Hibernate adds a "phone" column to the users table.
    Existing data remains intact.
```

For development: `update` is fine.
For production: Use `validate` or `none` with manual migrations.

---

#### dialect: org.hibernate.dialect.MySQLDialect

```text
Tells Hibernate which SQL dialect to generate.

MySQL:  LIMIT 10
Oracle: ROWNUM <= 10

Hibernate needs to know which syntax to use.
```

---

#### format_sql: true

```text
Pretty-prints SQL in the console.

Without:
    SELECT user0_.user_id AS user_id1_1_, ...

With:
    SELECT
        user0_.user_id AS user_id1_1_,
        user0_.name AS name2_1_,
        ...
```

---

#### show_sql: true

```text
Prints all SQL queries to the console.

Useful for debugging.
BAD for production (clutters logs, performance impact).
```

---

### Commented-out HikariCP Configuration

```yaml
# hikari:
#   pool-name: HikariCP
#   maximum-pool-size: 5
#   connection-timeout: 10000
#   idle-timeout: 600000
#   max-lifetime: 1800000
```

HikariCP is the default connection pool in Spring Boot.

```text
Connection Pool = A set of reusable database connections.

Without pool:
    Every request opens a new connection → slow!

With pool:
    Connections are kept open and reused → fast!
```

These are commented out because defaults work fine for development.

---

## What Communicates With This File?

```text
Spring Boot       → Loads when profile is "dev"
HikariCP          → Uses datasource properties for connection pool
Hibernate         → Uses JPA properties for ORM behavior
MySQL Driver      → Uses URL, username, password to connect
```

---

## Common Mistakes

| Mistake | Result |
|---------|--------|
| Using ddl-auto: update in production | Accidental schema changes |
| Leaving show_sql: true in production | Performance degradation |
| Hardcoding passwords | Security vulnerability |
| Wrong database URL | Connection refused error |
| Wrong dialect | Incorrect SQL generated |

---

---

# 13. application-prod.yml (Production Profile)

## Full Content

```yaml
server:
  port: 8080
```

---

## Why Does This File Exist?

Production needs its own settings.

Currently it only sets the port.

```text
Production runs on port 8080 (standard HTTP port).
```

---

## Category

```text
Configuration (Profile-specific)
```

---

## What's Missing?

A real production file would include:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show_sql: false
```

Key differences from dev:

| Setting | Dev | Prod |
|---------|-----|------|
| Port | 8083 | 8080 |
| Database | localhost | Cloud/Remote |
| ddl-auto | update | validate |
| show_sql | true | false |
| Passwords | Hardcoded | Environment variables |

---

## How To Activate This Profile

```text
Option 1: Change application.yaml
    spring.profiles.active: prod

Option 2: Command line
    java -jar app.jar --spring.profiles.active=prod

Option 3: Environment variable
    SPRING_PROFILES_ACTIVE=prod
```

---

---

# 14. application-qa.yml (QA Profile)

## Full Content

```yaml
server:
  port: 8081
```

---

## Why Does This File Exist?

QA (Quality Assurance) environment runs on a different port.

```text
Dev:  8083
QA:   8081
Prod: 8080
```

This allows running all environments on the same machine:

```text
localhost:8083 → Dev
localhost:8081 → QA
localhost:8080 → Prod
```

---

## Category

```text
Configuration (Profile-specific)
```

---

## Port Summary

| Profile | Port | File |
|---------|------|------|
| Default | 8082 | application.yaml |
| Dev | 8083 | application-dev.yml |
| QA | 8081 | application-qa.yml |
| Prod | 8080 | application-prod.yml |

---

---

# 15. pom.xml (Project Object Model)

## Why Does This File Exist?

pom.xml is the HEART of a Maven project.

```text
It tells Maven:
1. What is this project?
2. What dependencies does it need?
3. How should it be built?
```

Analogy:

```text
pom.xml = Shopping List + Recipe

"I need these ingredients (dependencies).
Here's how to cook the dish (build configuration)."
```

---

## Category

```text
Build / Infrastructure
```

---

## Key Sections Breakdown

### Parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.15</version>
</parent>
```

```text
This project inherits from Spring Boot 3.5.15.

The parent provides:
    - Default dependency versions
    - Default plugin configurations
    - Default property values
```

Without the parent:

```text
You would have to specify versions for EVERY dependency.
With the parent, Spring Boot manages versions for you.
```

---

### Project Coordinates

```xml
<groupId>com.substring.auth</groupId>
<artifactId>auth-app</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

| Element | Value | Meaning |
|---------|-------|---------|
| groupId | com.substring.auth | Organization/company identifier |
| artifactId | auth-app | Project name |
| version | 0.0.1-SNAPSHOT | Version (SNAPSHOT = in development) |

---

### Java Version

```xml
<properties>
    <java.version>25</java.version>
</properties>
```

```text
This project uses Java 25.
Maven will compile using Java 25 features.
```

---

### Dependencies (7 Total)

#### 1. spring-boot-starter-data-jpa

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

```text
Brings in:
    - Hibernate (ORM)
    - Spring Data JPA (Repositories)
    - HikariCP (Connection Pool)
    - Jakarta Persistence API

Used by: User.java, Role.java, UserRepository.java
```

---

#### 2. spring-boot-starter-security

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```text
Brings in:
    - Spring Security
    - Password encoding (BCrypt)
    - Authentication/Authorization filters
    - CSRF protection

Note: Adding this dependency automatically
secures ALL endpoints with a login page.
```

---

#### 3. spring-boot-starter-validation

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

```text
Brings in:
    - Jakarta Validation (Bean Validation)
    - @NotNull, @Email, @Size annotations

Used for: Validating DTOs before processing.
Example:
    @Email
    private String email;
```

---

#### 4. spring-boot-starter-web

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

```text
Brings in:
    - Embedded Tomcat
    - Spring MVC
    - Jackson (JSON serialization)
    - RESTful web service support

Used for: Creating REST APIs.
```

---

#### 5. mysql-connector-j

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

```text
The MySQL JDBC driver.
Allows Java to talk to MySQL.

scope=runtime means:
    Not needed at compile time.
    Only needed when the application RUNS.
```

---

#### 6. lombok

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

```text
Generates boilerplate code at compile time:
    Getters, Setters, Constructors, Builders

optional=true means:
    Projects that depend on YOUR project
    will NOT automatically get Lombok.
```

---

#### 7. spring-boot-starter-test + spring-security-test

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

```text
Testing libraries:
    - JUnit 5
    - Mockito
    - Spring Test
    - Security Test (mock users, roles)

scope=test means:
    Only available during testing.
    NOT included in the production JAR.
```

---

### Build Plugins

#### spring-boot-maven-plugin

```text
Creates executable JAR files.
Excludes Lombok from the final JAR (not needed at runtime).
```

#### maven-compiler-plugin

```text
Configures the Java compiler.
Registers Lombok as an annotation processor
for both main code and test code.
```

---

## Dependency Flow Visualization

```text
pom.xml Dependencies
    │
    ├── starter-web        → Tomcat, Spring MVC, Jackson
    ├── starter-data-jpa   → Hibernate, JPA, HikariCP
    ├── starter-security   → Authentication, Authorization
    ├── starter-validation  → Bean Validation
    ├── mysql-connector-j   → MySQL Driver
    ├── lombok              → Code Generation
    └── starter-test        → JUnit, Mockito
```

---

---

# 16. .gitignore

## Full Content

```text
HELP.md
target/
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/
```

---

## Why Does This File Exist?

Git tracks ALL files in a project by default.

Some files should NEVER be committed:

```text
- Compiled code (target/)
- IDE settings (.idea, .vscode)
- Sensitive data
- OS-generated files
```

.gitignore tells Git:

```text
"Ignore these files. Don't track them."
```

---

## Category

```text
Version Control
```

---

## Key Entries Explained

| Pattern | What It Ignores |
|---------|-----------------|
| target/ | Compiled classes, JARs (build output) |
| .idea | IntelliJ IDEA project settings |
| *.iml | IntelliJ module files |
| .vscode/ | VS Code settings |
| .classpath | Eclipse classpath |
| build/ | Gradle build output |
| HELP.md | Spring Initializr generated help file |

The `!` prefix means "DO NOT ignore":

```text
!**/src/main/**/target/
→ Even though we ignore target/,
   DO NOT ignore target/ inside src/main/
```

---

---

# 17. .gitattributes

## Full Content

```text
/mvnw text eol=lf
*.cmd text eol=crlf
```

---

## Why Does This File Exist?

Different operating systems use different line endings:

```text
Windows: \r\n (CRLF)
Linux/Mac: \n (LF)
```

This causes problems when developers on different OS contribute.

.gitattributes normalizes line endings:

| Rule | Meaning |
|------|---------|
| /mvnw text eol=lf | Maven wrapper script uses LF (Unix format) |
| *.cmd text eol=crlf | Windows batch files use CRLF (Windows format) |

Why?

```text
mvnw is a Linux/Mac shell script → needs LF
mvnw.cmd is a Windows batch file → needs CRLF
```

---

## Category

```text
Version Control
```

---

---

# 18. maven-wrapper.properties

## Full Content

```text
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

---

## Why Does This File Exist?

Problem:

```text
Developer A has Maven 3.9.16
Developer B has Maven 3.8.1

Different Maven versions might build differently.
```

Solution: Maven Wrapper.

```text
The Maven Wrapper ensures EVERYONE uses the same Maven version.
```

How:

```text
Instead of running:
    mvn clean install

You run:
    ./mvnw clean install  (Linux/Mac)
    mvnw.cmd clean install (Windows)

The wrapper downloads Maven 3.9.16 automatically
if it's not already installed.
```

---

## Properties Explained

| Property | Value | Meaning |
|----------|-------|---------|
| wrapperVersion | 3.3.4 | Version of the wrapper plugin itself |
| distributionType | only-script | Only use wrapper scripts (no JAR download) |
| distributionUrl | ...apache-maven-3.9.16-bin.zip | URL to download Maven 3.9.16 |

---

## Category

```text
Build / Infrastructure
```

---

---

# Summary: How Everything Connects

```text
                    ┌─────────────────────┐
                    │ AuthAppApplication   │
                    │  (Starts Everything) │
                    └─────────┬───────────┘
                              │
                    ┌─────────▼───────────┐
                    │   Component Scan     │
                    │ Finds all @Service,  │
                    │ @Repository, @Entity │
                    └─────────┬───────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
    ┌─────────▼──────┐ ┌─────▼──────┐ ┌──────▼──────────┐
    │ UserServiceImpl │ │   User     │ │ UserRepository  │
    │   (@Service)    │ │  (@Entity) │ │ (JpaRepository) │
    └───────┬────────┘ └─────┬──────┘ └────────┬────────┘
            │                │                  │
            │    ┌───────────┼───────────┐      │
            │    │           │           │      │
            │  ┌─▼──┐  ┌────▼────┐  ┌───▼──┐   │
            │  │Role │  │Provider │  │MySQL │   │
            │  └─────┘  └─────────┘  └──────┘   │
            │                                    │
            └──── uses ──────────────────────────┘
```

Data Flow:

```text
Client → [Controller] → UserDto → [UserService] → [UserServiceImpl]
                                                        ↓
                                              UserDto → User (Entity)
                                                        ↓
                                              [UserRepository.save()]
                                                        ↓
                                              Hibernate → MySQL
                                                        ↓
                                              User (saved) → UserDto
                                                        ↓
                                              Response ← UserDto
```

Configuration Flow:

```text
application.yaml          → Base settings
    + application-dev.yml  → Dev overrides
    = Final Configuration  → Port 8083, MySQL localhost, show SQL
```

Build Flow:

```text
pom.xml                   → Defines dependencies
maven-wrapper.properties  → Ensures consistent Maven version
.gitignore                → Keeps build artifacts out of Git
.gitattributes            → Normalizes line endings
```

---

# Interview Quick Reference

| Question | Answer |
|----------|--------|
| What does @SpringBootApplication do? | Combines @Configuration + @EnableAutoConfiguration + @ComponentScan |
| What is a DTO? | Data Transfer Object — carries data between layers without exposing internals |
| Why use an Enum instead of String? | Type safety, compile-time checking, no typos |
| What is @Entity? | Marks a class as a JPA entity mapped to a database table |
| What is JpaRepository? | Interface providing CRUD operations — Spring generates implementation |
| Why use Optional? | Forces handling of null cases, prevents NullPointerException |
| What is @ManyToMany? | Relationship where both sides can have multiple references |
| What is a Join Table? | Intermediate table that connects two entities in a Many-to-Many |
| What does ddl-auto: update do? | Auto-creates/modifies tables to match entities |
| What is the Maven Wrapper? | Ensures all developers use the same Maven version |
| Why separate Service interface and implementation? | Abstraction, testability, swappability (Dependency Inversion) |
| What is @PrePersist? | JPA lifecycle callback — runs before first save |
| What is @PreUpdate? | JPA lifecycle callback — runs before every update |
| Why FetchType.EAGER for roles? | Roles are always needed for authentication/authorization |
| What is HikariCP? | High-performance JDBC connection pool (default in Spring Boot) |

---

> "Understanding every file in your project is like understanding every instrument in an orchestra. Each one has a role. Together, they create the symphony."
