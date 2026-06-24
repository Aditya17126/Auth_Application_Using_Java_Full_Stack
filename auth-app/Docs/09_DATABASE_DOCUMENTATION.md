# Section 8: Database Documentation — From First Principles

Let's understand how the entire database layer works in this authentication application.

We will start from scratch and build up to the complete picture.

---

# What Problem Does The Database Solve?

Imagine you build a login system.

A user registers:

```text
Email: aditya@gmail.com
Password: secret123
```

Question:

> Where do we store this information?

If we store it in Java memory:

```text
Server restarts
    ↓
All data is lost
```

We need permanent storage.

Answer:

```text
Database
```

This application uses:

```text
MySQL
```

Running on:

```text
localhost:3306
```

Database name:

```text
auth_application_java
```

---

# How Java Talks To The Database

```text
Java Application
      ↓
Spring Data JPA
      ↓
Hibernate (ORM)
      ↓
JDBC Driver (mysql-connector-j)
      ↓
MySQL Database
```

Each layer has a purpose:

| Layer              | Purpose                                    |
| ------------------ | ------------------------------------------ |
| Spring Data JPA    | Provides Repository interfaces             |
| Hibernate          | Converts Java objects to SQL                |
| JDBC Driver        | Opens connections to MySQL                  |
| MySQL              | Stores data permanently on disk             |

---

# The Three Tables

This application creates exactly three tables:

```text
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    users     │     │  user_roles  │     │    roles     │
│──────────────│     │──────────────│     │──────────────│
│ user_id (PK) │────▶│ user_id (FK) │     │   id (PK)   │
│ user_email   │     │ role_id (FK) │◀────│   name      │
│ name         │     └──────────────┘     └──────────────┘
│ password     │
│ image        │
│ enable       │
│ created_at   │
│ updated_at   │
│ provider     │
└──────────────┘
```

Two of them are created explicitly:

```text
users     → from User.java
roles     → from Role.java
```

One is created automatically by Hibernate:

```text
user_roles → from @JoinTable annotation
```

---

# Table 1: users

## Source Code

```java
@Entity
@Table(name="users")
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
}
```

## Column Breakdown

| Java Field   | Column Name    | SQL Type           | Constraints              | Default      |
| ------------ | -------------- | ------------------ | ------------------------ | ------------ |
| `id`         | `user_id`      | `binary(16)` / `varchar(36)` | PRIMARY KEY, NOT NULL    | UUID auto    |
| `email`      | `user_email`   | `varchar(300)`     | UNIQUE, NOT NULL         | —            |
| `name`       | `name`         | `varchar(255)`     | —                        | —            |
| `password`   | `password`     | `varchar(255)`     | —                        | —            |
| `image`      | `image`        | `varchar(255)`     | —                        | —            |
| `enable`     | `enable`       | `bit(1)`           | —                        | `true`       |
| `createdAt`  | `created_at`   | `datetime(6)`      | —                        | `Instant.now()` |
| `updatedAt`  | `updated_at`   | `datetime(6)`      | —                        | `Instant.now()` |
| `provider`   | `provider`     | `varchar(255)`     | —                        | `LOCAL`      |

---

## How Column Names Are Decided

Question:

> Why is `createdAt` stored as `created_at`?

Hibernate uses a naming strategy:

```text
Java Field Name
      ↓
camelCase to snake_case
      ↓
Database Column Name
```

Examples:

```text
createdAt  → created_at
updatedAt  → updated_at
email      → email (no change, already lowercase)
```

But we can override this:

```java
@Column(name = "user_id")
private UUID id;
```

Without the override:

```text
id → id
```

With the override:

```text
id → user_id
```

And:

```java
@Column(name = "user_email")
private String email;
```

```text
email → user_email
```

---

## Why `unique = true` on Email?

```java
@Column(name = "user_email", unique = true, length = 300)
```

Question:

> Can two users have the same email?

Answer:

```text
No.
```

Think about it:

```text
aditya@gmail.com → registers
aditya@gmail.com → tries to register again
```

What should happen?

```text
Error: Email already exists.
```

The `unique = true` constraint tells MySQL:

```sql
ALTER TABLE users ADD CONSTRAINT UK_user_email UNIQUE (user_email);
```

If someone tries to insert a duplicate email:

```text
MySQL throws: Duplicate entry 'aditya@gmail.com' for key 'UK_user_email'
Hibernate catches this and throws: DataIntegrityViolationException
```

---

## Why `length = 300`?

Default `varchar` length in Hibernate:

```text
255
```

But some emails can be very long:

```text
very.long.email.address+with.labels@subdomain.example.co.uk
```

300 gives extra room.

---

# Table 2: roles

## Source Code

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

## Column Breakdown

| Java Field | Column Name | SQL Type           | Constraints                  | Default           |
| ---------- | ----------- | ------------------ | ---------------------------- | ----------------- |
| `id`       | `id`        | `binary(16)` / `varchar(36)` | PRIMARY KEY, NOT NULL        | `UUID.randomUUID()` |
| `name`     | `name`      | `varchar(255)`     | UNIQUE, NOT NULL             | —                 |

---

## Key Difference: UUID Generation

Notice something important.

In User:

```java
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

In Role:

```java
private UUID id = UUID.randomUUID();
```

What is the difference?

---

### User: Hibernate Generates The UUID

```text
User Object Created
      ↓
id = null (at this point)
      ↓
userRepository.save(user)
      ↓
Hibernate generates UUID
      ↓
Inserts into database
```

Hibernate controls when and how the UUID is created.

---

### Role: Java Generates The UUID

```text
Role Object Created
      ↓
id = UUID.randomUUID() (immediately)
      ↓
roleRepository.save(role)
      ↓
Hibernate uses the existing UUID
      ↓
Inserts into database
```

Java creates the UUID the moment the object is instantiated.

---

### When To Use Which?

| Strategy                                  | Use When                                      |
| ----------------------------------------- | --------------------------------------------- |
| `@GeneratedValue(strategy = UUID)`        | Want Hibernate to manage ID lifecycle          |
| `UUID.randomUUID()` in field initializer  | Want ID available before saving to database    |

---

## Why `nullable = false` on Role Name?

```java
@Column(unique = true, nullable = false)
private String name;
```

Question:

> Can a role exist without a name?

Answer:

```text
No.
```

A role like:

```text
name = null
```

makes no sense.

Every role MUST have a name:

```text
USER
ADMIN
MANAGER
```

`nullable = false` tells MySQL:

```sql
name VARCHAR(255) NOT NULL
```

---

# Table 3: user_roles (Join Table)

This table is NOT created from a Java class.

It is created by the `@JoinTable` annotation:

```java
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
```

## Column Breakdown

| Column Name | SQL Type           | Constraints            | References     |
| ----------- | ------------------ | ---------------------- | -------------- |
| `user_id`   | `binary(16)` / `varchar(36)` | FOREIGN KEY, NOT NULL  | users(user_id) |
| `role_id`   | `binary(16)` / `varchar(36)` | FOREIGN KEY, NOT NULL  | roles(id)      |

Together these two columns form a:

```text
Composite Primary Key
```

Meaning:

```text
The combination of (user_id, role_id) must be unique.
```

---

## Why Does This Table Exist?

Databases cannot store:

```text
Aditya → [USER, ADMIN]
```

inside a single cell.

Relational databases are flat:

```text
One value per cell.
```

So we need a separate table to store the connection:

```text
user_id        | role_id
───────────────┼──────────────
aditya-uuid    | user-role-uuid
aditya-uuid    | admin-role-uuid
rahul-uuid     | user-role-uuid
```

---

## Visual Analogy

Think of it like a friendship list:

```text
Friendship List
───────────────────
Person     | Friend
───────────────────
Aditya     | Rahul
Aditya     | Priya
Rahul      | Priya
```

Similarly:

```text
user_roles
───────────────────
User       | Role
───────────────────
Aditya     | USER
Aditya     | ADMIN
Rahul      | USER
```

---

# Entity-Relationship Diagram

```text
┌─────────────────────────────────────────────────────────┐
│                    ER DIAGRAM                           │
│                                                         │
│  ┌─────────────┐          ┌─────────────┐               │
│  │   USERS     │          │   ROLES     │               │
│  │─────────────│          │─────────────│               │
│  │ *user_id PK │──┐   ┌──│ *id PK      │               │
│  │  user_email │  │   │  │  name       │               │
│  │  name       │  │   │  └─────────────┘               │
│  │  password   │  │   │                                 │
│  │  image      │  │   │                                 │
│  │  enable     │  │   │  ┌──────────────┐               │
│  │  created_at │  └──▶│  USER_ROLES   │               │
│  │  updated_at │      │──────────────│               │
│  │  provider   │      │ *user_id FK  │◀──┐            │
│  └─────────────┘      │ *role_id FK  │───┘            │
│                        └──────────────┘               │
│                                                         │
│  Relationship: Many-to-Many                             │
│  One User can have Many Roles                           │
│  One Role can belong to Many Users                      │
│                                                         │
│  * = Primary Key                                        │
│  FK = Foreign Key                                       │
│  PK = Primary Key                                       │
└─────────────────────────────────────────────────────────┘
```

---

# The ManyToMany Relationship — Deep Dive

```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles = new HashSet<>();
```

Let's break this down word by word.

---

## @ManyToMany

Tells Hibernate:

```text
Many Users  ←→  Many Roles
```

This is a bidirectional concept:

```text
Direction 1: One User can have many Roles
Direction 2: One Role can belong to many Users
```

In our code, only the User side defines the relationship.

This makes User the:

```text
Owning Side
```

The owning side controls the join table.

---

## @JoinTable(name = "user_roles")

Tells Hibernate:

```text
Create a table called "user_roles" to store the relationship.
```

---

## joinColumns = @JoinColumn(name = "user_id")

Question:

> Which column in user_roles points BACK to the users table?

Answer:

```text
user_id
```

---

## inverseJoinColumns = @JoinColumn(name = "role_id")

Question:

> Which column in user_roles points to the OTHER table (roles)?

Answer:

```text
role_id
```

---

## Visual Flow

```text
User Object
     │
     │ has a Set<Role>
     ↓
Hibernate reads @ManyToMany
     │
     │ reads @JoinTable
     ↓
Creates/Uses user_roles table
     │
     │ joinColumns → user_id (points to User)
     │ inverseJoinColumns → role_id (points to Role)
     ↓
Stores relationships as rows
```

---

# FetchType.EAGER vs FetchType.LAZY

```java
@ManyToMany(fetch = FetchType.EAGER)
```

This is one of the most important decisions in JPA.

---

## What is FetchType?

Question:

> When we load a User from the database, should we ALSO load all their Roles?

Two possible answers:

---

### FetchType.EAGER

```text
Load everything immediately.
```

When you do:

```java
User user = userRepository.findById(id).get();
```

Hibernate runs:

```text
Query 1: SELECT * FROM users WHERE user_id = ?
Query 2: SELECT * FROM roles JOIN user_roles ON ... WHERE user_id = ?
```

Both queries execute immediately.

The `user.getRoles()` set is already filled.

---

### FetchType.LAZY (Default for @ManyToMany)

```text
Load only when accessed.
```

When you do:

```java
User user = userRepository.findById(id).get();
```

Hibernate runs:

```text
Query 1: SELECT * FROM users WHERE user_id = ?
```

Only ONE query.

When you later call:

```java
user.getRoles();  // accessing roles
```

THEN Hibernate runs:

```text
Query 2: SELECT * FROM roles JOIN user_roles ON ... WHERE user_id = ?
```

---

### Comparison Table

| Feature              | EAGER                          | LAZY                              |
| -------------------- | ------------------------------ | --------------------------------- |
| When data loads      | Immediately with parent        | Only when accessed                |
| Number of queries    | Multiple upfront               | On-demand                         |
| Memory usage         | Higher (everything loaded)     | Lower (loaded as needed)          |
| Risk                 | N+1 problem if many relations  | LazyInitializationException risk  |
| Best for             | Small related data, always needed | Large data, rarely needed      |

---

### Why Our App Uses EAGER

```java
@ManyToMany(fetch = FetchType.EAGER)
```

In an authentication system:

```text
Every time we load a User
    ↓
We ALWAYS need their Roles
    ↓
To check permissions
```

Example:

```text
User logs in
    ↓
Load user from database
    ↓
Check: Does user have ADMIN role?
    ↓
Roles MUST be available immediately
```

So EAGER makes sense here.

---

### LazyInitializationException — The Most Common JPA Error

If we used LAZY:

```java
@ManyToMany(fetch = FetchType.LAZY)
```

And tried to access roles OUTSIDE a transaction:

```java
User user = userRepository.findById(id).get();
// Transaction closes here

user.getRoles();  // BOOM! LazyInitializationException
```

Why?

```text
Hibernate Session is closed.
It cannot run the second query.
```

EAGER avoids this problem entirely.

---

# UUID Strategy — Deep Dive

## What is UUID?

```text
UUID = Universally Unique Identifier
```

Example:

```text
550e8400-e29b-41d4-a716-446655440000
```

It has:

```text
32 hexadecimal characters
4 hyphens
128 bits total
```

---

## UUID vs Auto-Increment

Most tutorials use:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

This generates:

```text
1, 2, 3, 4, 5, ...
```

Our app uses:

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

This generates:

```text
550e8400-e29b-41d4-a716-446655440000
7c9e6679-7425-40de-944b-e07fc1f90ae7
```

---

## Comparison

| Feature              | Auto-Increment (Long)           | UUID                              |
| -------------------- | ------------------------------- | --------------------------------- |
| Format               | `1, 2, 3, 4`                   | `550e8400-e29b-...`               |
| Predictable?         | Yes (next is current + 1)       | No (random)                       |
| Security             | Attacker can guess IDs          | Very hard to guess                |
| Distributed systems  | Conflicts on multiple servers   | Globally unique                   |
| Storage size         | 8 bytes                         | 16 bytes                          |
| Index performance    | Better (sequential)             | Slightly worse (random)           |
| URL appearance       | `/users/5`                      | `/users/550e8400-...`             |

---

## Why UUID For Auth App?

Security:

```text
Auto-Increment: /api/users/1
Attacker tries:  /api/users/2
                 /api/users/3
                 /api/users/4
```

Easily enumerable.

UUID:

```text
/api/users/550e8400-e29b-41d4-a716-446655440000
```

Attacker cannot guess the next user's ID.

For an authentication application:

```text
Security > Performance
```

UUID is the right choice.

---

# @PrePersist and @PreUpdate — Lifecycle Callbacks

## Source Code

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

---

## What Are Lifecycle Callbacks?

They are methods that Hibernate calls automatically at specific moments.

Think of them like event listeners:

```text
Event: "About to save a NEW entity"
    ↓
Hibernate calls: @PrePersist method

Event: "About to UPDATE an existing entity"
    ↓
Hibernate calls: @PreUpdate method
```

---

## @PrePersist — Before First Save

When does it fire?

```text
userRepository.save(newUser)
    ↓
Hibernate detects: This user does NOT exist in DB
    ↓
Hibernate calls: onCreate()
    ↓
Then executes: INSERT INTO users ...
```

What does it do?

```java
Instant now = Instant.now();
if(createdAt == null) createdAt = now;
updatedAt = now;
```

Step by step:

```text
1. Get current timestamp
2. If createdAt is null, set it to now
3. Always set updatedAt to now
```

Why check `if(createdAt == null)`?

Because the field has a default:

```java
private Instant createdAt = Instant.now();
```

When the User object is created in Java:

```text
createdAt = Instant.now()  (already set by field initializer)
```

When @PrePersist fires:

```text
createdAt is NOT null
    ↓
Keep the original value
```

This prevents overwriting the creation time.

---

## @PreUpdate — Before Every Update

When does it fire?

```text
user.setName("New Name");
userRepository.save(existingUser);
    ↓
Hibernate detects: This user ALREADY exists in DB
    ↓
Hibernate calls: onUpdate()
    ↓
Then executes: UPDATE users SET ... WHERE user_id = ?
```

What does it do?

```java
updatedAt = Instant.now();
```

Simply updates the timestamp to "right now."

---

## Complete Lifecycle Timeline

```text
User Object Created in Java
│  createdAt = Instant.now()  (field initializer)
│  updatedAt = Instant.now()  (field initializer)
│
▼
userRepository.save(user)
│
├── Is this a NEW entity? (no ID in DB yet)
│   ├── YES → @PrePersist fires → onCreate()
│   │         createdAt stays (not null)
│   │         updatedAt = now
│   │         Then → INSERT
│   │
│   └── NO  → @PreUpdate fires → onUpdate()
│             updatedAt = now
│             Then → UPDATE
│
▼
Entity saved in Database
```

---

# The Repository Layer

## UserRepository

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

## What is JpaRepository?

```text
JpaRepository<User, UUID>
```

This means:

```text
Entity Type: User
Primary Key Type: UUID
```

JpaRepository gives you these methods FOR FREE:

| Method                | Purpose                     |
| --------------------- | --------------------------- |
| `save(entity)`        | Insert or Update            |
| `findById(id)`        | Find by primary key         |
| `findAll()`           | Get all entities            |
| `deleteById(id)`      | Delete by primary key       |
| `count()`             | Count total rows            |
| `existsById(id)`      | Check if ID exists          |

You write ZERO SQL for these.

---

## Custom Query Methods

### findByEmail

```java
Optional<User> findByEmail(String email);
```

Spring Data JPA reads the method name:

```text
findBy  →  SELECT ... WHERE
Email   →  user_email = ?
```

So this method name becomes:

```sql
SELECT * FROM users WHERE user_email = ?
```

Why `Optional<User>`?

Because the user might NOT exist:

```text
findByEmail("unknown@gmail.com")
    ↓
Optional.empty()

findByEmail("aditya@gmail.com")
    ↓
Optional.of(user)
```

---

### existsByEmail

```java
boolean existsByEmail(String email);
```

Spring Data JPA reads:

```text
existsBy  →  SELECT COUNT(*) > 0 FROM ... WHERE
Email     →  user_email = ?
```

Returns:

```text
true  → email exists
false → email does not exist
```

This is commonly used during registration:

```text
User wants to register
    ↓
existsByEmail("aditya@gmail.com")
    ↓
true? → "Email already taken"
false? → Proceed with registration
```

---

# Hibernate Generated SQL — Every Query Explained

Here is every SQL query that Hibernate generates for our application.

These are the ACTUAL queries that run against MySQL.

---

## 1. DDL — Table Creation (On Application Startup)

When Spring Boot starts with `ddl-auto: update`:

### Create users table

```sql
CREATE TABLE users (
    user_id BINARY(16) NOT NULL,
    user_email VARCHAR(300),
    name VARCHAR(255),
    password VARCHAR(255),
    image VARCHAR(255),
    enable BIT(1),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    provider VARCHAR(255),
    PRIMARY KEY (user_id)
) ENGINE=InnoDB;

ALTER TABLE users
    ADD CONSTRAINT UK_user_email UNIQUE (user_email);
```

### Create roles table

```sql
CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

ALTER TABLE roles
    ADD CONSTRAINT UK_roles_name UNIQUE (name);
```

### Create user_roles join table

```sql
CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB;

ALTER TABLE user_roles
    ADD CONSTRAINT FK_user_roles_user_id
    FOREIGN KEY (user_id) REFERENCES users(user_id);

ALTER TABLE user_roles
    ADD CONSTRAINT FK_user_roles_role_id
    FOREIGN KEY (role_id) REFERENCES roles(id);
```

---

## 2. INSERT User

When:

```java
userRepository.save(newUser);
```

Hibernate generates:

```sql
INSERT INTO users (
    user_id,
    user_email,
    name,
    password,
    image,
    enable,
    created_at,
    updated_at,
    provider
) VALUES (
    ?,    -- UUID: 550e8400-e29b-41d4-a716-446655440000
    ?,    -- 'aditya@gmail.com'
    ?,    -- 'Aditya'
    ?,    -- 'secret123' (plain text — BAD, should be hashed)
    ?,    -- 'https://example.com/photo.jpg'
    ?,    -- 1 (true)
    ?,    -- '2026-06-20 12:48:00.000000'
    ?,    -- '2026-06-20 12:48:00.000000'
    ?     -- 'LOCAL'
);
```

Step by step:

```text
1. @PrePersist fires → sets createdAt and updatedAt
2. Hibernate generates UUID (if using @GeneratedValue)
3. INSERT query executes
4. User is now in the database
```

---

## 3. SELECT User By Email (findByEmail)

When:

```java
userRepository.findByEmail("aditya@gmail.com");
```

Hibernate generates:

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
WHERE u.user_email = ?;
```

Because `FetchType.EAGER` is set, Hibernate ALSO runs:

```sql
SELECT
    r.id,
    r.name
FROM user_roles ur
INNER JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = ?;
```

Two queries:

```text
Query 1: Get the user
Query 2: Get the user's roles (because EAGER)
```

---

## 4. SELECT User By ID (findById)

When:

```java
userRepository.findById(uuid);
```

Hibernate generates:

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
WHERE u.user_id = ?;
```

Plus the EAGER roles query:

```sql
SELECT
    r.id,
    r.name
FROM user_roles ur
INNER JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = ?;
```

---

## 5. Check If Email Exists (existsByEmail)

When:

```java
userRepository.existsByEmail("aditya@gmail.com");
```

Hibernate generates:

```sql
SELECT
    COUNT(*) > 0
FROM users u
WHERE u.user_email = ?;
```

Or alternatively:

```sql
SELECT
    1
FROM users u
WHERE u.user_email = ?
LIMIT 1;
```

Returns:

```text
true  → at least one row found
false → no rows found
```

This query is very efficient because:

```text
1. It does NOT load the entire User object
2. It just checks existence
3. The UNIQUE index on user_email makes it fast
```

---

## 6. INSERT Into user_roles

When a User with Roles is saved:

```java
User user = User.builder()
    .email("aditya@gmail.com")
    .name("Aditya")
    .build();

Role userRole = new Role();
userRole.setName("USER");

user.getRoles().add(userRole);

userRepository.save(user);
```

Hibernate generates:

```sql
-- First: Insert the user
INSERT INTO users (user_id, user_email, name, ...) VALUES (?, ?, ?, ...);

-- Then: Insert the role (if not already present)
INSERT INTO roles (id, name) VALUES (?, ?);

-- Finally: Insert the relationship
INSERT INTO user_roles (user_id, role_id) VALUES (?, ?);
```

The join table insertion:

```sql
INSERT INTO user_roles (user_id, role_id)
VALUES (
    ?,    -- User's UUID
    ?     -- Role's UUID
);
```

---

## 7. SELECT All Users (findAll)

When:

```java
userRepository.findAll();
```

Hibernate generates:

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
FROM users u;
```

Because of EAGER fetch, for EACH user Hibernate also runs:

```sql
SELECT
    r.id,
    r.name
FROM user_roles ur
INNER JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = ?;
```

If you have 100 users:

```text
1 query for users
+ 100 queries for roles (one per user)
= 101 queries total
```

This is called the:

```text
N+1 Problem
```

---

### N+1 Problem Explained

```text
Query 1 (the "1"):
    SELECT * FROM users;
    → Returns 100 users

Query 2 to 101 (the "N"):
    For each of the 100 users:
    SELECT roles for user_id = ?
```

Total: 101 queries.

This is bad for performance.

Solution:

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.roles")
List<User> findAllWithRoles();
```

This uses a JOIN FETCH:

```sql
SELECT u.*, r.*
FROM users u
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id;
```

One single query instead of 101.

---

## 8. DELETE User

When:

```java
userRepository.deleteById(uuid);
```

Hibernate generates:

```sql
-- First: Delete the user's role associations
DELETE FROM user_roles WHERE user_id = ?;

-- Then: Delete the user
DELETE FROM users WHERE user_id = ?;
```

Why two queries?

```text
user_roles has a FOREIGN KEY pointing to users.
You cannot delete a user while rows in user_roles reference them.
So Hibernate deletes the join table entries first.
```

Order matters:

```text
Step 1: Remove relationships  →  DELETE FROM user_roles
Step 2: Remove the user       →  DELETE FROM users
```

---

# Database Configuration

## application-dev.yml

```yaml
server:
  port: 8083

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

## Configuration Breakdown

| Property           | Value                                          | Meaning                                    |
| ------------------ | ---------------------------------------------- | ------------------------------------------ |
| `datasource.url`   | `jdbc:mysql://localhost:3306/auth_application_java` | MySQL on localhost, port 3306, database name |
| `username`         | `root`                                         | MySQL username                             |
| `password`         | `Aditya13325@`                                 | MySQL password (hardcoded — security risk) |
| `ddl-auto`         | `update`                                       | Auto-create/update tables on startup       |
| `dialect`          | `MySQLDialect`                                 | Tells Hibernate to generate MySQL-specific SQL |
| `format_sql`       | `true`                                         | Pretty-print SQL in console                |
| `show_sql`         | `true`                                         | Log all SQL to console                     |

---

## ddl-auto Options Explained

| Value          | Behavior                                            | Use In           |
| -------------- | --------------------------------------------------- | ---------------- |
| `none`         | Do nothing. Tables must exist.                      | Production       |
| `validate`     | Check if tables match entities. Error if mismatch.  | Production       |
| `update`       | Create missing tables/columns. Never drops.         | Development      |
| `create`       | Drop all tables, recreate on startup.               | Testing          |
| `create-drop`  | Drop all tables on shutdown.                        | Testing          |

Our app uses `update`:

```text
Spring Boot Starts
    ↓
Hibernate compares Entity classes with existing tables
    ↓
Missing table? → CREATE TABLE
Missing column? → ALTER TABLE ADD COLUMN
Extra column in DB but not in Entity? → Ignore (never drops)
    ↓
Application Ready
```

---

# How The Service Layer Uses The Repository

## UserService Interface

```java
public interface UserService {
    UserDto createUser(UserDto userDto);
    UserDto getUserByEmail(String email);
    UserDto updateUSer(UserDto userDto, String userId);
    void deleteUser(String userId);
    UserDto getUserById(String userId);
    Iterable<UserDto> getAllUsers();
}
```

## Data Flow

```text
Controller (HTTP Request)
     ↓
Service Layer (Business Logic)
     ↓
Repository (Database Operations)
     ↓
Hibernate (SQL Generation)
     ↓
MySQL (Data Storage)
```

Each service method maps to repository calls:

| Service Method       | Repository Method Called               | SQL Operation          |
| -------------------- | -------------------------------------- | ---------------------- |
| `createUser()`       | `userRepository.save()`               | INSERT                 |
| `getUserByEmail()`   | `userRepository.findByEmail()`        | SELECT WHERE email=?   |
| `updateUser()`       | `userRepository.save()`               | UPDATE                 |
| `deleteUser()`       | `userRepository.deleteById()`         | DELETE                 |
| `getUserById()`      | `userRepository.findById()`           | SELECT WHERE id=?      |
| `getAllUsers()`       | `userRepository.findAll()`            | SELECT ALL             |

---

# The Provider Enum and Database Storage

```java
public enum Provider {
   LOCAL,
   GOOGLE,
   GITHUB,
   FACEBOOK
}
```

Stored using:

```java
@Enumerated(EnumType.STRING)
private Provider provider = Provider.LOCAL;
```

In the database:

| user_id          | user_email         | provider |
| ---------------- | ------------------ | -------- |
| 550e...          | aditya@gmail.com   | LOCAL    |
| 7c9e...          | rahul@gmail.com    | GOOGLE   |
| a3f2...          | priya@gmail.com    | GITHUB   |

The provider column stores the actual string:

```text
LOCAL
GOOGLE
GITHUB
FACEBOOK
```

Not numbers like 0, 1, 2, 3.

---

# Complete Database Flow — Registration Example

Let's trace what happens when a new user registers:

```text
Step 1: User fills registration form
        Email: aditya@gmail.com
        Password: secret123
        Name: Aditya

Step 2: HTTP POST /api/users
        ↓
Step 3: Controller receives UserDto
        ↓
Step 4: Service layer processes
        ↓
Step 5: existsByEmail("aditya@gmail.com")
        → SQL: SELECT COUNT(*) > 0 FROM users WHERE user_email = ?
        → Result: false (email not taken)
        ↓
Step 6: Create User entity from UserDto
        ↓
Step 7: userRepository.save(user)
        ↓
Step 8: @PrePersist fires
        → createdAt set (if null)
        → updatedAt set
        ↓
Step 9: Hibernate generates UUID
        ↓
Step 10: SQL: INSERT INTO users (...) VALUES (...)
        ↓
Step 11: SQL: INSERT INTO user_roles (...) VALUES (...)
         (if roles were assigned)
        ↓
Step 12: User saved in MySQL
        ↓
Step 13: Response sent back to client
```

---

# Interview Questions and Answers

## Q1: Why use UUID instead of auto-increment?

```text
UUID provides:
1. Global uniqueness across distributed systems
2. Security — IDs are not guessable
3. No collision when merging databases

Auto-increment provides:
1. Better index performance
2. Smaller storage (8 bytes vs 16 bytes)
3. Human-readable IDs

For auth apps, UUID is better because of security.
```

## Q2: What is the N+1 problem?

```text
When loading a list of entities with EAGER relationships,
Hibernate runs:
  1 query for the parent entities
  N queries for each parent's related entities

Solution: Use JOIN FETCH or @EntityGraph
```

## Q3: What is the difference between @PrePersist and @PreUpdate?

```text
@PrePersist: Called before INSERT (first save)
@PreUpdate:  Called before UPDATE (subsequent saves)
```

## Q4: Why use Set instead of List for roles?

```text
Set prevents duplicate entries.
A user should not have the same role twice.
Set enforces this at the Java level.
```

## Q5: What happens if ddl-auto is set to "create" in production?

```text
ALL tables are dropped and recreated on every startup.
ALL data is permanently lost.
Never use "create" or "create-drop" in production.
Use "validate" or "none" instead.
```

---

# Summary

```text
Database Layer Architecture
───────────────────────────

Tables:
  users          → Stores user information
  roles          → Stores role definitions
  user_roles     → Maps users to roles (join table)

Keys:
  users.user_id  → Primary Key (UUID)
  roles.id       → Primary Key (UUID)
  user_roles     → Composite Key (user_id + role_id)

Constraints:
  users.user_email  → UNIQUE
  roles.name        → UNIQUE, NOT NULL

Relationships:
  User ←→ Role    → Many-to-Many (via user_roles)
  FetchType        → EAGER (roles loaded with user)

Lifecycle:
  @PrePersist      → Sets timestamps on first save
  @PreUpdate       → Updates timestamp on every update

Repository:
  findByEmail()    → SELECT WHERE user_email = ?
  existsByEmail()  → COUNT check for existence
  save()           → INSERT or UPDATE
  findById()       → SELECT WHERE user_id = ?
  findAll()        → SELECT ALL
  deleteById()     → DELETE (join table first, then user)
```
