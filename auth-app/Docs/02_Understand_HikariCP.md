# HikariCP Advanced Settings Explained from First Principles

## First, What is HikariCP?

Imagine your application is a restaurant.

Customers = User Requests

Tables = Database Connections

Without HikariCP:

```text
Customer Arrives
      ↓
Build New Table
      ↓
Customer Eats
      ↓
Destroy Table
```

Very expensive.

With HikariCP:

```text
Restaurant Already Has 5 Tables
```

When customers arrive:

```text
Use Existing Table
      ↓
Finish
      ↓
Return Table
```

HikariCP does exactly this with database connections.

---

## leak-detection-threshold

```yaml
leak-detection-threshold: 0
```

### What problem does it solve?

Suppose a developer borrows a connection.

```text
Borrow Connection
      ↓
Run Query
      ↓
Return Connection
```

Everything is fine.

But suppose the developer forgets to return it.

```text
Borrow Connection
      ↓
Run Query
      ↓
Never Return Connection
```

This is called a:

```text
Connection Leak
```

---

### Real Life Example

Imagine your office has 5 laptops.

```text
Laptop 1
Laptop 2
Laptop 3
Laptop 4
Laptop 5
```

Employee takes Laptop 2.

```text
Employee Takes Laptop 2
      ↓
Never Returns It
```

Eventually:

```text
No Laptops Available
```

The same thing happens with database connections.

---

### What does this setting mean?

```yaml
leak-detection-threshold: 0
```

Means:

```text
Leak Detection Disabled
```

HikariCP will NOT check for leaked connections.

---

### Example

```yaml
leak-detection-threshold: 5000
```

Means:

```text
If a connection is borrowed
for more than 5 seconds,
print a warning.
```

Hikari logs something like:

```text
Possible connection leak detected
```

This helps developers find bugs.

---

## initialization-fail-timeout

```yaml
initialization-fail-timeout: -1
```

### What problem does it solve?

When Spring Boot starts:

```text
Spring Boot
      ↓
HikariCP
      ↓
MySQL
```

HikariCP tries to connect to MySQL.

---

### Normal Situation

```text
Application Starts
      ↓
Database Available
      ↓
Connection Successful
      ↓
Application Ready
```

---

### Problem Situation

Suppose MySQL is down.

```text
Application Starts
      ↓
Database Not Running
```

What should happen?

---

### Option 1

Fail Immediately

```text
Application Starts
      ↓
Cannot Connect
      ↓
Crash
```

---

### Option 2

Keep Trying

```text
Application Starts
      ↓
Cannot Connect
      ↓
Keep Retrying
```

This is what:

```yaml
initialization-fail-timeout: -1
```

means.

---

### Real Life Example

Suppose you open a shop.

Internet is down.

Option A:

```text
Internet Down
      ↓
Do Not Open Shop
```

Option B:

```text
Internet Down
      ↓
Open Shop Anyway
      ↓
Keep Trying To Reconnect
```

`-1` chooses Option B.

---

## Complete HikariCP Mental Model

Think of HikariCP as a parking lot.

```text
Parking Lot
 ├── Connection 1
 ├── Connection 2
 ├── Connection 3
 ├── Connection 4
 └── Connection 5
```

### maximum-pool-size

```yaml
maximum-pool-size: 5
```

Means:

```text
Maximum 5 connections
can exist.
```

---

### connection-timeout

```yaml
connection-timeout: 10000
```

Means:

```text
Wait 10 seconds
for a free connection.
```

---

### idle-timeout

```yaml
idle-timeout: 600000
```

Means:

```text
Close unused connections
after 10 minutes.
```

---

### max-lifetime

```yaml
max-lifetime: 1800000
```

Means:

```text
Replace connections
every 30 minutes.
```

---

### validation-timeout

```yaml
validation-timeout: 1800000
```

Means:

```text
Check whether a connection
is still alive before use.
```

---

### leak-detection-threshold

```yaml
leak-detection-threshold: 0
```

Means:

```text
Do not check
for leaked connections.
```

---

### initialization-fail-timeout

```yaml
initialization-fail-timeout: -1
```

Means:

```text
If database is unavailable,
do not fail immediately.
Keep trying.
```

---

## What Happens During a Login Request?

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
No New Connection Created
```

The connection already existed in the pool.

That is why HikariCP is fast and is the default connection pool used by Spring Boot.
