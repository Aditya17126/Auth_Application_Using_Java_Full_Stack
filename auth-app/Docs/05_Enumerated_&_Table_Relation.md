# Understanding `@Enumerated` and Connecting Two Tables from First Principles

Let's understand this from the very beginning.

You have two classes:

```java
User
```

and

```java
Role
```

and you want to understand:

1. What is `@Enumerated`?
2. How are two Java classes connected?
3. How are two database tables connected?
4. How do we decide which relationship to use?

---

# Part 1: What is @Enumerated?

You have:

```java
@Enumerated(EnumType.STRING)
private Provider provider = Provider.LOCAL;
```

and

```java
public enum Provider {
    LOCAL,
    GOOGLE,
    GITHUB,
    FACEBOOK
}
```

---

## First Principles

Question:

> How should Hibernate store an enum in the database?

Remember:

Java understands:

```java
Provider.GOOGLE
```

MySQL does not understand Java enums.

MySQL only understands values like:

```text
1
2
GOOGLE
true
abc
```

So Hibernate has to convert the enum.

---

## Option 1: EnumType.ORDINAL

Suppose:

```java
public enum Provider {
    LOCAL,
    GOOGLE,
    GITHUB,
    FACEBOOK
}
```

Java internally gives positions:

```text
LOCAL      → 0
GOOGLE     → 1
GITHUB     → 2
FACEBOOK   → 3
```

Database:

| provider |
| -------- |
| 1        |

means:

```text
GOOGLE
```

---

### Problem

Suppose later you change:

```java
public enum Provider {
    LOCAL,
    FACEBOOK,
    GOOGLE,
    GITHUB
}
```

Now:

```text
FACEBOOK → 1
GOOGLE   → 2
```

Your old data becomes wrong.

---

## Option 2: EnumType.STRING

```java
@Enumerated(EnumType.STRING)
private Provider provider;
```

Database stores:

| provider |
| -------- |
| GOOGLE   |

instead of:

| provider |
| -------- |
| 1        |

This is much safer.

---

# Mental Model

```text
Java Enum
     ↓
Hibernate Translator
     ↓
Database Value
```

STRING means:

```text
Store the actual word.
```

ORDINAL means:

```text
Store the position number.
```

Most developers use:

```java
EnumType.STRING
```

---

# Part 2: Why Connect Two Tables?

Question:

Can one user have multiple roles?

Example:

Aditya:

```text
USER
```

Admin:

```text
USER
ADMIN
```

Question:

Can one role belong to multiple users?

Example:

```text
USER Role
```

belongs to:

```text
Aditya
Rahul
Priya
```

Answer:

YES.

---

# Visual Representation

```text
Users
------
Aditya
Rahul

Roles
------
USER
ADMIN
```

Aditya:

```text
USER
ADMIN
```

Rahul:

```text
USER
```

---

# What Relationship Is This?

One User:

```text
Many Roles
```

One Role:

```text
Many Users
```

Therefore:

```text
Many-to-Many
```

---

# How Databases Solve This

Databases cannot directly store:

```text
Aditya → USER, ADMIN
```

inside a single column.

Instead they create a third table.

---

## users

| user_id | email                                       |
| ------- | ------------------------------------------- |
| 1       | [aditya@gmail.com](mailto:aditya@gmail.com) |
| 2       | [rahul@gmail.com](mailto:rahul@gmail.com)   |

---

## roles

| role_id | name  |
| ------- | ----- |
| 10      | USER  |
| 20      | ADMIN |

---

## user_roles

| user_id | role_id |
| ------- | ------- |
| 1       | 10      |
| 1       | 20      |
| 2       | 10      |

This table is called:

```text
Join Table
```

---

# How JPA Represents This

User:

```java
@ManyToMany
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles;
```

---

## What Does @ManyToMany Mean?

It means:

```text
One User
    ↓
Can Have Many Roles

One Role
    ↓
Can Belong To Many Users
```

---

# What Does @JoinTable Mean?

Hibernate asks:

> Which table should connect them?

You answer:

```java
@JoinTable(name = "user_roles")
```

Means:

```text
Use the table named:

user_roles
```

---

# What Are joinColumns?

```java
joinColumns = @JoinColumn(name = "user_id")
```

Means:

```text
Inside user_roles,

this column points to User.
```

---

# What Are inverseJoinColumns?

```java
inverseJoinColumns =
    @JoinColumn(name = "role_id")
```

Means:

```text
Inside user_roles,

this column points to Role.
```

---

# Visual Flow

```text
User
 ↓
@ManyToMany
 ↓
user_roles
 ↓
Role
```

---

# Should Role Be Named user_roles?

You currently have:

```java
@Entity
@Table(name = "user_roles")
public class Role {
```

This is NOT correct.

Because Role represents:

```text
Roles
```

like:

```text
USER
ADMIN
MANAGER
```

Its table should be:

```java
@Table(name = "roles")
```

not:

```java
@Table(name = "user_roles")
```

---

# Correct Design

Role:

```java
@Entity
@Table(name = "roles")
public class Role {

    @Id
    private UUID id;

    private String name;
}
```

---

User:

```java
@ManyToMany
@JoinTable(
    name = "user_roles",
    joinColumns =
        @JoinColumn(name = "user_id"),
    inverseJoinColumns =
        @JoinColumn(name = "role_id")
)
private Set<Role> roles =
        new HashSet<>();
```

---

# Final Database Structure

Hibernate creates:

## users

```text
Stores Users
```

| user_id | email |
| ------- | ----- |

---

## roles

```text
Stores Roles
```

| role_id | name |
| ------- | ---- |

---

## user_roles

```text
Connects Users and Roles
```

| user_id | role_id |
| ------- | ------- |

---

# How Do We Decide Relationships?

Ask these questions:

## One User → One Address?

```text
One-to-One
```

Use:

```java
@OneToOne
```

---

## One User → Many Orders?

```text
One-to-Many
```

Use:

```java
@OneToMany
```

---

## Many Orders → One User?

```text
Many-to-One
```

Use:

```java
@ManyToOne
```

---

## Many Users → Many Roles?

```text
Many-to-Many
```

Use:

```java
@ManyToMany
```

---

# Mental Model

Think of JPA relationships like friendships.

```text
User
 ↓
Friendship Table
 ↓
Role
```

The friendship table simply answers:

```text
Which User has which Role?
```

Similarly:

```text
@Enumerated
↓
How should enums be stored?

@ManyToMany
↓
How should two entities be connected?

@JoinTable
↓
Which table stores that connection?
```

These are the core ideas behind modeling real-world relationships in JPA.
