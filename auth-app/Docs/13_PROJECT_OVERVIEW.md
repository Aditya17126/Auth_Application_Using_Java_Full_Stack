# Project Overview: Auth Application (Java Full Stack)

A complete first-principles guide to understanding how this project is organized.

---

# Section 1: Project Structure Overview

---

## What Are We Building?

An Authentication Application.

Question:

> What does an authentication application do?

Answer:

```text
1. Let users REGISTER
2. Let users LOGIN
3. Control WHO can access WHAT
```

Think of it like a building security system:

```text
Registration  → Getting your ID card
Login         → Showing your ID card
Authorization → Which floors you can access
```

---

## The Complete File Tree

```text
auth-app/
│
├── .gitattributes
├── .gitignore
├── .idea/                          ← IDE settings (ignored)
├── .mvn/wrapper/
│   └── maven-wrapper.properties
│
├── Docs/                           ← Learning documentation
│
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml                         ← Project dependencies
│
├── src/
│   ├── main/
│   │   ├── java/com/substring/auth/auth_app/
│   │   │   │
│   │   │   ├── AuthAppApplication.java       ← Entry point
│   │   │   │
│   │   │   ├── entities/                     ← Database models
│   │   │   │   ├── Provider.java
│   │   │   │   ├── Role.java
│   │   │   │   └── User.java
│   │   │   │
│   │   │   ├── dtos/                         ← Data Transfer Objects
│   │   │   │   ├── RoleDto.java
│   │   │   │   └── UserDto.java
│   │   │   │
│   │   │   ├── repositories/                 ← Database access
│   │   │   │   └── UserRepository.java
│   │   │   │
│   │   │   └── services/                     ← Business logic
│   │   │       ├── UserService.java
│   │   │       └── UserServiceImpl.java
│   │   │
│   │   └── resources/                        ← Configuration
│   │       ├── application.yaml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── application-qa.yml
│   │       ├── static/
│   │       └── templates/
│   │
│   └── test/                                 ← Tests
│       └── java/com/substring/auth/auth_app/
│           └── AuthAppApplicationTests.java
│
└── target/                                   ← Compiled output
```

---

## Why Is the Project Organized This Way?

Question:

> Imagine you have 50 Java files. Do you put them all in one folder?

Answer:

```text
NO.
```

That would be like putting all your clothes in one pile:

```text
Shirts
Pants
Socks
Underwear
Jackets
```

All in one pile.

Finding anything takes forever.

---

Instead, you organize:

```text
Wardrobe/
├── Shirts/
├── Pants/
├── Socks/
├── Underwear/
└── Jackets/
```

Now you know exactly where to look.

Software projects follow the same principle.

---

## The Design Pattern: Layered Architecture

This project uses:

```text
Layered Architecture
```

also known as:

```text
N-Tier Architecture
```

---

### What is Layered Architecture?

Think of a restaurant:

```text
Customer
    ↓
Waiter (takes order)
    ↓
Chef (makes food)
    ↓
Storage Room (gets ingredients)
```

Each person has ONE job.

The customer never enters the kitchen.

The chef never talks to the customer.

---

In this project:

```text
Client (Browser / Postman)
    ↓
Controller Layer  (takes request)     ← MISSING - Not built yet
    ↓
Service Layer     (business logic)    ← EXISTS  - But stubs (return null)
    ↓
Repository Layer  (database access)   ← EXISTS  - Working
    ↓
Entity Layer      (data models)       ← EXISTS  - Working
    ↓
Database (MySQL)
```

---

### Why Layers?

Rule:

```text
Each layer talks ONLY to the layer directly below it.
```

Example:

```text
Controller → Service    ✅ Allowed
Controller → Repository ❌ Not allowed
Service    → Repository ✅ Allowed
Service    → Database   ❌ Not allowed (Repository does this)
```

---

Analogy:

```text
Manager → Team Lead → Developer → Code

Manager does NOT write code directly.
Team Lead does NOT talk to the client directly.
Developer does NOT attend client meetings.
```

Each layer has clear boundaries.

---

### The Layer Responsibilities

```text
┌─────────────────────────────────────────────┐
│              CONTROLLER LAYER               │
│                                             │
│  Receives HTTP requests from the client     │
│  Validates input format                     │
│  Returns HTTP responses                     │
│                                             │
│  Status: ❌ NOT YET CREATED                 │
├─────────────────────────────────────────────┤
│              SERVICE LAYER                  │
│                                             │
│  Contains business logic                    │
│  Decides WHAT to do                         │
│  Transforms data between DTOs and Entities  │
│                                             │
│  Status: ⚠️  STUBS ONLY (return null)       │
├─────────────────────────────────────────────┤
│              REPOSITORY LAYER               │
│                                             │
│  Talks to the database                      │
│  Performs CRUD operations                   │
│  Translates Java to SQL                     │
│                                             │
│  Status: ✅ WORKING                         │
├─────────────────────────────────────────────┤
│              ENTITY LAYER                   │
│                                             │
│  Defines data structure                     │
│  Maps Java objects to database tables       │
│  Defines relationships between tables       │
│                                             │
│  Status: ✅ WORKING                         │
├─────────────────────────────────────────────┤
│              DATABASE (MySQL)               │
│                                             │
│  Stores data permanently                    │
│  Port: 3306                                 │
│  Database: auth_application_java            │
│                                             │
│  Status: ✅ EXTERNAL (already running)      │
└─────────────────────────────────────────────┘
```

---

### How a Request Would Flow (When Complete)

Suppose a user registers:

```text
Step 1: Client sends POST /api/users
            ↓
Step 2: Controller receives the request
            ↓
Step 3: Controller calls Service
            ↓
Step 4: Service validates business rules
            ↓
Step 5: Service converts DTO → Entity
            ↓
Step 6: Service calls Repository
            ↓
Step 7: Repository saves to MySQL
            ↓
Step 8: Repository returns saved Entity
            ↓
Step 9: Service converts Entity → DTO
            ↓
Step 10: Service returns DTO to Controller
            ↓
Step 11: Controller returns HTTP 201 Created
```

Currently:

```text
Steps 1-3:   ❌ Cannot happen (no Controller)
Steps 4-6:   ⚠️  Stubs (methods return null)
Steps 7-8:   ✅ Ready (Repository works)
Steps 9-11:  ❌ Cannot happen (stubs + no Controller)
```

---

### Convention: MVC Pattern

This project also follows:

```text
MVC = Model - View - Controller
```

Mapping:

```text
Model      → entities/ folder     (data)
View       → Not applicable       (REST API, no HTML views)
Controller → Not yet created      (will handle HTTP)
```

Since this is a REST API and not a web page application:

```text
Traditional MVC:    Model → View → Controller
This project:       Entity → DTO → Controller (REST)
```

The View is replaced by JSON responses.

---

## Advantages of This Structure

| # | Advantage | Explanation |
|---|-----------|-------------|
| 1 | **Separation of Concerns** | Each folder has ONE job. Entities define data. Services handle logic. Repositories talk to the database. Nobody does someone else's job. |
| 2 | **Easy to Find Files** | Need to change a database query? Go to `repositories/`. Need to change business logic? Go to `services/`. No guessing. |
| 3 | **Easy to Test** | You can test each layer independently. Test the service without needing a database. Test the repository without needing a controller. |
| 4 | **Team Collaboration** | Developer A works on entities. Developer B works on services. They don't step on each other's toes. |
| 5 | **Easy to Replace** | Want to switch from MySQL to PostgreSQL? Only the repository layer changes. Services and controllers stay the same. |
| 6 | **Industry Standard** | Almost every Spring Boot project uses this pattern. Learn it once, use it everywhere. |
| 7 | **Scalable** | Adding new features means adding new files in existing folders. The structure grows naturally. |

---

## Disadvantages of This Structure

| # | Disadvantage | Explanation |
|---|--------------|-------------|
| 1 | **Boilerplate Code** | Every entity needs a DTO, a repository, a service interface, a service implementation, and a controller. That's 5+ files per feature. |
| 2 | **Over-engineering for Small Projects** | If you only have one entity, this structure feels heavy. But this project will grow. |
| 3 | **Rigid Layering** | Sometimes you want to skip a layer (e.g., Controller directly calling Repository). Layered architecture says NO. You must always go through the Service layer. |
| 4 | **Mapping Overhead** | Converting Entity → DTO and DTO → Entity for every operation. This is repetitive work (libraries like MapStruct can help). |
| 5 | **Package Growth** | As the project grows, each folder gets many files. You might need sub-packages like `services.user`, `services.role`, etc. |

---

## Current Project Status

```text
┌──────────────────┬──────────┬───────────────────────────────────┐
│ Component        │ Status   │ Notes                             │
├──────────────────┼──────────┼───────────────────────────────────┤
│ Entities         │ ✅ Done  │ User, Role, Provider defined      │
│ DTOs             │ ✅ Done  │ UserDto, RoleDto defined          │
│ Repositories     │ ✅ Done  │ UserRepository with custom queries│
│ Services         │ ⚠️ Stubs │ All methods return null           │
│ Controllers      │ ❌ None  │ No REST endpoints exist           │
│ Security         │ ❌ None  │ Dependency added but not config'd │
│ Exception Handling│ ❌ None │ No @ControllerAdvice              │
│ DTO Mapping      │ ❌ None  │ No mapper utility                 │
│ Tests            │ ⚠️ Basic │ Only smoke test exists            │
└──────────────────┴──────────┴───────────────────────────────────┘
```

---

## Technology Stack Summary

```text
┌─────────────────────────────────────────┐
│           TECHNOLOGY STACK              │
├─────────────────────────────────────────┤
│                                         │
│  Language:    Java 25                   │
│  Framework:   Spring Boot 3.5.15       │
│  Build Tool:  Maven (with wrapper)      │
│  Database:    MySQL 8+                  │
│  ORM:         Hibernate (via JPA)       │
│  Pool:        HikariCP (default)        │
│  Port:        8083 (dev profile)        │
│                                         │
│  Dependencies (pom.xml):                │
│  ┌────────────────────────────────────┐ │
│  │ spring-boot-starter-data-jpa      │ │
│  │ spring-boot-starter-security      │ │
│  │ spring-boot-starter-validation    │ │
│  │ spring-boot-starter-web           │ │
│  │ mysql-connector-j                 │ │
│  │ lombok                            │ │
│  │ spring-boot-starter-test          │ │
│  │ spring-security-test              │ │
│  └────────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

---

## Package Naming Convention

```text
com.substring.auth.auth_app
│       │        │     │
│       │        │     └── Artifact ID (project name)
│       │        └── Module (auth)
│       └── Organization (substring)
└── Top-level domain (com)
```

This follows Java's reverse domain naming convention:

```text
Company:  substring.com
Package:  com.substring.auth.auth_app
```

Why?

```text
To avoid naming conflicts with other libraries.
```

Example:

```text
com.substring.auth.User  → YOUR User class
com.google.auth.User     → Google's User class
```

No collision.

---

---

# Section 2: Folder-by-Folder Analysis

---

## Folder 1: `entities/` — The Data Models

---

### Purpose

```text
Define WHAT data looks like.
```

This folder answers:

```text
What does a User look like?
What does a Role look like?
How does a User login (Provider)?
```

---

### Files in This Folder

| File | Purpose | Type |
|------|---------|------|
| `User.java` | Represents a user in the system | @Entity (JPA class) |
| `Role.java` | Represents a permission role | @Entity (JPA class) |
| `Provider.java` | Lists login methods (LOCAL, GOOGLE, etc.) | Enum |

---

### Analogy

Think of entities as **blueprints for a house**.

```text
Blueprint (Entity)
    ↓
Says: 3 bedrooms, 2 bathrooms, 1 kitchen
    ↓
Builder (Hibernate) reads the blueprint
    ↓
Builds the actual house (Database Table)
```

The blueprint is NOT the house.

The blueprint DESCRIBES the house.

Similarly:

```text
User.java is NOT the database table.
User.java DESCRIBES the database table.
```

Hibernate reads User.java and creates:

```text
users table
    ↓
id | email | name | password | image | enable | ...
```

---

### What Happens If This Folder Didn't Exist?

```text
No entities = No database tables
No database tables = No data storage
No data storage = Application is useless
```

Imagine a restaurant with no menu:

```text
Customer: "What do you serve?"
Waiter:   "We don't know."
```

Nothing works.

Entities are the FOUNDATION of the entire application.

---

### How It Interacts With Other Folders

```text
entities/ is used by:
    ↓
repositories/   → Repository needs Entity to know WHAT to save
    ↓
services/       → Service manipulates Entities for business logic
    ↓
dtos/           → DTOs mirror Entity structure for safe data transfer
```

Flow:

```text
Entity defines data structure
    ↓
Repository stores/retrieves Entities
    ↓
Service transforms Entities ↔ DTOs
    ↓
Controller (future) sends DTOs to the client
```

---

### Key Details

User.java creates this database table:

```text
users
──────────────────────────────────────────
id          │ UUID (primary key)
email       │ VARCHAR
name        │ VARCHAR
password    │ VARCHAR
image       │ VARCHAR
enable      │ BOOLEAN (default: true)
created_at  │ TIMESTAMP
updated_at  │ TIMESTAMP
provider    │ VARCHAR (enum: LOCAL, GOOGLE, etc.)
──────────────────────────────────────────
```

Role.java creates:

```text
roles
──────────────────────────────────────────
id          │ UUID (primary key)
name        │ VARCHAR
──────────────────────────────────────────
```

The @ManyToMany relationship creates a third table:

```text
user_roles
──────────────────────────────────────────
user_id     │ UUID (foreign key → users)
role_id     │ UUID (foreign key → roles)
──────────────────────────────────────────
```

Visual:

```text
users ──────┐
            │──→ user_roles (join table)
roles ──────┘
```

---

---

## Folder 2: `dtos/` — The Data Transfer Objects

---

### Purpose

```text
Define WHAT data is safe to send outside the application.
```

This folder answers:

```text
What should the client SEE?
What should we HIDE from the client?
```

---

### Files in This Folder

| File | Purpose | Fields |
|------|---------|--------|
| `UserDto.java` | Safe user data for client | Mirrors User entity fields |
| `RoleDto.java` | Safe role data for client | UUID id, String name |

---

### Analogy

Think of a hospital:

```text
Patient Record (Entity)
──────────────────────────
Name:              Aditya
Blood Type:        O+
Medical History:   Surgery in 2020
Social Security:   123-45-6789
Credit Card:       4111-xxxx-xxxx-1234
```

Question:

> Should we send ALL this to the front desk?

Answer:

```text
NO!
```

We only send:

```text
Patient Summary (DTO)
──────────────────────────
Name:              Aditya
Blood Type:        O+
```

The DTO hides sensitive information.

---

In this project:

```text
User Entity (database)
──────────────────────────
id
email
name
password          ← SENSITIVE! Never send to client!
image
enable
createdAt
updatedAt
provider
roles
```

UserDto (what client receives):

```text
UserDto
──────────────────────────
id
email
name
                  ← No password!
image
enable
createdAt
updatedAt
provider
roles (as RoleDtos)
```

---

### Why Not Just Use the Entity Directly?

Three reasons:

Reason 1: Security

```text
Entity has password field.
Sending Entity to client → Client sees password.
```

Reason 2: Circular References

```text
User has Roles.
If Role also has Users:

User → Roles → Users → Roles → Users → ...

Infinite loop. Application crashes.
```

DTO breaks this cycle:

```text
UserDto → RoleDtos (no back-reference)
```

Reason 3: Flexibility

```text
Entity must match database structure EXACTLY.
DTO can have any shape you want.

Example:
Entity has: firstName, lastName
DTO can have: fullName (firstName + " " + lastName)
```

---

### What Happens If This Folder Didn't Exist?

```text
Without DTOs:
    ↓
You send Entity objects directly to the client
    ↓
Password gets exposed
    ↓
Security vulnerability
    ↓
Potential data breach
```

Also:

```text
Without DTOs:
    ↓
Any database change forces API change
    ↓
All clients break
    ↓
Maintenance nightmare
```

DTOs act as a SHIELD between your database and the outside world.

---

### How It Interacts With Other Folders

```text
dtos/ interacts with:
    ↓
services/     → Service converts Entity ↔ DTO
    ↓
controllers/  → Controller receives/sends DTOs (not Entities)
    ↓
entities/     → DTOs are MODELED after Entities (but NOT the same)
```

Flow:

```text
Client sends JSON
    ↓
Controller receives DTO
    ↓
Service converts DTO → Entity
    ↓
Repository saves Entity to DB
    ↓
Repository returns Entity
    ↓
Service converts Entity → DTO
    ↓
Controller sends DTO as JSON
```

Important rule:

```text
Entities NEVER leave the Service layer.
Only DTOs go to the Controller.
Only DTOs go to the Client.
```

---

---

## Folder 3: `repositories/` — The Database Access Layer

---

### Purpose

```text
Talk to the database.
```

This folder answers:

```text
How do we SAVE a user?
How do we FIND a user?
How do we DELETE a user?
How do we CHECK if a user exists?
```

---

### Files in This Folder

| File | Purpose | Extends |
|------|---------|---------|
| `UserRepository.java` | Database operations for User | JpaRepository<User, UUID> |

---

### Analogy

Think of a librarian:

```text
You:       "I want the book 'Java for Beginners'."
Librarian: Goes to the shelf → Finds the book → Gives it to you.

You:       "Please add this new book to the library."
Librarian: Takes the book → Puts it on the correct shelf.
```

You NEVER go to the shelf yourself.

The librarian (Repository) does it for you.

---

In this project:

```text
Service:     "Find the user with email aditya@gmail.com"
Repository:  Goes to MySQL → Runs SELECT query → Returns User object

Service:     "Save this new user"
Repository:  Goes to MySQL → Runs INSERT query → Returns saved User
```

---

### What JpaRepository Gives You for Free

```text
UserRepository extends JpaRepository<User, UUID>
```

This single line gives you:

```text
┌─────────────────────────────────────────────────┐
│           FREE METHODS                          │
├─────────────────────────────────────────────────┤
│ save(user)              → INSERT or UPDATE      │
│ findById(uuid)          → SELECT by primary key │
│ findAll()               → SELECT all users      │
│ deleteById(uuid)        → DELETE by primary key │
│ count()                 → COUNT all users       │
│ existsById(uuid)        → CHECK if user exists  │
│ ... and many more                               │
└─────────────────────────────────────────────────┘
```

You write ZERO SQL.

Spring Data JPA generates the SQL automatically.

---

### Custom Queries in UserRepository

This project adds two custom methods:

```java
Optional<User> findByEmail(String email);
```

Spring sees the method name and generates:

```sql
SELECT * FROM users WHERE email = ?
```

```java
boolean existsByEmail(String email);
```

Spring generates:

```sql
SELECT COUNT(*) > 0 FROM users WHERE email = ?
```

This is called:

```text
Query Derivation
```

Spring reads the method name and derives the SQL query.

```text
findByEmail
│  │    │
│  │    └── WHERE clause (email = ?)
│  └── Action (SELECT)
└── Prefix (find)
```

---

### What Happens If This Folder Didn't Exist?

```text
Without repositories:
    ↓
You write raw SQL manually
    ↓
You manage database connections manually
    ↓
You handle result set mapping manually
    ↓
Hundreds of lines of boilerplate code
```

Example without repository:

```java
Connection conn = DriverManager.getConnection(url, user, pass);
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
ps.setString(1, email);
ResultSet rs = ps.executeQuery();
if (rs.next()) {
    User user = new User();
    user.setId(UUID.fromString(rs.getString("id")));
    user.setEmail(rs.getString("email"));
    // ... 10 more fields
}
conn.close();
```

With repository:

```java
Optional<User> user = userRepository.findByEmail(email);
```

One line.

---

### How It Interacts With Other Folders

```text
repositories/ interacts with:
    ↓
entities/     → Repository needs Entity type (User, Role)
    ↓
services/     → Service calls Repository methods
    ↓
database      → Repository auto-generates SQL and sends to MySQL
```

Flow:

```text
Service calls repository.save(user)
    ↓
Spring Data JPA intercepts
    ↓
Hibernate converts User object → SQL
    ↓
HikariCP provides database connection
    ↓
MySQL executes INSERT statement
    ↓
Result flows back up
```

Important:

```text
Repository NEVER contains business logic.
Repository ONLY does database operations.
```

---

### Missing: RoleRepository

Currently there is no:

```text
RoleRepository.java
```

This means:

```text
No way to save or find Roles independently.
Roles can only be managed through the User entity.
```

This will need to be created as the project grows.

---

---

## Folder 4: `services/` — The Business Logic Layer

---

### Purpose

```text
Decide WHAT to do with the data.
```

This folder answers:

```text
How do we register a user?
What rules must be followed?
What happens when a user updates their profile?
```

---

### Files in This Folder

| File | Purpose | Type |
|------|---------|------|
| `UserService.java` | Defines WHAT operations exist | Interface |
| `UserServiceImpl.java` | Defines HOW operations work | Implementation (@Service) |

---

### Analogy

Think of a recipe:

```text
UserService.java (Interface) = Menu Card
    ↓
"We offer: Registration, Login, Profile Update, Delete Account"

UserServiceImpl.java (Implementation) = Recipe Book
    ↓
"To register: validate email, hash password, save to database..."
```

The menu card says WHAT is available.

The recipe book says HOW to make it.

---

### Why Both Interface and Implementation?

Question:

> Why not just one file?

Answer:

Suppose tomorrow you want TWO different implementations:

```text
UserServiceImpl.java       → Normal users (saves to MySQL)
AdminUserServiceImpl.java  → Admin users (saves to MySQL + sends notification)
```

Both implement the same interface:

```text
UserService
    ├── UserServiceImpl
    └── AdminUserServiceImpl
```

Spring can switch between them without changing ANY other code.

This is called:

```text
Programming to an Interface
```

Also called:

```text
Dependency Inversion Principle (D in SOLID)
```

---

### Current State: All Stubs

Every method in UserServiceImpl currently:

```java
@Override
public UserDto createUser(UserDto userDto) {
    return null;  // ← STUB
}
```

This means:

```text
The methods EXIST but DO NOTHING.
They are placeholders waiting for real logic.
```

Why write stubs?

```text
1. Define the API contract early
2. Other developers know what methods will exist
3. Can write tests that expect certain behavior
4. Gradual development (fill in one method at a time)
```

---

### What Methods Are Defined?

```text
UserService Interface:
──────────────────────────────────────────
createUser(UserDto)          → Register new user
updateUser(UUID, UserDto)    → Update existing user
deleteUser(UUID)             → Delete user
getUserById(UUID)            → Find one user
getAllUsers()                 → List all users
──────────────────────────────────────────
```

These are standard CRUD operations:

```text
C = Create  → createUser
R = Read    → getUserById, getAllUsers
U = Update  → updateUser
D = Delete  → deleteUser
```

---

### What Happens If This Folder Didn't Exist?

```text
Without services:
    ↓
Controller talks directly to Repository
    ↓
Business logic gets mixed into Controller
    ↓
Controller becomes massive and unreadable
    ↓
Cannot reuse logic
    ↓
Cannot test business rules independently
```

Imagine a restaurant where the waiter also cooks:

```text
Waiter:
  - Takes order
  - Goes to kitchen
  - Cooks food
  - Serves food
  - Handles payment
```

One person doing everything = chaos.

The Service layer is the Chef:

```text
Waiter (Controller): "Customer wants pasta."
Chef (Service):      "I'll cook it my way."
Storage (Repository): "Here are the ingredients."
```

---

### How It Interacts With Other Folders

```text
services/ interacts with:
    ↓
repositories/   → Service calls Repository to access database
    ↓
entities/       → Service works with Entity objects internally
    ↓
dtos/           → Service receives and returns DTOs
    ↓
controllers/    → Controller calls Service methods (future)
```

Data transformation flow:

```text
Controller sends UserDto
    ↓
Service receives UserDto
    ↓
Service converts UserDto → User (Entity)
    ↓
Service calls repository.save(user)
    ↓
Service converts saved User → UserDto
    ↓
Service returns UserDto to Controller
```

---

---

## Folder 5: `resources/` — The Configuration Files

---

### Purpose

```text
Configure HOW the application behaves.
```

This folder answers:

```text
Which database to connect to?
Which port to run on?
What is the application name?
Which environment are we in?
```

---

### Files in This Folder

| File | Purpose | Key Config |
|------|---------|------------|
| `application.yaml` | Main config (shared) | app name: auth-app, active profile: dev, port: 8082 |
| `application-dev.yml` | Development config | MySQL localhost:3306, ddl-auto: update, port: 8083 |
| `application-prod.yml` | Production config | port: 8080 only |
| `application-qa.yml` | QA/Testing config | port: 8081 only |
| `static/` | Static files (CSS, JS) | Empty |
| `templates/` | HTML templates | Empty |

---

### Analogy

Think of a car:

```text
application.yaml        = Default car settings
application-dev.yml     = City driving mode (comfort, slower)
application-prod.yml    = Highway mode (performance, faster)
application-qa.yml      = Test drive mode (monitoring everything)
```

Same car. Different settings for different situations.

---

### How Profiles Work

```text
application.yaml says:
    spring.profiles.active = dev
        ↓
    Spring loads: application.yaml FIRST
        ↓
    Then OVERRIDES with: application-dev.yml
        ↓
    Final config = base + dev
```

Example:

```text
application.yaml:       port = 8082
application-dev.yml:    port = 8083

Result:                 port = 8083  (dev overrides base)
```

Switching to production:

```text
Change: spring.profiles.active = prod
    ↓
Spring loads: application.yaml + application-prod.yml
    ↓
Result: port = 8080
```

---

### Environment Port Map

```text
┌─────────────┬───────┬──────────────────────────────┐
│ Environment │ Port  │ Database                     │
├─────────────┼───────┼──────────────────────────────┤
│ Default     │ 8082  │ (not specified)              │
│ Dev         │ 8083  │ localhost:3306/auth_app_java  │
│ QA          │ 8081  │ (not specified)              │
│ Prod        │ 8080  │ (not specified)              │
└─────────────┴───────┴──────────────────────────────┘
```

Note:

```text
Only DEV has database configured.
QA and PROD need database config before they can be used.
```

---

### What Happens If This Folder Didn't Exist?

```text
Without resources/:
    ↓
No database connection info
    ↓
Application cannot start
    ↓
Spring Boot has NO idea:
  - Which database to use
  - Which port to run on
  - What the app is called
```

You would have to hardcode everything:

```java
// BAD
String dbUrl = "jdbc:mysql://localhost:3306/auth_application_java";
String dbUser = "root";
String dbPass = "password";
```

Hardcoding is dangerous:

```text
1. Passwords visible in source code
2. Cannot change without recompiling
3. Different environments need different values
```

Configuration files solve all these problems.

---

### How It Interacts With Other Folders

```text
resources/ interacts with:
    ↓
Spring Boot Core     → Reads config at startup
    ↓
entities/            → ddl-auto setting controls table creation
    ↓
repositories/        → Database URL tells where to connect
    ↓
Embedded Tomcat      → Port setting controls where app listens
```

Startup flow:

```text
Spring Boot starts
    ↓
Reads application.yaml
    ↓
Sees: active profile = dev
    ↓
Reads application-dev.yml
    ↓
Merges configurations
    ↓
Connects to MySQL at localhost:3306
    ↓
Sets server port to 8083
    ↓
Hibernate scans @Entity classes
    ↓
Creates/Updates tables (ddl-auto: update)
    ↓
Application Ready
```

---

---

## Folder 6: Root Files — Project Infrastructure

---

### Purpose

```text
Build, configure, and manage the project itself.
```

These are NOT Java code files.

They are PROJECT MANAGEMENT files.

---

### Files at the Root

| File | Purpose |
|------|---------|
| `pom.xml` | Maven build config — lists ALL dependencies |
| `mvnw` | Maven wrapper for Linux/Mac |
| `mvnw.cmd` | Maven wrapper for Windows |
| `.gitignore` | Tells Git which files to ignore |
| `.gitattributes` | Controls line endings (Windows vs Linux) |
| `HELP.md` | Spring Initializr generated reference links |

---

### Analogy

Think of a construction project:

```text
pom.xml          = Shopping list (all materials needed)
mvnw / mvnw.cmd  = Delivery truck (brings materials)
.gitignore       = "Do NOT deliver these items"
.gitattributes   = "Standard format for documents"
HELP.md          = Instruction manual
```

---

### pom.xml — The Shopping List

Every dependency your project needs:

```text
pom.xml says:
──────────────────────────────────────────
"I need":
  1. spring-boot-starter-data-jpa     → Database access
  2. spring-boot-starter-security     → Login/Auth
  3. spring-boot-starter-validation   → Input checking
  4. spring-boot-starter-web          → REST API
  5. mysql-connector-j                → MySQL driver
  6. lombok                           → Less boilerplate
  7. spring-boot-starter-test         → Testing
  8. spring-security-test             → Security testing
──────────────────────────────────────────
```

Maven reads this file and downloads ALL required JARs.

Without pom.xml:

```text
You would manually download 100+ JAR files.
And manage their versions yourself.
And handle conflicts between libraries.
```

---

### Maven Wrapper (mvnw / mvnw.cmd)

Question:

> What if someone doesn't have Maven installed?

Answer:

```text
mvnw / mvnw.cmd downloads Maven automatically.
```

Usage:

```text
Windows:   mvnw.cmd clean install
Linux/Mac: ./mvnw clean install
```

This ensures:

```text
Everyone uses the SAME Maven version (3.9.16)
```

No "works on my machine" problems.

---

### .gitignore

Tells Git:

```text
DO NOT track these files:
──────────────────────────────────────────
target/        → Compiled code (regenerated every build)
.idea/         → IntelliJ settings (personal to each dev)
*.iml          → IntelliJ module files
*.class        → Compiled bytecode
```

Why?

```text
These files are generated automatically.
Tracking them causes merge conflicts.
They waste repository space.
```

---

---

## Folder 7: `.mvn/wrapper/` — Maven Wrapper Configuration

---

### Purpose

```text
Ensure consistent Maven version across all developers.
```

### File

| File | Purpose |
|------|---------|
| `maven-wrapper.properties` | Specifies Maven version: 3.9.16 |

---

### What Happens If This Folder Didn't Exist?

```text
Without .mvn/wrapper/:
    ↓
mvnw / mvnw.cmd won't work
    ↓
Each developer uses their own Maven version
    ↓
Developer A: Maven 3.8
Developer B: Maven 3.9
    ↓
Build works for A but fails for B
    ↓
"Works on my machine" problem
```

---

---

## Folder 8: `test/` — The Testing Layer

---

### Purpose

```text
Verify that the application works correctly.
```

### Files

| File | Purpose |
|------|---------|
| `AuthAppApplicationTests.java` | Smoke test — checks if app starts |

---

### Analogy

Think of a car factory:

```text
Before shipping a car:
    ↓
Does the engine start?        → Smoke test ✅
Does the steering work?       → Unit test
Does the whole car drive?     → Integration test
Can it handle a road trip?    → End-to-end test
```

Currently only the smoke test exists:

```text
"Does Spring Boot start without crashing?"
```

If it does:

```text
✅ PASS — basic wiring is correct
```

If it doesn't:

```text
❌ FAIL — something is fundamentally broken
```

---

### What Happens If This Folder Didn't Exist?

```text
Without tests:
    ↓
No way to verify code works
    ↓
Every change could break something
    ↓
You only find bugs when users report them
    ↓
Debugging in production
    ↓
Stress and sleepless nights
```

---

### How It Interacts With Other Folders

```text
test/ interacts with:
    ↓
ALL source folders  → Tests can test any class
    ↓
resources/          → Tests use test-specific config
    ↓
Maven               → mvn test runs all tests
```

---

---

## Folder 9: `Docs/` — Learning Documentation

---

### Purpose

```text
Document what you learn about each concept.
```

### Files

| File | Topic |
|------|-------|
| `01_Understand_HikariCP.md` | Connection pooling |
| `02_Understand_HikariCP.md` | Advanced HikariCP |
| `03_User_Java_File.md` | User entity explained |
| `04_Difference_Entity_and_Table.md` | @Entity vs @Table |
| `05_Enumerated_&_Table_Relation.md` | Enums and table relationships |
| `PROJECT_OVERVIEW.md` | This file — project structure |

---

### Analogy

```text
Docs/ is your personal textbook.
Written BY you, FOR you.
```

When you revisit this project 6 months later:

```text
Without Docs:  "What is HikariCP? Why did I use UUID?"
With Docs:     "Let me read my notes."
```

---

### What Happens If This Folder Didn't Exist?

```text
Without Docs/:
    ↓
Knowledge stays in your head
    ↓
You forget over time
    ↓
New team members have no reference
    ↓
Everyone asks the same questions
```

This folder is optional for the APPLICATION.

But essential for LEARNING.

---

---

## Folder 10: `target/` — Compiled Output

---

### Purpose

```text
Store compiled bytecode and packaged JARs.
```

---

### Analogy

```text
Source code (.java files) = Recipe
target/ (.class files)    = Cooked food

You write recipes (source code).
Maven cooks them (compiles).
The food goes in target/ (compiled output).
```

---

### What Happens If This Folder Didn't Exist?

```text
Nothing bad.
Maven recreates it every time you build.
```

You can safely delete target/:

```text
mvn clean
```

And rebuild:

```text
mvn install
```

This is why target/ is in .gitignore:

```text
Never commit compiled code to Git.
```

---

---

## Complete Folder Interaction Map

```text
                    ┌──────────────┐
                    │   Client     │
                    │ (Browser /   │
                    │  Postman)    │
                    └──────┬───────┘
                           │
                           │ HTTP Request (JSON)
                           │
                    ┌──────▼───────┐
                    │ controllers/ │
                    │  (MISSING)   │
                    │              │
                    │ Receives     │
                    │ requests,    │
                    │ sends        │
                    │ responses    │
                    └──────┬───────┘
                           │
                           │ calls (DTOs)
                           │
                    ┌──────▼───────┐
        ┌──────────│  services/   │──────────┐
        │          │              │          │
        │          │ Business     │          │
        │          │ Logic        │          │
        │          └──────┬───────┘          │
        │                 │                  │
   uses │                 │ calls            │ uses
        │                 │                  │
  ┌─────▼─────┐   ┌──────▼───────┐   ┌─────▼──────┐
  │   dtos/   │   │repositories/ │   │ entities/  │
  │           │   │              │   │            │
  │ Data      │   │ Database     │   │ Data       │
  │ Transfer  │   │ Access       │   │ Models     │
  │ Objects   │   │              │   │            │
  └───────────┘   └──────┬───────┘   └────────────┘
                         │
                         │ SQL Queries (auto-generated)
                         │
                  ┌──────▼───────┐
                  │   MySQL      │
                  │  Database    │
                  │              │
                  │ Port: 3306   │
                  └──────────────┘

  Configuration: resources/ (feeds into ALL layers)
  Build:         pom.xml + mvnw (manages dependencies)
  Tests:         test/ (validates ALL layers)
  Docs:          Docs/ (human reference)
```

---

## Summary Table: All Folders at a Glance

| # | Folder | Purpose | Analogy | Status |
|---|--------|---------|---------|--------|
| 1 | `entities/` | Define data models | Blueprints for a house | ✅ Complete |
| 2 | `dtos/` | Safe data transfer | Patient summary (no sensitive data) | ✅ Complete |
| 3 | `repositories/` | Database access | Librarian who finds/stores books | ✅ Complete |
| 4 | `services/` | Business logic | Chef who cooks the food | ⚠️ Stubs only |
| 5 | `resources/` | App configuration | Car mode settings | ✅ Complete |
| 6 | Root files | Project infrastructure | Shopping list + delivery truck | ✅ Complete |
| 7 | `.mvn/wrapper/` | Maven version control | Standard toolkit | ✅ Complete |
| 8 | `test/` | Code verification | Factory quality check | ⚠️ Basic only |
| 9 | `Docs/` | Learning documentation | Personal textbook | 📝 Growing |
| 10 | `target/` | Compiled output | Cooked food (from recipes) | 🔄 Auto-generated |

---

## What Needs to Be Built Next?

```text
Priority 1: Controller Layer
    ↓
    Create REST endpoints
    POST /api/users  (register)
    GET  /api/users  (list all)
    GET  /api/users/{id}  (get one)
    PUT  /api/users/{id}  (update)
    DELETE /api/users/{id}  (delete)

Priority 2: Service Implementation
    ↓
    Replace all "return null" with real logic
    Add Entity ↔ DTO conversion

Priority 3: Exception Handling
    ↓
    Create @ControllerAdvice
    Handle: UserNotFoundException, DuplicateEmailException, etc.

Priority 4: Security Configuration
    ↓
    Configure Spring Security
    Add JWT token generation
    Protect endpoints

Priority 5: Validation
    ↓
    Add @NotNull, @Email, @Size to DTOs
    Validate user input before processing
```

---

## Key Mental Model

Remember this:

```text
Entities define WHAT the data looks like.
DTOs define WHAT the client sees.
Repositories define HOW to access the database.
Services define WHAT to do with the data.
Controllers define HOW the client talks to the app.
Resources define HOW the app is configured.
Tests define WHETHER the app works correctly.
```

Each folder has ONE clear responsibility.

Each folder talks only to specific other folders.

This is the power of Layered Architecture.

> "A place for everything, and everything in its place."
