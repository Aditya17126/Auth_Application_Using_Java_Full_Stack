# Section 9: Security Documentation — From First Principles

Let's understand the complete security picture of this authentication application.

We will start from what IS configured, what it DOES by default, and what SHOULD be built.

---

# The Current State

The application has this dependency in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

But there is:

```text
NO SecurityConfig class
NO custom security configuration
NO password encoder bean
NO JWT implementation
NO OAuth2 configuration
```

This is important to understand.

Spring Security is ON.

But it is running with DEFAULT settings.

---

# What Happens When spring-boot-starter-security Is On The Classpath?

The moment you add the dependency, Spring Boot auto-configures security.

Even without writing a single line of security code.

---

## Default Behavior 1: All Endpoints Are Protected

Every HTTP endpoint in your application becomes protected.

Before adding security:

```text
GET /api/users → 200 OK (accessible)
POST /api/users → 200 OK (accessible)
```

After adding security:

```text
GET /api/users → 401 Unauthorized
POST /api/users → 401 Unauthorized
```

EVERY endpoint.

No exceptions.

---

## Default Behavior 2: Auto-Generated Password

When the application starts, look at the console:

```text
Using generated security password: 8a7f3c2b-1d4e-5f6a-7b8c-9d0e1f2a3b4c
```

Spring Security generates a random password every time the application starts.

The default username is:

```text
user
```

The password changes on every restart.

---

## Default Behavior 3: Login Form At /login

Spring Security provides a built-in login page:

```text
http://localhost:8083/login
```

This is a white HTML page with:

```text
Username: [         ]
Password: [         ]
[Sign In]
```

You did not create this page.

Spring Security created it for you.

---

## Default Behavior 4: Logout At /logout

```text
http://localhost:8083/logout
```

Shows a confirmation page:

```text
Are you sure you want to log out?
[Log Out]
```

---

## Default Behavior 5: CSRF Protection Enabled

CSRF stands for:

```text
Cross-Site Request Forgery
```

Spring Security enables CSRF protection by default.

What does this mean?

Every POST, PUT, DELETE request must include a CSRF token.

Without it:

```text
POST /api/users → 403 Forbidden
```

Even with correct authentication.

This is a security feature, but it causes confusion when building REST APIs.

---

## Default Behavior 6: Session-Based Authentication

By default, Spring Security uses:

```text
HTTP Session + Cookies
```

Flow:

```text
1. User logs in at /login
2. Server creates a session (JSESSIONID)
3. Session ID sent as cookie to browser
4. Browser sends cookie with every request
5. Server validates session
```

This is NOT ideal for REST APIs.

REST APIs should be stateless (no sessions).

---

## Visual Summary: Current Default Security

```text
HTTP Request Arrives
       ↓
Spring Security Filter Chain (Auto-configured)
       ↓
Is the user authenticated?
       ├── NO → Redirect to /login (for browsers)
       │        OR return 401 (for API clients)
       │
       └── YES → Is CSRF token valid?
                  ├── NO → 403 Forbidden
                  │
                  └── YES → Allow request through
                            ↓
                      Controller handles request
```

---

# Why The Default Configuration Is NOT Enough

This application is an authentication system.

It needs:

```text
1. Users register with email + password
2. Passwords stored securely (hashed)
3. Users log in and receive a token
4. Token used for subsequent requests
5. Different roles have different access levels
6. Support Google, GitHub, Facebook login
```

The default security does NONE of this.

---

# What SHOULD Be Implemented

Let's go through each security component that this application needs.

---

## 1. Custom SecurityFilterChain Bean

### What Is It?

The `SecurityFilterChain` is the central configuration for Spring Security.

It tells Spring:

```text
Which endpoints are public?
Which endpoints need authentication?
Which endpoints need specific roles?
How should authentication work?
```

### What We Need

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // Disable CSRF for REST APIs
            .csrf(csrf -> csrf.disable())

            // Configure endpoint access rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // Use stateless sessions (for JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS)
            )

            // Add JWT filter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
```

### What Each Part Does

```text
csrf.disable()
    ↓
    REST APIs don't need CSRF tokens.
    CSRF is for browser forms.

.requestMatchers("/api/auth/**").permitAll()
    ↓
    Login and Register endpoints are PUBLIC.
    Anyone can access them without authentication.

.requestMatchers("/api/admin/**").hasRole("ADMIN")
    ↓
    Only users with ADMIN role can access admin endpoints.

.anyRequest().authenticated()
    ↓
    Everything else requires login.

SessionCreationPolicy.STATELESS
    ↓
    No server-side sessions.
    Each request must carry its own authentication (JWT token).

addFilterBefore(jwtAuthFilter, ...)
    ↓
    Add our custom JWT filter BEFORE
    Spring's default authentication filter.
```

### Visual Flow With Custom Config

```text
HTTP Request Arrives
       ↓
Is endpoint /api/auth/**?
├── YES → Allow through (no auth needed)
│
└── NO → JWT Filter checks for token
         ├── No token → 401 Unauthorized
         │
         └── Has token → Validate token
                         ├── Invalid → 401 Unauthorized
                         │
                         └── Valid → Extract user info
                                    ↓
                              Check role requirements
                                    ├── Insufficient role → 403 Forbidden
                                    │
                                    └── Authorized → Controller
```

---

## 2. BCrypt Password Encoding

### The Problem Right Now

Look at the current service:

```java
@Override
public UserDto createUser(UserDto userDto) {
    return null;  // Not implemented yet
}
```

When this IS implemented, if password is stored like this:

```java
user.setPassword(userDto.getPassword());
```

The password goes into the database as:

```text
password = "secret123"
```

Plain text.

If someone gets access to the database:

```text
SELECT user_email, password FROM users;

┌───────────────────┬────────────┐
│ user_email        │ password   │
├───────────────────┼────────────┤
│ aditya@gmail.com  │ secret123  │
│ rahul@gmail.com   │ pass456    │
│ priya@gmail.com   │ mypass789  │
└───────────────────┴────────────┘
```

Every user's password is exposed.

### The Solution: BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt is a one-way hashing algorithm:

```text
Input:  "secret123"
Output: "$2a$10$8a6Kp5L3Rz3wXQ7nM9yq.Oz4r2Jv6hK8yL0mN1pQ3rS5tU7vW9xY"
```

Key properties:

```text
1. One-way: Cannot reverse the hash to get "secret123"
2. Salted: Same password produces different hashes each time
3. Slow: Intentionally slow to prevent brute-force attacks
```

### How Registration SHOULD Work

```text
User enters: "secret123"
       ↓
BCrypt encodes: "$2a$10$8a6Kp..."
       ↓
Database stores: "$2a$10$8a6Kp..."
       ↓
Original password is NEVER stored
```

### How Login SHOULD Work

```text
User enters: "secret123"
       ↓
BCrypt compares with stored hash
       ↓
passwordEncoder.matches("secret123", "$2a$10$8a6Kp...")
       ↓
Returns: true (password matches)
```

### Implementation Needed

```java
@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public UserDto createUser(UserDto userDto) {
        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setName(userDto.getName());

        // HASH the password before saving
        user.setPassword(
            passwordEncoder.encode(userDto.getPassword())
        );

        userRepository.save(user);
        return convertToDto(user);
    }
}
```

---

## 3. JWT Token Authentication

### What Is JWT?

```text
JWT = JSON Web Token
```

It is a token that the server gives to the client after successful login.

The client sends this token with every subsequent request.

### JWT Structure

```text
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZGl0eWFA.SflKxwRJSMeKKF2QT4fwpM
```

This consists of three parts separated by dots:

```text
HEADER.PAYLOAD.SIGNATURE
```

---

#### Part 1: Header

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Tells the server:

```text
Algorithm: HMAC SHA-256
Type: JWT
```

---

#### Part 2: Payload

```json
{
  "sub": "aditya@gmail.com",
  "roles": ["USER", "ADMIN"],
  "iat": 1719835200,
  "exp": 1719921600
}
```

Contains:

```text
sub: Subject (who is this token for?)
roles: User's roles
iat: Issued At (when was this created?)
exp: Expiration (when does this expire?)
```

---

#### Part 3: Signature

```text
HMACSHA256(
  base64(header) + "." + base64(payload),
  secret-key
)
```

Ensures the token has NOT been tampered with.

---

### JWT Authentication Flow

```text
┌──────────┐                    ┌──────────────┐              ┌──────────┐
│  Client  │                    │   Server     │              │ Database │
│ (Browser)│                    │ (Spring Boot)│              │ (MySQL)  │
└────┬─────┘                    └──────┬───────┘              └────┬─────┘
     │                                 │                           │
     │  1. POST /api/auth/login        │                           │
     │  { email, password }            │                           │
     │────────────────────────────────▶│                           │
     │                                 │  2. Find user by email    │
     │                                 │──────────────────────────▶│
     │                                 │                           │
     │                                 │  3. User data returned    │
     │                                 │◀──────────────────────────│
     │                                 │                           │
     │                                 │  4. Verify password       │
     │                                 │  BCrypt.matches()         │
     │                                 │                           │
     │                                 │  5. Generate JWT token    │
     │                                 │                           │
     │  6. Return JWT token            │                           │
     │◀────────────────────────────────│                           │
     │                                 │                           │
     │  7. GET /api/users              │                           │
     │  Header: Bearer <jwt-token>     │                           │
     │────────────────────────────────▶│                           │
     │                                 │  8. Validate JWT token    │
     │                                 │  9. Extract user info     │
     │                                 │ 10. Process request       │
     │                                 │──────────────────────────▶│
     │                                 │                           │
     │  11. Return response            │  12. Return data          │
     │◀────────────────────────────────│◀──────────────────────────│
     │                                 │                           │
```

---

### JWT Classes Needed

#### JwtService

```java
@Service
public class JwtService {

    private static final String SECRET_KEY = "your-256-bit-secret";
    private static final long EXPIRATION = 86400000; // 24 hours

    // Generate token for a user
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(
                new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Validate token
    public boolean isTokenValid(
            String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
            && !isTokenExpired(token);
    }
}
```

#### JwtAuthenticationFilter

```java
@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        // 1. Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        // 2. Extract username from token
        String username = jwtService.extractUsername(jwt);

        // 3. Load user from database
        UserDetails userDetails =
            userDetailsService.loadUserByUsername(username);

        // 4. Validate token
        if (jwtService.isTokenValid(jwt, userDetails)) {
            // 5. Set authentication in SecurityContext
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null,
                    userDetails.getAuthorities()
                );
            SecurityContextHolder
                .getContext()
                .setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
```

---

### JWT Flow — Step By Step

```text
Step 1: Client sends POST /api/auth/login
        with email and password

Step 2: Server finds user by email
        userRepository.findByEmail(email)

Step 3: Server verifies password
        passwordEncoder.matches(rawPassword, hashedPassword)

Step 4: If valid → Generate JWT token
        jwtService.generateToken(userDetails)

Step 5: Send token back to client
        { "token": "eyJhbGciOi..." }

Step 6: Client stores token
        (localStorage, sessionStorage, or cookie)

Step 7: Client sends token with EVERY request
        Header: Authorization: Bearer eyJhbGciOi...

Step 8: JwtAuthenticationFilter intercepts request
        Extracts token from header
        Validates token
        Sets user in SecurityContext

Step 9: Controller processes the request
```

---

## 4. UserDetailsService Implementation

### What Is It?

Spring Security needs to know:

```text
How to load a user from YOUR database?
```

Spring Security does not know about your User entity.

You need to teach it.

### Implementation Needed

```java
@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "User not found: " + email));

        return org.springframework.security.core.userdetails
            .User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(
                    "ROLE_" + role.getName()))
                .toList())
            .accountLocked(!user.isEnable())
            .build();
    }
}
```

### What This Does

```text
Spring Security calls: loadUserByUsername("aditya@gmail.com")
    ↓
We call: userRepository.findByEmail("aditya@gmail.com")
    ↓
We get: User entity from database
    ↓
We convert: User entity → Spring Security's UserDetails
    ↓
Spring Security uses: UserDetails for authentication
```

### Mapping Our Fields To Spring Security

| Our User Field   | Spring Security Field      | Purpose                  |
| ---------------- | -------------------------- | ------------------------ |
| `email`          | `username`                 | Login identifier         |
| `password`       | `password`                 | Hashed password          |
| `roles`          | `authorities`              | Permissions              |
| `enable`         | `accountNonLocked`         | Account status           |

---

## 5. Authentication Provider

### What Is It?

The component that actually verifies credentials.

```text
User submits: email + password
    ↓
AuthenticationProvider checks:
    1. Does the user exist?
    2. Is the password correct?
    3. Is the account enabled?
    ↓
Result: Authenticated or Rejected
```

### Implementation

```java
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
}

@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```

### Flow

```text
Login Request
    ↓
AuthenticationManager
    ↓
DaoAuthenticationProvider
    ├── loadUserByUsername() → Get user from DB
    ├── passwordEncoder.matches() → Verify password
    └── Check account status → Is enable = true?
    ↓
Authentication object (if successful)
    ↓
Generate JWT token
```

---

## 6. Authorization Rules

### What Is Authorization?

```text
Authentication = WHO are you?
Authorization  = WHAT can you do?
```

Example:

```text
Aditya has role: USER
    → Can access: /api/users/profile
    → Cannot access: /api/admin/dashboard

System Admin has roles: USER, ADMIN
    → Can access: /api/users/profile
    → Can access: /api/admin/dashboard
```

### How To Configure

```java
.authorizeHttpRequests(auth -> auth
    // Public endpoints
    .requestMatchers(
        "/api/auth/login",
        "/api/auth/register"
    ).permitAll()

    // Admin only
    .requestMatchers("/api/admin/**")
        .hasRole("ADMIN")

    // User or Admin
    .requestMatchers("/api/users/**")
        .hasAnyRole("USER", "ADMIN")

    // Everything else needs authentication
    .anyRequest().authenticated()
)
```

### Authorization Flow

```text
Request: GET /api/admin/dashboard
    ↓
JWT Filter: Token valid → User: Aditya, Roles: [USER]
    ↓
Authorization Check:
    /api/admin/** requires ADMIN role
    Aditya has USER role only
    ↓
Result: 403 Forbidden
```

```text
Request: GET /api/admin/dashboard
    ↓
JWT Filter: Token valid → User: SystemAdmin, Roles: [USER, ADMIN]
    ↓
Authorization Check:
    /api/admin/** requires ADMIN role
    SystemAdmin has ADMIN role
    ↓
Result: 200 OK
```

---

## 7. CORS Configuration

### What Is CORS?

```text
CORS = Cross-Origin Resource Sharing
```

Problem:

```text
Frontend runs on: http://localhost:3000 (React)
Backend runs on:  http://localhost:8083 (Spring Boot)
```

These are different origins.

By default, browsers BLOCK requests between different origins.

### Why?

Security.

Without CORS protection:

```text
Evil website: http://evil.com
    ↓
Makes request to: http://your-api.com/api/users
    ↓
Steals user data
```

CORS prevents this.

### Implementation Needed

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(List.of(
        "http://localhost:3000"
    ));

    configuration.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    ));

    configuration.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type"
    ));

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
}
```

Then in SecurityFilterChain:

```java
http.cors(cors -> cors
    .configurationSource(corsConfigurationSource()));
```

---

## 8. OAuth2 Integration

### Why OAuth2?

The application has a Provider enum:

```java
public enum Provider {
   LOCAL,
   GOOGLE,
   GITHUB,
   FACEBOOK
}
```

This means the app is DESIGNED to support social login.

But OAuth2 is NOT configured yet.

---

### What Is OAuth2?

```text
OAuth2 = Open Authorization 2.0
```

It allows users to log in using their existing accounts:

```text
"Sign in with Google"
"Continue with GitHub"
"Login with Facebook"
```

The user never gives YOUR app their Google password.

Instead:

```text
1. User clicks "Sign in with Google"
2. User is redirected to Google's login page
3. User logs into Google
4. Google sends an authorization code to YOUR app
5. Your app exchanges the code for user information
6. Your app creates/updates the user account
7. User is logged in
```

---

### OAuth2 Flow Diagram

```text
┌──────────┐          ┌──────────────┐          ┌──────────────┐
│  Client  │          │  Your Server │          │   Google     │
│ (Browser)│          │ (Spring Boot)│          │  (Provider)  │
└────┬─────┘          └──────┬───────┘          └──────┬───────┘
     │                       │                         │
     │ 1. Click              │                         │
     │ "Sign in with Google" │                         │
     │──────────────────────▶│                         │
     │                       │                         │
     │ 2. Redirect to Google │                         │
     │◀──────────────────────│                         │
     │                       │                         │
     │ 3. Login at Google    │                         │
     │────────────────────────────────────────────────▶│
     │                       │                         │
     │ 4. Google asks:       │                         │
     │ "Allow this app       │                         │
     │  access to your info?"│                         │
     │◀────────────────────────────────────────────────│
     │                       │                         │
     │ 5. User clicks Allow  │                         │
     │────────────────────────────────────────────────▶│
     │                       │                         │
     │ 6. Redirect with      │                         │
     │    authorization code │                         │
     │◀────────────────────────────────────────────────│
     │                       │                         │
     │ 7. Send code to server│                         │
     │──────────────────────▶│                         │
     │                       │ 8. Exchange code for    │
     │                       │    access token         │
     │                       │────────────────────────▶│
     │                       │                         │
     │                       │ 9. Return access token  │
     │                       │◀────────────────────────│
     │                       │                         │
     │                       │ 10. Get user info       │
     │                       │     with access token   │
     │                       │────────────────────────▶│
     │                       │                         │
     │                       │ 11. Return user info    │
     │                       │     (email, name, photo)│
     │                       │◀────────────────────────│
     │                       │                         │
     │                       │ 12. Create/update User  │
     │                       │     Set provider=GOOGLE │
     │                       │                         │
     │ 13. Return JWT token  │                         │
     │◀──────────────────────│                         │
     │                       │                         │
```

---

### Configuration Needed

#### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
            scope: email, profile

          github:
            client-id: your-github-client-id
            client-secret: your-github-client-secret
            scope: user:email, read:user

          facebook:
            client-id: your-facebook-client-id
            client-secret: your-facebook-client-secret
            scope: email, public_profile
```

#### OAuth2 Success Handler

```java
@Component
public class OAuth2SuccessHandler
    extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        OAuth2User oAuth2User =
            (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Check if user already exists
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                // Create new user
                User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .provider(Provider.GOOGLE) // or GITHUB, FACEBOOK
                    .build();
                return userRepository.save(newUser);
            });

        // Generate JWT token
        String token = jwtService.generateToken(user);

        // Redirect with token
        response.sendRedirect(
            "http://localhost:3000/oauth2/callback?token=" + token);
    }
}
```

---

# Current Security Vulnerabilities

These are real security problems in the current application.

---

## Vulnerability 1: Plain Text Passwords

### Problem

The service layer has no password encoding:

```java
@Override
public UserDto createUser(UserDto userDto) {
    return null;  // Not implemented yet
}
```

When implemented naively:

```java
user.setPassword(userDto.getPassword());
// "secret123" stored directly in database
```

### Risk

```text
Database breach → ALL passwords exposed
SQL injection → ALL passwords exposed
Database backup stolen → ALL passwords exposed
```

### Fix

```java
user.setPassword(passwordEncoder.encode(userDto.getPassword()));
// "$2a$10$8a6..." stored in database
```

---

## Vulnerability 2: Database Credentials Hardcoded

### Problem

In `application-dev.yml`:

```yaml
spring:
  datasource:
   url: jdbc:mysql://localhost:3306/auth_application_java
   username: root
   password: Aditya13325@
```

The database password is in plain text in a file that could be:

```text
1. Committed to Git → Everyone on the team sees it
2. Pushed to GitHub → The entire world sees it
3. Leaked in logs → Anyone with log access sees it
```

### Fix

Use environment variables:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

Or use Spring Cloud Vault, AWS Secrets Manager, etc.

---

## Vulnerability 3: No HTTPS Enforcement

### Problem

The application runs on:

```text
http://localhost:8083
```

HTTP, not HTTPS.

Data transmitted over HTTP is:

```text
Plain text
Visible to anyone on the network
Can be intercepted (Man-in-the-Middle attack)
```

Including:

```text
Passwords
JWT tokens
User data
```

### Fix

```java
http.requiresChannel(channel -> channel
    .anyRequest().requiresSecure()
);
```

And configure SSL/TLS certificate.

---

## Vulnerability 4: No Rate Limiting

### Problem

Nothing prevents:

```text
POST /api/auth/login (attempt 1)
POST /api/auth/login (attempt 2)
POST /api/auth/login (attempt 3)
...
POST /api/auth/login (attempt 10000)
```

An attacker can try thousands of passwords per second.

This is called:

```text
Brute Force Attack
```

### Fix

Options:

```text
1. Spring Security's built-in attempt limiting
2. Bucket4j library for rate limiting
3. API Gateway rate limiting (e.g., Spring Cloud Gateway)
4. Fail2Ban at the server level
```

Example implementation:

```java
// Track login attempts
private Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();

public void loginFailed(String email) {
    int attempts = loginAttempts.getOrDefault(email, 0);
    loginAttempts.put(email, attempts + 1);
}

public boolean isBlocked(String email) {
    return loginAttempts.getOrDefault(email, 0) >= 5;
}
```

---

## Vulnerability 5: No Input Validation In Service Layer

### Problem

The service layer does not validate input:

```java
@Override
public UserDto createUser(UserDto userDto) {
    return null;  // No validation
}
```

Without validation:

```text
email = null → NullPointerException
email = "" → Empty email saved
email = "not-an-email" → Invalid email saved
password = "" → Empty password saved
name = "<script>alert('XSS')</script>" → XSS attack
```

### Fix

Add validation annotations:

```java
public class UserDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

And use `@Valid` in controller:

```java
@PostMapping("/register")
public ResponseEntity<UserDto> register(
        @Valid @RequestBody UserDto userDto) {
    return ResponseEntity.ok(userService.createUser(userDto));
}
```

Note: The project already has `spring-boot-starter-validation` dependency.

---

## Vulnerability 6: Using Root Database User

### Problem

```yaml
username: root
```

The application connects to MySQL as root.

Root has ALL privileges:

```text
DROP DATABASE auth_application_java;  ← Possible
DROP TABLE users;                     ← Possible
CREATE USER;                          ← Possible
```

If an attacker gets SQL injection access, they have FULL database control.

### Fix

Create a dedicated database user with minimal privileges:

```sql
CREATE USER 'auth_app_user'@'localhost'
    IDENTIFIED BY 'strong_password';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON auth_application_java.*
    TO 'auth_app_user'@'localhost';
```

This user can only:

```text
SELECT → Read data
INSERT → Create data
UPDATE → Modify data
DELETE → Remove data
```

Cannot:

```text
DROP tables
CREATE new tables (in production)
Manage other users
Access other databases
```

---

## Vulnerability 7: No Token Expiration Strategy

### Problem

Without JWT implementation, there is no token management.

When JWT IS implemented, without proper expiration:

```text
Token never expires → Stolen token usable forever
No refresh token → User must re-login frequently
No token revocation → Cannot invalidate compromised tokens
```

### Fix

```text
Access Token:  Short-lived (15 minutes)
Refresh Token: Long-lived (7 days)
Token Blacklist: Store revoked tokens in Redis/DB
```

---

## All Vulnerabilities Summary

| #  | Vulnerability                  | Severity | Status         |
| -- | ------------------------------ | -------- | -------------- |
| 1  | Plain text passwords           | CRITICAL | Not Implemented|
| 2  | Hardcoded DB credentials       | HIGH     | In YAML file   |
| 3  | No HTTPS                       | HIGH     | HTTP only      |
| 4  | No rate limiting               | MEDIUM   | Not Implemented|
| 5  | No input validation            | MEDIUM   | Not Implemented|
| 6  | Root database user             | HIGH     | Using root     |
| 7  | No token management            | HIGH     | Not Implemented|

---

# Security Completion Roadmap

Here is a step-by-step plan to complete the security layer.

---

## Phase 1: Foundation (Must Have)

```text
Step 1: Create SecurityConfig class
        → Define SecurityFilterChain bean
        → Disable CSRF for REST API
        → Set stateless session policy

Step 2: Add PasswordEncoder bean
        → BCryptPasswordEncoder
        → Encode passwords during registration

Step 3: Implement UserServiceImpl
        → Use PasswordEncoder when saving users
        → Use UserRepository for database operations

Step 4: Implement CustomUserDetailsService
        → Load user from database
        → Convert User → UserDetails
        → Map roles to authorities
```

---

## Phase 2: JWT Authentication (Must Have)

```text
Step 5: Add JWT dependency (jjwt)
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>

Step 6: Create JwtService
        → generateToken()
        → validateToken()
        → extractUsername()

Step 7: Create JwtAuthenticationFilter
        → Extract token from header
        → Validate token
        → Set SecurityContext

Step 8: Create AuthController
        → POST /api/auth/register
        → POST /api/auth/login
        → POST /api/auth/refresh
```

---

## Phase 3: Authorization (Must Have)

```text
Step 9:  Configure endpoint access rules
         → Public: /api/auth/**
         → User: /api/users/**
         → Admin: /api/admin/**

Step 10: Add role-based access control
         → @PreAuthorize("hasRole('ADMIN')")
         → Method-level security
```

---

## Phase 4: OAuth2 Social Login (Nice To Have)

```text
Step 11: Register apps with providers
         → Google Cloud Console
         → GitHub Developer Settings
         → Facebook Developer Portal

Step 12: Configure OAuth2 in application.yml
         → Client IDs and Secrets

Step 13: Create OAuth2SuccessHandler
         → Handle successful social login
         → Create/update user in database
         → Set correct Provider enum value
         → Generate JWT token

Step 14: Test each provider
         → Google login flow
         → GitHub login flow
         → Facebook login flow
```

---

## Phase 5: Hardening (Production Ready)

```text
Step 15: Move credentials to environment variables
         → DB_URL, DB_USERNAME, DB_PASSWORD
         → JWT_SECRET
         → OAuth2 client secrets

Step 16: Add rate limiting
         → Login attempt limiting
         → API rate limiting

Step 17: Configure CORS
         → Allow only your frontend origin

Step 18: Add HTTPS
         → SSL/TLS certificate
         → Redirect HTTP → HTTPS

Step 19: Create dedicated DB user
         → Minimal privileges

Step 20: Add input validation
         → @Valid on all endpoints
         → Custom validators
```

---

## Phase Diagram

```text
Phase 1: Foundation
│  SecurityConfig
│  PasswordEncoder
│  UserDetailsService
│
▼
Phase 2: JWT
│  JwtService
│  JwtFilter
│  AuthController
│
▼
Phase 3: Authorization
│  Endpoint rules
│  Role-based access
│
▼
Phase 4: OAuth2
│  Google login
│  GitHub login
│  Facebook login
│
▼
Phase 5: Hardening
│  Environment variables
│  Rate limiting
│  CORS
│  HTTPS
│
▼
Production Ready ✓
```

---

# Complete Security Architecture — Target State

```text
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENT (Browser / Mobile)                   │
│─────────────────────────────────────────────────────────────────│
│  Sends: Authorization: Bearer <JWT Token>                       │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING SECURITY FILTER CHAIN                 │
│─────────────────────────────────────────────────────────────────│
│                                                                 │
│  ┌─────────────────┐    ┌──────────────────┐                    │
│  │   CORS Filter   │───▶│  CSRF Filter     │                    │
│  │ (Allow origins) │    │ (Disabled for    │                    │
│  └─────────────────┘    │  REST APIs)      │                    │
│                         └────────┬─────────┘                    │
│                                  │                              │
│                                  ▼                              │
│                    ┌─────────────────────────┐                  │
│                    │  JWT Authentication     │                  │
│                    │  Filter                 │                  │
│                    │                         │                  │
│                    │  1. Extract token       │                  │
│                    │  2. Validate token      │                  │
│                    │  3. Load user           │                  │
│                    │  4. Set SecurityContext  │                  │
│                    └────────────┬────────────┘                  │
│                                 │                               │
│                                 ▼                               │
│                    ┌─────────────────────────┐                  │
│                    │  Authorization Filter   │                  │
│                    │                         │                  │
│                    │  Check roles and        │                  │
│                    │  permissions             │                  │
│                    └────────────┬────────────┘                  │
│                                 │                               │
└─────────────────────────────────┼───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CONTROLLER LAYER                            │
│─────────────────────────────────────────────────────────────────│
│  AuthController  │  UserController  │  AdminController          │
└──────────────────┼──────────────────┼───────────────────────────┘
                   │                  │
                   ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                               │
│─────────────────────────────────────────────────────────────────│
│  UserServiceImpl (PasswordEncoder, UserRepository)              │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     REPOSITORY LAYER                            │
│─────────────────────────────────────────────────────────────────│
│  UserRepository (JPA → Hibernate → MySQL)                       │
└─────────────────────────────────────────────────────────────────┘
```

---

# Interview Questions and Answers

## Q1: What happens if you add spring-boot-starter-security without any configuration?

```text
All endpoints are secured by default.
A random password is generated on startup.
Default username is "user".
A login form is available at /login.
CSRF protection is enabled.
Session-based authentication is used.
```

## Q2: What is the difference between Authentication and Authorization?

```text
Authentication: Verifying WHO you are.
    "Are you really Aditya?"
    Done through: username + password, or JWT token.

Authorization: Verifying WHAT you can do.
    "Can Aditya access the admin dashboard?"
    Done through: Roles and permissions.
```

## Q3: Why disable CSRF for REST APIs?

```text
CSRF attacks target browser-based forms.
REST APIs use tokens (JWT) in headers.
Tokens are not automatically sent by browsers.
So CSRF protection is unnecessary for stateless REST APIs.
```

## Q4: What is the difference between session-based and token-based auth?

```text
Session-based:
    Server stores session in memory.
    Client sends session ID as cookie.
    Stateful — server must remember sessions.
    Does not scale easily.

Token-based (JWT):
    Server generates token, sends to client.
    Client sends token in Authorization header.
    Stateless — server validates token without storing it.
    Scales easily across multiple servers.
```

## Q5: Why is BCrypt preferred over MD5 or SHA for passwords?

```text
MD5/SHA:
    Fast — can hash millions per second.
    Attacker can brute-force quickly.
    Not designed for passwords.

BCrypt:
    Intentionally slow (adjustable work factor).
    Includes salt (prevents rainbow table attacks).
    Designed specifically for passwords.
    Each hash takes ~100ms (brute-force resistant).
```

## Q6: What is a JWT refresh token?

```text
Access Token: Short-lived (15 min). Used for API requests.
Refresh Token: Long-lived (7 days). Used to get new access tokens.

Flow:
1. Login → Get access token + refresh token
2. Use access token for 15 minutes
3. Access token expires
4. Send refresh token to /api/auth/refresh
5. Get new access token
6. Continue using API

Why: If access token is stolen, attacker has only 15 minutes.
```

---

# Summary

```text
Current State:
  spring-boot-starter-security is on classpath
  NO custom configuration
  Default: all endpoints secured, auto-generated password
  Default: session-based, CSRF enabled

What Is Missing:
  SecurityFilterChain configuration
  Password encoding (BCrypt)
  JWT authentication
  UserDetailsService implementation
  OAuth2 social login
  CORS configuration
  Rate limiting
  HTTPS enforcement
  Input validation

Critical Vulnerabilities:
  Passwords would be stored as plain text
  Database credentials hardcoded in YAML
  Using MySQL root user
  No rate limiting on login
  No HTTPS

Roadmap:
  Phase 1 → SecurityConfig + PasswordEncoder
  Phase 2 → JWT authentication
  Phase 3 → Authorization rules
  Phase 4 → OAuth2 social login
  Phase 5 → Production hardening
```
