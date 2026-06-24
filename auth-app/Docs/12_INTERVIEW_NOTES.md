# Interview Notes: Auth Application Concepts

This document covers every major concept used in this project.

For each concept:

```text
1. Simple language summary
2. Real-world analogy
3. Interview questions and answers
4. Common beginner misconceptions
5. Small code examples
```

---

# 1. Spring Boot

## Simple Summary

Spring Boot is a framework that makes building Java web applications easy.

Without Spring Boot:

```text
1. Download Tomcat manually
2. Create web.xml configuration
3. Configure database connection manually
4. Write hundreds of lines of XML
5. Deploy WAR file to Tomcat
6. Restart Tomcat every time
```

With Spring Boot:

```text
1. Add dependencies in pom.xml
2. Write application.yaml
3. Run the main method
4. Everything works
```

---

## Real-World Analogy

```text
Without Spring Boot:
  Building a car from scratch.
  You buy the engine, wheels, seats, steering wheel separately.
  You assemble everything yourself.
  Every part needs manual wiring.

With Spring Boot:
  Buying a car that's already assembled.
  You just put the key in and drive.
  The engine, AC, music system — all pre-configured.
  You customize what you need (seat color, music).
```

---

## How It's Used in This Project

```java
@SpringBootApplication
public class AuthAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthAppApplication.class, args);
    }
}
```

What this does:

```text
1. Starts embedded Tomcat server
2. Scans all packages for beans (@Service, @Repository, etc.)
3. Auto-configures database, security, web server
4. Reads application.yaml for custom settings
5. Application is ready
```

---

## Interview Questions

**Q: What is Spring Boot and how is it different from Spring?**

A:

```text
Spring is the core framework — IoC container, DI, AOP, etc.
Spring Boot is an OPINIONATED layer on top of Spring.

Spring:  You configure everything manually.
Spring Boot: It gives you sensible defaults.
             You override only what you need.

Example:
  Spring:  You configure DataSource bean manually.
  Spring Boot: Just put spring.datasource.url in YAML.
               DataSource is auto-configured.
```

**Q: What does @SpringBootApplication do?**

A:

```text
It combines three annotations:

@Configuration       → This class can define @Bean methods
@EnableAutoConfiguration → Auto-configure beans based on classpath
@ComponentScan       → Scan current package + sub-packages for beans
```

**Q: What is auto-configuration?**

A:

```text
Spring Boot checks your classpath (dependencies in pom.xml).

It finds: spring-boot-starter-data-jpa
It thinks: "JPA is present. Let me configure:
            - DataSource (HikariCP)
            - EntityManagerFactory
            - TransactionManager
            - JPA Repositories"

You didn't write a single line of config for these.
They were auto-configured.
```

---

## Common Misconceptions

```text
❌ "Spring Boot is a different framework from Spring."
✅ Spring Boot IS Spring. It's Spring with auto-configuration.

❌ "Spring Boot doesn't use Tomcat."
✅ Spring Boot embeds Tomcat inside the JAR. No separate installation.

❌ "You can't customize anything in Spring Boot."
✅ You can override every single auto-configuration with your own beans.

❌ "@SpringBootApplication scans ALL packages."
✅ It scans only the package where it's located and sub-packages.
   That's why AuthAppApplication is in com.substring.auth.auth_app.
   Everything under this package gets scanned.
```

---

---

# 2. JPA / Hibernate

## Simple Summary

```text
JPA    = A specification (set of rules/interfaces)
Hibernate = An implementation of JPA (the actual code)
```

Think of it like:

```text
JPA:       "Every car must have brakes, steering, and an engine."
Hibernate: "Here's an actual car that follows those rules."
```

JPA defines annotations like:

```java
@Entity, @Table, @Id, @Column, @ManyToMany
```

Hibernate reads these annotations and:

```text
1. Creates database tables
2. Converts Java objects to SQL rows
3. Converts SQL rows back to Java objects
```

---

## How It's Used in This Project

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "user_email", unique = true, length = 300)
    private String email;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

What Hibernate does with this:

```text
CREATE TABLE users (
    user_id    BINARY(16) PRIMARY KEY,
    user_email VARCHAR(300) UNIQUE,
    name       VARCHAR(255),
    password   VARCHAR(255),
    image      VARCHAR(255),
    enable     BIT(1),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    provider   VARCHAR(255)
);

CREATE TABLE roles (
    id   BINARY(16) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE user_roles (
    user_id BINARY(16),
    role_id BINARY(16),
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

---

## Interview Questions

**Q: What is the difference between JPA and Hibernate?**

A:

```text
JPA     = Interface (specification)
Hibernate = Class (implementation)

JPA says: "There should be a method to persist an entity."
Hibernate says: "Here's how I persist it — using SQL INSERT."

You code against JPA annotations.
Hibernate runs behind the scenes.

You could replace Hibernate with EclipseLink
without changing your entity code.
```

**Q: What does ddl-auto: update mean?**

A:

```text
It tells Hibernate what to do with tables at startup.

validate:  Check if tables match entities. Don't change anything.
update:    Create missing tables/columns. Never delete anything.
create:    Drop all tables and recreate them every startup.
create-drop: Like create, but also drop tables on shutdown.
none:      Do nothing.

Our project uses 'update':
  First run:  Creates users, roles, user_roles tables.
  Second run: Checks if tables exist. If new fields added, alters table.
  Never deletes existing data.
```

**Q: What is the N+1 problem?**

A:

```text
Suppose you load 100 users.
Each user has roles (loaded lazily).

Query 1: SELECT * FROM users          → 1 query
Query 2-101: SELECT * FROM roles      → 100 queries (one per user)
             WHERE user_id = ?

Total: 101 queries for what should be 1-2 queries.

Solutions:
  1. @ManyToMany(fetch = FetchType.EAGER) → loads in one query (our project)
  2. JOIN FETCH in JPQL query
  3. @EntityGraph
  4. @BatchSize
```

---

## Common Misconceptions

```text
❌ "JPA and Hibernate are the same thing."
✅ JPA is a specification. Hibernate is one implementation.

❌ "ddl-auto: update is safe for production."
✅ NEVER use 'update' in production.
   It can alter tables unexpectedly.
   Use 'validate' or 'none' in production.
   Use Flyway or Liquibase for migrations.

❌ "@Entity creates the table automatically."
✅ @Entity marks the class for JPA.
   Hibernate creates the table only if ddl-auto is set to update/create.

❌ "EAGER fetch is always better because data is ready."
✅ EAGER loads related data EVERY time, even when not needed.
   For large datasets, this causes massive performance issues.
   LAZY is the default for @ManyToMany and @OneToMany for good reason.
```

---

---

# 3. Repository Pattern

## Simple Summary

A Repository is a class that handles all database operations for a specific entity.

```text
Want to save a user?      → userRepository.save(user)
Want to find a user?      → userRepository.findById(id)
Want to delete a user?    → userRepository.deleteById(id)
Want all users?           → userRepository.findAll()
```

You never write SQL.

Spring Data JPA generates the SQL for you.

---

## Real-World Analogy

```text
Think of a Repository as a librarian.

You:        "I need the book with ISBN 12345."
Librarian:  Goes to the shelf, finds it, brings it to you.

You:        "Save this new book."
Librarian:  Takes the book, catalogs it, puts it on the shelf.

You never go into the storage room.
You never organize the shelves.
The librarian handles all that.

Repository = Librarian for your database.
```

---

## How It's Used in This Project

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Notice:

```text
1. It's an INTERFACE, not a class
2. No implementation code
3. No @Repository annotation needed
4. Spring Data JPA generates the implementation at runtime
```

What `JpaRepository<User, UUID>` gives you for free:

```text
save(User user)              → INSERT or UPDATE
findById(UUID id)            → SELECT by primary key
findAll()                    → SELECT all
deleteById(UUID id)          → DELETE by primary key
count()                      → COUNT(*)
existsById(UUID id)          → EXISTS check
findAll(Pageable pageable)   → Pagination support
findAll(Sort sort)           → Sorting support
```

Custom methods:

```text
findByEmail(String email)    → SELECT * FROM users WHERE user_email = ?
existsByEmail(String email)  → SELECT COUNT(*) > 0 FROM users WHERE user_email = ?
```

How does Spring know the SQL?

```text
Method name:  findByEmail
Spring reads: find → SELECT
              By   → WHERE
              Email → user_email column (from @Column on entity)

Result: SELECT * FROM users WHERE user_email = :email
```

---

## Interview Questions

**Q: How does Spring Data JPA generate implementations for repository interfaces?**

A:

```text
At startup, Spring Data JPA:
1. Scans for interfaces extending JpaRepository
2. Creates a PROXY class implementing the interface
3. This proxy class uses EntityManager internally
4. Method names are parsed to generate JPQL/SQL
5. The proxy is registered as a Spring bean

You write: Optional<User> findByEmail(String email);
Spring creates: A class with the actual SQL query logic.
```

**Q: What is the difference between CrudRepository and JpaRepository?**

A:

```text
CrudRepository:
  Basic CRUD: save, findById, findAll, delete, count
  No pagination or sorting.

JpaRepository (extends CrudRepository):
  Everything in CrudRepository PLUS:
  Pagination: findAll(Pageable)
  Sorting: findAll(Sort)
  Batch operations: saveAll, deleteAllInBatch
  Flush: flush, saveAndFlush

Our project uses JpaRepository → we get everything.
```

**Q: Why does findByEmail return Optional instead of User?**

A:

```text
Because the user might not exist.

Without Optional:
  User user = findByEmail("x@y.com");
  user.getName();  // NullPointerException if not found!

With Optional:
  Optional<User> user = findByEmail("x@y.com");
  user.orElseThrow(() -> new NotFoundException("User not found"));

Optional FORCES you to handle the "not found" case.
It eliminates NullPointerException.
```

---

## Common Misconceptions

```text
❌ "You need to write @Repository on JpaRepository interfaces."
✅ Not needed. Spring Data JPA auto-detects JpaRepository sub-interfaces.
   @Repository is only needed for custom DAO classes.

❌ "You can name methods anything and Spring will figure it out."
✅ Method names must follow strict conventions.
   findByEmail    ✅ (matches field 'email')
   findByMail     ❌ (no field called 'mail')
   getByEmail     ✅ (get, find, read, query, search all work)

❌ "Repositories handle business logic."
✅ Repositories ONLY handle data access.
   Business logic belongs in the Service layer.
   A repository should never validate data or check permissions.
```

---

---

# 4. Service Layer

## Simple Summary

The Service layer contains business logic.

```text
Repository:  "I can save and find users."
Service:     "I decide WHEN and HOW to save users."
```

Example:

```text
Creating a user:
1. Check if email already exists       ← Business rule
2. Hash the password                   ← Business rule
3. Assign default role                 ← Business rule
4. Save to database                    ← Delegates to repository
5. Send welcome email                  ← Business rule
6. Return the user DTO                 ← Transformation
```

Steps 1, 2, 3, 5, 6 are business logic.

Step 4 is data access (delegated to repository).

---

## Real-World Analogy

```text
Think of a restaurant.

Customer (Controller):    "I want a pizza."
Chef (Service):           Decides the recipe, cooks it, plates it.
Pantry (Repository):      Stores and provides ingredients.

The chef doesn't just pass ingredients to the customer.
The chef transforms them into a finished dish.

Service layer = Chef
```

---

## How It's Used in This Project

Interface:

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

Implementation:

```java
@Service
public class UserServiceImpl implements UserService {
    @Override
    public UserDto createUser(UserDto userDto) {
        return null;  // Not yet implemented
    }
    // ... all methods return null
}
```

---

## Interview Questions

**Q: Why use an interface for the Service layer?**

A:

```text
1. Abstraction: Controller depends on UserService (interface),
   NOT UserServiceImpl. It doesn't know the implementation.

2. Testability: In tests, you can create a MockUserService
   that implements UserService. No database needed.

3. Flexibility: You can swap implementations.
   Today: UserServiceImpl (MySQL)
   Tomorrow: UserServiceRedisImpl (Redis cache)
   Controller code doesn't change.

4. Dependency Inversion Principle (SOLID's D):
   High-level modules depend on abstractions,
   not concrete implementations.
```

**Q: Where should validation happen — Controller or Service?**

A:

```text
Both, but different types.

Controller validation:
  "Is the email field present?"
  "Is the email format valid?"
  → Use @Valid + Jakarta Validation annotations
  → These are INPUT validation rules

Service validation:
  "Does this email already exist in the database?"
  "Does the user have permission for this action?"
  → These are BUSINESS validation rules
  → They require database access or business context

Example:
  Controller: @NotBlank, @Email, @Size
  Service:    if (userRepository.existsByEmail(email)) throw DuplicateException
```

---

## Common Misconceptions

```text
❌ "The Service layer is just a pass-through to the Repository."
✅ If your Service just calls repository methods with no logic,
   you either have very simple CRUD (which is fine)
   or you're missing business rules.

❌ "Services should return entities."
✅ Services should return DTOs.
   Entities are internal implementation details.
   DTOs are the public API.

❌ "Each entity must have exactly one Service."
✅ A Service can work with multiple repositories.
   UserService might need both UserRepository and RoleRepository
   to assign roles during user creation.
```

---

---

# 5. DTO Pattern (Data Transfer Object)

## Simple Summary

A DTO is a simple object that carries data between layers.

```text
Entity:  User (has database-specific fields like @Id, @Table)
DTO:     UserDto (has only the data needed for the API)
```

Why not just use Entity everywhere?

```text
Entity exposes:
  Database column names
  Internal relationships
  Fields that should be hidden (like password hash)

DTO exposes:
  Only what the client needs
  Clean, API-friendly field names
  No database details
```

---

## Real-World Analogy

```text
Think of a hospital.

Patient Record (Entity):
  Full medical history
  Insurance details
  Internal notes by doctors
  Lab results
  Social security number

Appointment Summary (DTO):
  Patient name
  Appointment date
  Doctor name
  Prescription

You don't send the entire patient record to the front desk.
You send only what they need.

Entity = Full patient record (internal)
DTO = Appointment summary (external)
```

---

## How It's Used in This Project

```java
public class UserDto {
    private UUID id;
    private String email;
    private String name;
    private String password;      // ← Problematic
    private String image;
    private boolean enable = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Provider provider = Provider.LOCAL;
    private Set<Role> roles = new HashSet<>();  // ← Problematic
}
```

Problems:

```text
1. Contains password field → Should NOT be in response DTO
2. Uses Role entity → Should use RoleDto
3. Uses Provider entity → Should use String or separate enum
4. Mirrors entity exactly → Defeats the purpose of DTOs
```

---

## Interview Questions

**Q: What is the DTO pattern and why is it important?**

A:

```text
DTO = Data Transfer Object

Purpose:
  1. Decouple API from database schema
  2. Hide sensitive fields (password, internal IDs)
  3. Combine data from multiple entities into one response
  4. Control what data goes in and out

Example:
  Entity User has 10 fields.
  API response needs only 5 fields.
  DTO carries only those 5 fields.
```

**Q: Should you have separate DTOs for request and response?**

A:

```text
YES. This is a best practice.

CreateUserRequest (input):
  email, name, password
  → Used when creating a user

UserResponse (output):
  id, email, name, image, roles
  → Used when returning user data
  → NO password!

UpdateUserRequest (input):
  name, image
  → Used when updating profile
  → Cannot change email or password here

Using one DTO for everything is a common mistake.
```

---

## Common Misconceptions

```text
❌ "DTOs are just copies of entities."
✅ If your DTO has the exact same fields as the entity,
   you're not getting any benefit. DTOs should differ.

❌ "DTOs are unnecessary boilerplate."
✅ Without DTOs, changing a database column name
   breaks your API. DTOs provide a contract.

❌ "DTOs should have business logic."
✅ DTOs should be pure data carriers.
   No logic, no validation rules, no methods.
   Just fields, getters, and setters.
```

---

---

# 6. Dependency Injection

## Simple Summary

Dependency Injection means:

```text
Instead of creating your dependencies yourself,
someone else creates them and gives them to you.
```

Without DI:

```java
public class UserServiceImpl {
    private UserRepository repo = new UserRepository(); // ❌ You create it
}
```

With DI:

```java
@Service
public class UserServiceImpl {
    private final UserRepository repo;

    public UserServiceImpl(UserRepository repo) { // ✅ Spring creates and gives it
        this.repo = repo;
    }
}
```

---

## Real-World Analogy

```text
Without DI:
  You're a chef.
  You grow your own vegetables.
  You raise your own chickens.
  You make your own cheese.
  Then you cook.

With DI:
  You're a chef.
  Ingredients are delivered to your kitchen.
  You just cook.

The delivery person = Spring IoC Container
The ingredients = Dependencies (Repository, other Services)
```

---

## Three Types of Injection

```java
// 1. Constructor Injection (BEST)
@Service
public class UserServiceImpl {
    private final UserRepository repo;

    public UserServiceImpl(UserRepository repo) {
        this.repo = repo;
    }
}

// 2. Field Injection (AVOID)
@Service
public class UserServiceImpl {
    @Autowired
    private UserRepository repo;
}

// 3. Setter Injection (RARE)
@Service
public class UserServiceImpl {
    private UserRepository repo;

    @Autowired
    public void setRepo(UserRepository repo) {
        this.repo = repo;
    }
}
```

---

## Interview Questions

**Q: Why is constructor injection preferred over field injection?**

A:

```text
Constructor Injection:
  1. Fields can be 'final' → immutable, thread-safe
  2. Dependencies are explicit in the constructor signature
  3. Easy to test: just pass mocks in the constructor
  4. Application fails fast if dependency is missing
  5. No reflection needed

Field Injection:
  1. Fields cannot be 'final'
  2. Dependencies are hidden (not visible in constructor)
  3. Requires reflection to set in tests (or @MockBean)
  4. Can create beans with null dependencies
  5. Spring-specific (not portable)
```

**Q: What is the IoC Container?**

A:

```text
IoC = Inversion of Control

Normally:
  Your code controls the flow.
  You create objects. You call methods.

With IoC:
  The framework controls the flow.
  It creates objects. It injects dependencies.
  You just define WHAT you need.

Spring's IoC Container:
  Called ApplicationContext.
  It's a big HashMap of beans.
  Key = type (UserService.class)
  Value = instance (UserServiceImpl object)
```

---

## Common Misconceptions

```text
❌ "@Autowired is always required for injection."
✅ If a class has only ONE constructor,
   @Autowired is optional. Spring auto-detects it.
   This is our project's recommended approach.

❌ "DI and IoC are the same thing."
✅ IoC = Broad principle (framework controls flow)
   DI = Specific technique (dependencies are injected)
   DI is one way to achieve IoC.

❌ "Every class should be a Spring bean."
✅ Only classes that need DI or lifecycle management.
   Entities are NOT beans.
   DTOs are NOT beans.
   Utility classes with static methods are NOT beans.
```

---

---

# 7. ManyToMany Relationships

## Simple Summary

ManyToMany means:

```text
One User can have Many Roles.
One Role can belong to Many Users.
```

Database needs a third table to store this:

```text
users:       Stores user data
roles:       Stores role data
user_roles:  Stores which user has which role
```

---

## Real-World Analogy

```text
Students and Courses.

One student enrolls in many courses.
One course has many students.

You need an enrollment table:
| student_id | course_id |
| ---------- | --------- |
| 1          | 101       |
| 1          | 102       |
| 2          | 101       |

This is a join table.
Same concept as user_roles in our project.
```

---

## How It's Used in This Project

```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles = new HashSet<>();
```

Breaking it down:

```text
@ManyToMany           → "This is a many-to-many relationship"
@JoinTable            → "Here's the join table configuration"
name = "user_roles"   → "The join table is called user_roles"
joinColumns           → "The column in the join table pointing to THIS entity (User)"
inverseJoinColumns    → "The column in the join table pointing to the OTHER entity (Role)"
```

---

## Interview Questions

**Q: What is a Join Table and why is it needed?**

A:

```text
Relational databases store data in rows.
One cell cannot contain multiple values.

You CANNOT do:
| user_id | roles          |
| 1       | [USER, ADMIN]  |  ← Arrays not allowed in cells

You MUST do:
users:
| user_id | email    |
| 1       | a@b.com  |

roles:
| role_id | name  |
| 10      | USER  |
| 20      | ADMIN |

user_roles:        ← JOIN TABLE
| user_id | role_id |
| 1       | 10      |
| 1       | 20      |
```

**Q: What is the difference between the owning side and inverse side?**

A:

```text
Owning side: The entity that defines @JoinTable.
             In our project: User owns the relationship.

Inverse side: The other entity.
              In our project: Role is the inverse side.

The owning side controls the join table.
INSERT/DELETE in user_roles happens through User.

If Role wanted to be bidirectional:
@ManyToMany(mappedBy = "roles")
private Set<User> users;

But in our project, Role has no reference back to User.
This is a UNIDIRECTIONAL many-to-many.
```

---

## Common Misconceptions

```text
❌ "ManyToMany always requires two @ManyToMany annotations."
✅ Only the owning side needs @ManyToMany + @JoinTable.
   The inverse side is optional (only if you need bidirectional navigation).
   Our project is unidirectional — only User knows about Roles.

❌ "List and Set are interchangeable for ManyToMany."
✅ Set prevents duplicates. List allows duplicates.
   For roles: Set<Role> is correct.
   Using List<Role> can cause performance issues
   (Hibernate may delete all + re-insert when modifying).

❌ "EAGER fetch is fine for ManyToMany."
✅ EAGER can cause performance problems with large datasets.
   LAZY is the default for @ManyToMany.
   Our project overrides this to EAGER — acceptable for roles
   (small dataset) but not for large relationships.
```

---

---

# 8. UUID vs Long for Primary Keys

## Simple Summary

```text
Long:  1, 2, 3, 4, 5, ...
UUID:  550e8400-e29b-41d4-a716-446655440000
```

---

## When to Use Each

```text
Long (Auto-Increment):
  ✅ Simple applications
  ✅ Single database server
  ✅ Internal IDs (not exposed in URLs)
  ✅ Better query performance (smaller index)
  ❌ Predictable (users can guess /api/users/2)
  ❌ Collision risk in distributed systems

UUID:
  ✅ Distributed systems (multiple servers)
  ✅ Public-facing IDs (URLs, APIs)
  ✅ Globally unique (no collisions ever)
  ✅ Can be generated without database
  ❌ Larger storage (16 bytes vs 8 bytes)
  ❌ Slower index lookups
  ❌ Harder to read/debug
```

Our project uses UUID:

```java
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

Good decision for an auth application where user IDs appear in API URLs.

---

## Interview Questions

**Q: Why did this project choose UUID over Long for the primary key?**

A:

```text
1. Security: User IDs are exposed in API URLs.
   /api/users/1 → attacker tries /api/users/2 (IDOR attack)
   /api/users/550e8400... → impossible to guess

2. Scalability: If we add a second server,
   both can generate UUIDs without coordination.
   With Long, both might generate ID = 1.

3. Pre-generation: UUID can be generated in Java
   BEFORE saving to the database.
   Useful for event-driven systems.
```

**Q: What is the performance impact of UUID keys?**

A:

```text
UUIDs are 16 bytes. Longs are 8 bytes.

For primary keys:
  Index size doubles.
  Disk usage increases.
  Joins are slightly slower.

For most applications (< 10 million rows):
  The difference is negligible.

For billions of rows:
  Consider ULIDs (sortable UUIDs)
  or Snowflake IDs (Twitter's approach).
```

---

---

# 9. Enum Handling in JPA

## Simple Summary

Java enums need to be stored in the database somehow.

```java
public enum Provider {
    LOCAL,
    GOOGLE,
    GITHUB,
    FACEBOOK
}
```

Two options:

```text
EnumType.ORDINAL → Store the position number (0, 1, 2, 3)
EnumType.STRING  → Store the name as text ("LOCAL", "GOOGLE")
```

---

## Why STRING Is Better

```text
Suppose you have:
  LOCAL = 0, GOOGLE = 1, GITHUB = 2, FACEBOOK = 3

Database row: provider = 1 → means GOOGLE

Now you reorder the enum:
  LOCAL = 0, FACEBOOK = 1, GOOGLE = 2, GITHUB = 3

Database still has: provider = 1
But now 1 = FACEBOOK, not GOOGLE!

Your data is CORRUPTED.

With STRING:
  Database has: provider = "GOOGLE"
  Reordering the enum doesn't matter.
  "GOOGLE" always means GOOGLE.
```

---

## Interview Questions

**Q: When would you use EnumType.ORDINAL?**

A:

```text
Almost never.

The only scenario:
  - The enum will NEVER change order
  - The enum will NEVER have values removed
  - You desperately need to save a few bytes per row

In practice: Always use EnumType.STRING.
```

---

---

# 10. Connection Pooling (HikariCP)

## Simple Summary

Opening a database connection is expensive:

```text
1. TCP handshake
2. SSL negotiation
3. Authentication
4. Protocol initialization
```

This takes 30-100 milliseconds each time.

Connection pooling solution:

```text
Create a pool of connections at startup.
Reuse them for every query.
Return them to the pool when done.
Never close them until the app shuts down.
```

---

## Real-World Analogy

```text
Without Connection Pool:
  Every customer at a restaurant gets a new table built.
  When they leave, the table is destroyed.
  Next customer → build another table.

With Connection Pool:
  10 tables are already set up.
  Customer arrives → sits at an available table.
  Customer leaves → table is cleaned and ready for the next person.
  Tables are reused, never destroyed.

Tables = Database connections
Restaurant = Application
Customers = Database queries
```

---

## How It Appears in This Project

In application-dev.yml (commented out):

```yaml
# hikari:
#   pool-name: HikariCP
#   maximum-pool-size: 5
#   connection-timeout: 10000
#   idle-timeout: 600000
#   max-lifetime: 1800000
```

Even though these are commented out, HikariCP is still active!

```text
Spring Boot auto-configures HikariCP with defaults:
  maximum-pool-size: 10
  connection-timeout: 30000 (30 seconds)
  idle-timeout: 600000 (10 minutes)
  max-lifetime: 1800000 (30 minutes)
```

---

## Interview Questions

**Q: What is HikariCP and why is it the default in Spring Boot?**

A:

```text
HikariCP is a JDBC connection pool library.
"Hikari" means "light" in Japanese.

Why it's the default:
  1. Fastest JDBC connection pool (benchmarked)
  2. Smallest footprint (~130KB)
  3. Zero-overhead proxying
  4. Reliable connection validation
  5. Production-ready with sensible defaults

Alternatives: Apache DBCP2, Tomcat JDBC Pool, C3P0
All are slower and heavier than HikariCP.
```

**Q: What happens if all connections in the pool are in use?**

A:

```text
New requests WAIT for a connection to become available.

If no connection becomes available within the 
connection-timeout (default: 30 seconds):
  A SQLException is thrown.

This is called: Connection Starvation.

Solutions:
  1. Increase pool size (but not too much)
  2. Optimize slow queries
  3. Reduce transaction duration
  4. Use read replicas for read queries
```

---

---

# 11. Spring Security Basics

## Simple Summary

Spring Security is a framework that handles:

```text
1. Authentication → "Who are you?" (login)
2. Authorization  → "What are you allowed to do?" (permissions)
```

---

## How It Works at a High Level

```text
HTTP Request arrives
       ↓
Security Filter Chain (15+ filters)
       ↓
Is the user authenticated?
       ↓
    NO → Send 401 Unauthorized
    YES → Does the user have the right role?
              ↓
           NO → Send 403 Forbidden
           YES → Forward to Controller
```

---

## Current State in This Project

```text
Dependency added:     ✅ (spring-boot-starter-security)
Configuration class:  ❌ (no SecurityConfig)
```

Default behavior:

```text
1. ALL endpoints require authentication
2. Default user: "user"
3. Random password printed in console
4. Basic authentication enabled
5. CSRF enabled (blocks POST from Postman)
6. Default login page at /login
```

---

## Interview Questions

**Q: What happens when you add spring-boot-starter-security without any configuration?**

A:

```text
Spring Security auto-configures with MAXIMUM security:

1. Every endpoint requires authentication
2. A user named "user" is created
3. A random password is generated and logged
4. Form-based login at /login
5. CSRF protection enabled
6. Session-based authentication
7. All POST/PUT/DELETE requests blocked without CSRF token

This is "secure by default" philosophy.
You explicitly OPEN endpoints, not close them.
```

**Q: What is a SecurityFilterChain?**

A:

```text
A chain of servlet filters that every HTTP request passes through.

Key filters:
1. CorsFilter              → Handle cross-origin requests
2. CsrfFilter              → Block CSRF attacks
3. UsernamePasswordFilter   → Process login form
4. BasicAuthenticationFilter → Process Basic Auth header
5. AuthorizationFilter      → Check user permissions
6. ExceptionTranslationFilter → Convert security exceptions to HTTP responses

You can customize which filters run and in what order.
```

---

---

# 12. Builder Pattern

## Simple Summary

Builder pattern solves the problem of creating objects with many fields.

Without Builder:

```java
User user = new User(
    UUID.randomUUID(),
    "aditya@gmail.com",
    "Aditya",
    "password123",
    "image.jpg",
    true,
    Instant.now(),
    Instant.now(),
    Provider.LOCAL,
    new HashSet<>()
);
```

Problem:

```text
Which parameter is which?
What if you only need email and name?
What if you mix up the order?
```

With Builder:

```java
User user = User.builder()
    .email("aditya@gmail.com")
    .name("Aditya")
    .password("password123")
    .build();
```

Clear, readable, and you only set what you need.

---

## Interview Questions

**Q: How does Lombok's @Builder work?**

A:

```text
@Builder generates:
1. A static inner class UserBuilder
2. Method for each field (returns builder for chaining)
3. A build() method that calls the constructor

Generated code (simplified):
public class User {
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String email;

        public UserBuilder email(String email) {
            this.email = email;
            return this;  // Enables chaining
        }

        public User build() {
            return new User(email, name, ...);
        }
    }
}
```

**Q: What is the difference between Builder and Constructor?**

A:

```text
Constructor:
  Order matters.
  All parameters required (unless overloaded).
  Hard to read with many parameters.

Builder:
  Order doesn't matter.
  Set only what you need.
  Self-documenting: .email("...") tells you what you're setting.

Rule of thumb:
  3 or fewer parameters → Constructor is fine
  4 or more parameters → Use Builder
```

---

---

# 13. Layered Architecture

## Simple Summary

Layered architecture divides the application into horizontal layers.

```text
Layer 1: Controller (API Layer)
  ↓ calls
Layer 2: Service (Business Logic Layer)
  ↓ calls
Layer 3: Repository (Data Access Layer)
  ↓ queries
Layer 4: Database
```

Each layer has ONE responsibility.

```text
Controller: Accept HTTP requests, return HTTP responses
Service:    Apply business rules, coordinate operations
Repository: Read/write data from database
Database:   Store data permanently
```

---

## The Rules

```text
Rule 1: A layer can ONLY call the layer directly below it.
  Controller → Service  ✅
  Controller → Repository  ❌ (skipping Service)

Rule 2: A layer should NEVER call the layer above it.
  Service → Controller  ❌ (going backwards)

Rule 3: Each layer communicates through interfaces.
  Controller uses UserService (interface)
  NOT UserServiceImpl (concrete class)
```

---

## Interview Questions

**Q: Why not just call the Repository directly from the Controller?**

A:

```text
Skipping the Service layer means:

1. Business logic in Controller
   → Controllers become huge and messy

2. No reusability
   → Two controllers doing the same thing
   → Duplicate business logic

3. Hard to test
   → Controllers need a web server to test
   → Services can be tested as plain Java

4. Mixing concerns
   → HTTP handling + business rules + data access
   → All in one class = maintenance nightmare

Service layer is the gatekeeper:
  "I decide the rules."
  "Repository just stores."
  "Controller just receives HTTP."
```

---

---

# 14. Bean Lifecycle in Spring

## Simple Summary

A Spring bean goes through a lifecycle:

```text
1. Instantiation     → Spring creates the object (new UserServiceImpl())
2. Populate Properties → Dependencies are injected
3. BeanNameAware     → Bean gets its name
4. BeanFactoryAware  → Bean gets reference to BeanFactory
5. Pre-Initialization → @PostConstruct runs
6. InitializingBean  → afterPropertiesSet() runs
7. Custom init-method → init() method runs
8. ****Bean is Ready**** → Application uses it
9. Pre-Destroy       → @PreDestroy runs
10. DisposableBean   → destroy() runs
11. Custom destroy   → destroy-method runs
12. Bean is Destroyed → Garbage collected
```

For most applications, you only care about:

```text
Constructor → Dependencies injected → @PostConstruct → Ready → @PreDestroy → Destroyed
```

---

## How It Relates to This Project

```text
Spring Boot starts
      ↓
Creates UserServiceImpl bean
      ↓
Injects dependencies (currently none, should be UserRepository)
      ↓
Bean is Ready
      ↓
Application serves requests using this bean
      ↓
Application shuts down
      ↓
Bean is destroyed
```

---

## Interview Questions

**Q: What is the difference between @PostConstruct and constructor?**

A:

```text
Constructor:
  Runs BEFORE dependencies are injected (for field/setter injection)
  Cannot use injected dependencies safely

@PostConstruct:
  Runs AFTER all dependencies are injected
  Safe to use injected dependencies

Example:
@Service
public class UserServiceImpl {
    @Autowired
    private UserRepository repo;

    public UserServiceImpl() {
        // repo is NULL here (not yet injected)
    }

    @PostConstruct
    public void init() {
        // repo is available here ✅
        long count = repo.count();
        log.info("Total users: {}", count);
    }
}

Note: With constructor injection, this distinction matters less
because dependencies are available in the constructor.
```

**Q: What is the default scope of a Spring bean?**

A:

```text
Singleton.

One instance per ApplicationContext.
All injections of UserService point to the SAME UserServiceImpl object.

Other scopes:
  prototype:  New instance every time
  request:    One instance per HTTP request
  session:    One instance per HTTP session
  application: One instance per ServletContext

For services and repositories: Singleton is correct.
They are stateless — no user-specific data stored in fields.
```

---

---

# 15. Profile-Based Configuration

## Simple Summary

Profiles allow different configurations for different environments.

```text
Development: MySQL on localhost, debug SQL, port 8083
QA:          QA database, port 8081
Production:  Production database, port 8080
```

Same code, different settings.

---

## How It Works in This Project

```yaml
# application.yaml (main config)
spring:
  profiles:
    active: dev    ← "Use dev profile"
server:
  port: 8082       ← Default port (overridden by profile)
```

```yaml
# application-dev.yml
server:
  port: 8083       ← Overrides 8082
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_application_java
```

```yaml
# application-prod.yml
server:
  port: 8080       ← Overrides 8082
```

```yaml
# application-qa.yml
server:
  port: 8081       ← Overrides 8082
```

How profiles are resolved:

```text
1. Load application.yaml       → Base config
2. Read spring.profiles.active → "dev"
3. Load application-dev.yml    → Merge with base
4. Profile values OVERRIDE base values
```

Result when `active: dev`:

```text
Port: 8083 (from dev, overrides base 8082)
Database: MySQL on localhost
Hibernate: update mode, show SQL
```

---

## Interview Questions

**Q: How do you activate a profile without changing application.yaml?**

A:

```text
Multiple ways:

1. Command line argument:
   java -jar app.jar --spring.profiles.active=prod

2. Environment variable:
   SPRING_PROFILES_ACTIVE=prod java -jar app.jar

3. JVM system property:
   java -Dspring.profiles.active=prod -jar app.jar

4. In application.yaml:
   spring.profiles.active: prod

Priority (highest to lowest):
  Command line > Environment variable > JVM property > YAML
```

**Q: Can multiple profiles be active at the same time?**

A:

```text
Yes!

spring:
  profiles:
    active: dev, debug

This loads:
  application.yaml
  application-dev.yml
  application-debug.yml

Properties from later profiles override earlier ones.
If both dev and debug define server.port,
debug wins because it's listed last.
```

---

## Common Misconceptions

```text
❌ "You need separate application files for profiles."
✅ You can use multi-document YAML in a single file:
   ---
   spring.config.activate.on-profile: dev
   server.port: 8083
   ---
   spring.config.activate.on-profile: prod
   server.port: 8080

❌ "Profile config completely replaces base config."
✅ Profile config MERGES with base config.
   Only conflicting properties are overridden.
   Non-conflicting properties from base are kept.

❌ "Profiles are only for ports and database URLs."
✅ You can profile anything:
   - Logging levels
   - Feature flags
   - External API URLs
   - Caching strategies
   - Security rules
```

---

---

# Quick Reference: All Concepts at a Glance

| Concept              | One-Line Summary                                           | Used In Our Project? |
|----------------------|------------------------------------------------------------|----------------------|
| Spring Boot          | Auto-configured framework for Java apps                    | ✅ Core framework    |
| JPA / Hibernate      | ORM: Maps Java objects to database tables                  | ✅ User, Role entities |
| Repository Pattern   | Interface-based data access                                | ✅ UserRepository    |
| Service Layer        | Business logic container                                   | ✅ UserService       |
| DTO Pattern          | Carries data between layers                                | ⚠️ Exists but flawed |
| Dependency Injection | Framework provides dependencies                            | ✅ @Service          |
| ManyToMany           | Relationship needing a join table                          | ✅ User ↔ Role       |
| UUID vs Long         | Globally unique ID vs auto-increment                       | ✅ UUID chosen       |
| Enum Handling        | Store enum as STRING in database                           | ✅ Provider enum     |
| HikariCP             | Fast connection pool for database                          | ✅ Auto-configured   |
| Spring Security      | Authentication and authorization framework                 | ⚠️ Added, not configured |
| Builder Pattern      | Readable object construction                               | ✅ @Builder          |
| Layered Architecture | Separate Controller → Service → Repository                | ⚠️ Missing Controller |
| Bean Lifecycle       | Create → Initialize → Use → Destroy                       | ✅ Managed by Spring |
| Profile Config       | Different settings per environment                         | ✅ dev/prod/qa       |

---

# Top 10 Interview Questions Across All Topics

**1. Explain the flow of a REST API request in a Spring Boot application.**

```text
HTTP Request → DispatcherServlet → Controller → Service → Repository → Database
Database → Repository → Service → Controller → HTTP Response
```

**2. What is the difference between @Component, @Service, @Repository, and @Controller?**

```text
All are specializations of @Component. They mark classes as Spring beans.
@Service → Business logic
@Repository → Data access (adds exception translation)
@Controller → Web controller (handles HTTP)
Functionally identical, but semantically different.
```

**3. What is the purpose of the DTO pattern?**

```text
Decouple API from database schema.
Hide sensitive fields.
Control input/output shape.
Allow API to evolve independently of database.
```

**4. Why use constructor injection over field injection?**

```text
Immutability (final fields), explicit dependencies,
easy testing, no reflection needed, fails fast.
```

**5. What is the N+1 select problem?**

```text
1 query for the parent + N queries for each child.
Solutions: EAGER fetch, JOIN FETCH, @EntityGraph, @BatchSize.
```

**6. What is ddl-auto and which value should be used in production?**

```text
Controls Hibernate's table management.
Development: update (safe, adds columns)
Production: validate or none (never auto-modify schema)
Use Flyway/Liquibase for production migrations.
```

**7. What is the difference between LAZY and EAGER loading?**

```text
EAGER: Load related data immediately.
LAZY: Load related data only when accessed.
Default: @ManyToMany and @OneToMany = LAZY
         @ManyToOne and @OneToOne = EAGER
```

**8. How does Spring Data JPA generate queries from method names?**

```text
Parses method name into keywords:
findByEmailAndName → SELECT WHERE email = ? AND name = ?
existsByEmail → SELECT COUNT(*) > 0 WHERE email = ?
```

**9. What is HikariCP and why does Spring Boot use it?**

```text
A JDBC connection pool.
Fastest available.
Smallest footprint.
Default in Spring Boot since 2.0.
Maintains reusable database connections.
```

**10. How do Spring profiles work?**

```text
Base config + profile-specific config.
Profile config overrides base where there's a conflict.
Activated via YAML, command line, or environment variable.
```
