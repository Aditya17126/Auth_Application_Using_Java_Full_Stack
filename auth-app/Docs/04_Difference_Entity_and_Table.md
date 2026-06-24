# Why Do We Need @Table If We Already Have @Entity?

## First Principles

Suppose you have a Java class:

```java
@Entity
public class User {
    private String name;
}
```

Question:

> What should Hibernate do with this class?

Answer:

```text
Store it in the database.
```

That's exactly what `@Entity` means.

```java
@Entity
```

translates to:

> "Hibernate, this class represents data that should be persisted."

---

# What Does @Entity Do?

Think of `@Entity` as saying:

```text
This Java class should become a database table.
```

Example:

```java
@Entity
public class User {
}
```

Hibernate thinks:

```text
Okay, I need a table for User.
```

---

# Then What Does @Table Do?

Question:

> What should the table be called?

Hibernate has to decide.

Without `@Table`:

```java
@Entity
public class User {
}
```

Hibernate uses default naming rules.

Usually:

```text
User
```

becomes:

```text
user
```

or sometimes:

```text
users
```

depending on configuration.

---

# Example Without @Table

Java:

```java
@Entity
public class User {
}
```

Hibernate:

```text
I will choose the table name myself.
```

Database:

| user |
| ---- |

---

# Example With @Table

Java:

```java
@Entity
@Table(name = "users")
public class User {
}
```

Hibernate:

```text
Don't decide yourself.
Use the table name "users".
```

Database:

| users |
| ----- |

---

# Real Life Analogy

Imagine you're opening a shop.

`@Entity` says:

```text
I want to open a shop.
```

But it doesn't say what the shop's name is.

Hibernate chooses a name.

---

`@Table` says:

```text
I want to open a shop,
and its name must be:

"Aditya Electronics"
```

Now there is no confusion.

---

# Why Is @Table Useful?

## 1. Existing Databases

Suppose the database already exists.

Database:

```text
tbl_users
```

Java:

```java
@Entity
public class User {
}
```

Hibernate may look for:

```text
user
```

and fail.

Instead:

```java
@Entity
@Table(name = "tbl_users")
public class User {
}
```

Now Hibernate knows exactly where to save data.

---

## 2. Avoid Reserved Keywords

Suppose Hibernate chooses:

```text
user
```

Some databases treat:

```text
USER
```

as a reserved keyword.

Problem.

You can avoid this by saying:

```java
@Table(name = "users")
```

---

## 3. Better Naming Conventions

Many companies use:

```text
tbl_users
app_users
auth_users
```

instead of:

```text
user
```

`@Table` lets you follow company standards.

---

# Do We Always Need @Table?

No.

This works perfectly:

```java
@Entity
public class User {
}
```

Hibernate will generate a table automatically.

---

Use `@Table` when:

```text
✓ You want a custom table name.

✓ You're working with an existing database.

✓ Your company follows naming conventions.

✓ You want to avoid reserved keywords.
```

---

# Mental Model

Think of it like this:

```text
@Entity
    ↓
"This class should be stored."

@Table
    ↓
"Store it in THIS table."
```

---

# Visual Flow

Without @Table:

```text
User Class
    ↓
@Entity
    ↓
Hibernate Chooses Table Name
    ↓
user
```

With @Table:

```text
User Class
    ↓
@Entity
    ↓
@Table(name="users")
    ↓
Hibernate Uses "users"
```

---

# Final Understanding

```java
@Entity
@Table(name = "users")
public class User {
}
```

means:

```text
Hibernate,

This class should be persisted,
and when you persist it,
use the database table named "users".
```

So:

```text
@Entity = Should this class be stored?

@Table  = Where exactly should it be stored?
```
