# Understanding HikariCP from First Principles

## What problem is HikariCP solving?

Suppose a user clicks Login.

Your Spring Boot application needs to talk to MySQL.

Without HikariCP:

```text
Login Request
      ↓
Create Database Connection
      ↓
Run SQL Query
      ↓
Close Database Connection
```

This happens for every request.

Creating a database connection is expensive because:

* Network communication is required.
* Authentication is required.
* Resources must be allocated.

If 100 users log in simultaneously, creating 100 new connections repeatedly becomes slow.

---

## Real Life Analogy

Imagine a restaurant.

Without connection pooling:

```text
Customer Arrives
      ↓
Build New Table
      ↓
Customer Eats
      ↓
Destroy Table
```

Very inefficient.

Instead, restaurants keep tables ready.

```text
Table 1
Table 2
Table 3
Table 4
Table 5
```

When a customer arrives:

```text
Use Available Table
      ↓
Finish
      ↓
Return Table
```

This is exactly what HikariCP does.

---

## What is HikariCP?

HikariCP is a connection pool manager.

It maintains a set of ready-to-use database connections.

```text
Spring Boot
      ↓
HikariCP
      ↓
MySQL
```

Instead of creating a new connection every time, Spring borrows one from the pool and returns it after use.

---

## pool-name

```yaml
pool-name: HikariCP
```

Simply gives the connection pool a name.

You may see logs such as:

```text
HikariCP-1 - Starting...
```

This setting is mostly for identification.

---

## maximum-pool-size

```yaml
maximum-pool-size: 5
```

Means:

```text
Maximum 5 database connections
can exist in the pool.
```

Example:

```text
Connection 1
Connection 2
Connection 3
Connection 4
Connection 5
```

If all 5 are busy and another request arrives, it must wait.

Restaurant analogy:

```text
5 Tables Available
Customer 6 waits
until a table becomes free.
```

---

## connection-timeout

```yaml
connection-timeout: 10000
```

10000 milliseconds = 10 seconds.

Means:

```text
How long should the application
wait for a free connection?
```

If all connections are busy:

```text
Wait up to 10 seconds.
```

If no connection becomes available:

```text
Throw an exception.
```

---

## idle-timeout

```yaml
idle-timeout: 600000
```

600000 milliseconds = 10 minutes.

Suppose a connection sits unused.

```text
Connection 4
```

No requests use it for 10 minutes.

HikariCP may close it to save resources.

Restaurant analogy:

```text
Unused Table
for a long time
      ↓
Remove it
```

---

## max-lifetime

```yaml
max-lifetime: 1800000
```

1800000 milliseconds = 30 minutes.

Means:

```text
Replace connections
every 30 minutes.
```

Even healthy connections are periodically recreated.

Why?

Because long-lived database connections can become stale due to:

* Network issues
* Database restarts
* Resource leaks

Refreshing them keeps the pool healthy.

---

## validation-timeout

```yaml
validation-timeout: 1800000
```

Before giving a connection to your application:

```text
Is this connection still alive?
```

HikariCP checks it.

This setting controls how long it waits during that validation.

---

## What happens when a Login Request arrives?

Suppose:

```text
POST /login
```

Request Flow:

```text
User Clicks Login
      ↓
Spring Boot
      ↓
HikariCP
      ↓
Borrow Connection #2
      ↓
Run SQL Query
      ↓
Return Connection #2
      ↓
Response Sent
```

Notice:

```text
Connection #2 was not created.
It already existed.
```

This is why connection pools are fast.

---

## Complete Architecture

```text
User Request
      ↓
Spring Boot
      ↓
Spring Data JPA
      ↓
Hibernate
      ↓
HikariCP
      ↓
MySQL
```

Responsibilities:

```text
Spring Data JPA → Repository Layer

Hibernate       → ORM (Object Relational Mapping)

HikariCP        → Connection Pool Management

MySQL           → Stores Data
```

---

## Mental Model

Think of HikariCP as a parking lot.

```text
Parking Lot
 ├─ Connection 1
 ├─ Connection 2
 ├─ Connection 3
 ├─ Connection 4
 └─ Connection 5
```

Whenever Spring needs the database:

```text
Take a car
Drive it
Return it
```

Instead of building a brand-new car every time.

That is the entire purpose of HikariCP.
