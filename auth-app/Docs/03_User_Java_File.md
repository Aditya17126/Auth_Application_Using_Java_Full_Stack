# Understanding the User Entity from First Principles

Let's understand this class as if we are building an authentication system from scratch.

```java
@Entity
@Table(name="users")
public class User {

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

# First Principles: What is an Entity?

Suppose you're building an authentication application.

Question:

> What is the most important thing we want to store?

Answer:

```text
Users
```

Every user has information:

* Email
* Name
* Password
* Profile Image
* Roles
* Login Provider

Java stores this information in an object.

Database stores this information in a table.

JPA connects these two worlds.

```text
Java Object
     ↓
JPA/Hibernate
     ↓
Database Table
```

This process is called:

```text
ORM (Object Relational Mapping)
```

---

# @Entity

```java
@Entity
```

Means:

> "Hibernate, this Java class should be stored in the database."

Without it:

```text
Java sees:
User is just a normal class.
```

With it:

```text
Hibernate sees:
User should become a database table.
```

Think of it like this:

```text
@Entity
↓
"This class is important.
Save it permanently."
```

---

# @Table(name="users")

```java
@Table(name="users")
```

Means:

> "Create or use a table called users."

Without this:

Hibernate would usually create:

```text
user
```

or

```text
User
```

depending on naming strategy.

With this:

```text
Table Name = users
```

Database:

| users    |
| -------- |
| id       |
| email    |
| password |

---

# UUID id

```java
private UUID id;
```

Question:

> How do we uniquely identify users?

Suppose two users have the same name:

```text
Aditya
Aditya
```

How do we know who is who?

We use an ID.

Most beginners see:

```java
Long id;
```

Example:

```text
1
2
3
4
```

But UUID generates:

```text
550e8400-e29b-41d4-a716-446655440000
```

Very difficult to guess.

---

## Why UUID?

Suppose your application runs on multiple servers.

Server A:

```text
Creates User 1
```

Server B:

```text
Creates User 1
```

Collision happens.

UUID solves this.

```text
Every user gets a globally unique ID.
```

---

# Email

```java
private String email;
```

Stores:

```text
aditya@gmail.com
```

Purpose:

```text
Login
Communication
Uniqueness
```

---

# Name

```java
private String name;
```

Stores:

```text
Aditya
```

Purpose:

```text
Display Name
```

---

# Password

```java
private String password;
```

Stores:

```text
User Credentials
```

Example:

User enters:

```text
Aditya123
```

IMPORTANT:

Never store plain passwords.

Bad:

```text
Aditya123
```

Good:

```text
$2a$10$8a6...
```

using BCrypt hashing.

---

# Image

```java
private String image;
```

Stores:

```text
https://...
```

or

```text
profile.jpg
```

Purpose:

```text
Profile Picture
```

---

# enable

```java
private boolean enable = true;
```

Question:

> Can the user access the application?

Example:

```java
enable = true;
```

Means:

```text
Account Active
```

Example:

```java
enable = false;
```

Means:

```text
Account Disabled
```

Real Life:

Admin blocks a user.

```text
enable = false
```

Login denied.

---

# createdAt

```java
private Instant createdAt = Instant.now();
```

Question:

> When was this account created?

Example:

```text
2025-07-20T10:15:00Z
```

Stores:

```text
Account Creation Time
```

---

# updatedAt

```java
private Instant updatedAt = Instant.now();
```

Question:

> When was this account last modified?

Example:

User changes profile picture.

```text
updatedAt changes.
```

Purpose:

```text
Audit Information
```

---

# Provider

```java
private Provider provider = Provider.LOCAL;
```

Question:

> How did the user register?

Possible answers:

```text
LOCAL
GOOGLE
GITHUB
FACEBOOK
```

Example:

Normal Signup:

```text
Email + Password
↓
LOCAL
```

Google Login:

```text
Sign in with Google
↓
GOOGLE
```

GitHub Login:

```text
Continue with GitHub
↓
GITHUB
```

---

# Roles

```java
private Set<Role> roles = new HashSet<>();
```

Question:

> What permissions does this user have?

Examples:

```text
USER
ADMIN
MANAGER
```

Suppose:

Aditya:

```text
USER
```

System Administrator:

```text
ADMIN
USER
```

Why Set?

Because:

```text
No Duplicate Roles
```

Bad:

```text
ADMIN
ADMIN
ADMIN
```

Good:

```text
ADMIN
USER
```

---

# Why HashSet?

```java
new HashSet<>()
```

Because Set guarantees:

```text
Unique Values
```

Example:

Adding:

```java
roles.add(ADMIN);
roles.add(ADMIN);
```

Result:

```text
Only One ADMIN
```

---

# Lombok Annotations

Without Lombok:

You would write:

```java
getters
setters
constructors
builder methods
```

manually.

Hundreds of lines.

Lombok generates them automatically.

---

## @Getter

```java
@Getter
```

Generates:

```java
getEmail()
getName()
getPassword()
```

---

## @Setter

```java
@Setter
```

Generates:

```java
setEmail(...)
setName(...)
```

---

## @NoArgsConstructor

```java
@NoArgsConstructor
```

Generates:

```java
User user = new User();
```

Equivalent to:

```java
public User() {
}
```

---

## @AllArgsConstructor

```java
@AllArgsConstructor
```

Generates:

```java
public User(
    UUID id,
    String email,
    ...
)
```

Constructor with all fields.

---

## @Builder

Builder solves the problem of large constructors.

Without Builder:

```java
User user = new User(
    id,
    email,
    name,
    password,
    image,
    ...
);
```

Hard to read.

With Builder:

```java
User user = User.builder()
    .email("aditya@gmail.com")
    .name("Aditya")
    .password("secret")
    .build();
```

Much cleaner.

---

# What Happens When Spring Boot Starts?

```text
Spring Boot Starts
       ↓
Hibernate Scans Classes
       ↓
Finds @Entity User
       ↓
Creates/Updates users Table
       ↓
Maps Fields to Columns
       ↓
Application Ready
```

---

# Mental Model

Think of User as a "User Registration Form."

```text
Registration Form
-----------------------
ID
Email
Name
Password
Image
Enabled?
Created At
Updated At
Provider
Roles
```

Java stores this form as an object.

Hibernate stores this form as a database row.

```text
User Object
      ↓
Hibernate
      ↓
users Table
```

This is the essence of JPA and Hibernate:

> "Model your business using Java objects, and let Hibernate persist them into the database."
