# WebSecurityConfig.java Documentation

## Overview
`WebSecurityConfig.java` is the main Spring Security configuration class that defines the security rules, authentication mechanisms, and authorization policies for the entire application.

## Class Declaration
```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig
```

### Annotations:
- **@Configuration**: Marks this as a Spring configuration class
- **@EnableWebSecurity**: Enables Spring Security's web security support

## Dependencies and Autowired Components

```java
@Autowired
UserDetailsServiceImpl userDetailsService;

@Autowired
private AuthEntryPointJwt unauthorizedHandler;
```

- **userDetailsService**: Custom implementation for loading user details from database
- **unauthorizedHandler**: Handles authentication exceptions and unauthorized access attempts

## Bean Definitions

### 1. AuthTokenFilter Bean
```java
@Bean
public AuthTokenFilter authenticationJwtTokenFilter()
```
**Purpose**: Creates the JWT authentication filter that intercepts all requests
**Returns**: New instance of AuthTokenFilter
**Usage**: Added to the security filter chain before UsernamePasswordAuthenticationFilter

### 2. DaoAuthenticationProvider Bean
```java
@Bean
public DaoAuthenticationProvider authenticationProvider()
```
**Purpose**: Configures the authentication provider with custom user details service
**Configuration**:
- Sets custom UserDetailsService
- Sets password encoder (BCrypt)
**Returns**: Configured DaoAuthenticationProvider

### 3. AuthenticationManager Bean
```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
```
**Purpose**: Provides the authentication manager for the application
**Parameters**: AuthenticationConfiguration injected by Spring
**Returns**: AuthenticationManager instance

### 4. PasswordEncoder Bean
```java
@Bean
public PasswordEncoder passwordEncoder()
```
**Purpose**: Defines the password encoding strategy
**Returns**: BCryptPasswordEncoder instance
**Security**: Uses BCrypt hashing algorithm for password storage

### 5. SecurityFilterChain Bean (Core Security Configuration)
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http)
```

**Purpose**: Main security configuration defining all security rules

**Configuration Details**:

#### CSRF Protection
```java
http.csrf(csrf -> csrf.disable())
```
- Disabled for stateless JWT authentication
- Not needed as tokens are validated on each request

#### CORS Configuration
```java
.cors(cors -> {})
```
- Enables CORS with default configuration
- Actual CORS settings defined in WebConfig.java

#### Exception Handling
```java
.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
```
- Sets custom authentication entry point
- Returns JSON error responses for unauthorized access

#### Session Management
```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
- Configures stateless session policy
- No server-side sessions created

#### Authorization Rules
```java
.authorizeHttpRequests(auth ->
    auth.requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/v3/api-docs/**").permitAll()
        .requestMatchers("/h2-console/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/seller/**").hasAnyRole("ADMIN","SELLER")
        .requestMatchers("/api/public/**").permitAll()
        .requestMatchers("/swagger-ui/**").permitAll()
        .requestMatchers("/api/test/**").permitAll()
        .requestMatchers("/images/**").permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .anyRequest().authenticated()
)
```

**Endpoint Access Rules**:
- `/api/auth/**`: Public (login, signup, etc.)
- `/api/admin/**`: ADMIN role only
- `/api/seller/**`: ADMIN or SELLER roles
- `/api/public/**`: Public access
- `/swagger-ui/**`, `/v3/api-docs/**`: API documentation
- `/h2-console/**`: H2 database console (dev only)
- `/images/**`: Static image resources
- `OPTIONS /**`: Preflight CORS requests
- All other endpoints: Require authentication

#### Authentication Provider
```java
http.authenticationProvider(authenticationProvider());
```
- Sets the custom authentication provider

#### JWT Filter Addition
```java
http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
```
- Adds JWT filter before Spring's default authentication filter
- Ensures JWT validation happens first

#### Security Headers
```java
http.headers(headers -> headers.frameOptions(
    frameOptions -> frameOptions.sameOrigin()));
```
- Allows same-origin framing (needed for H2 console)

### 6. WebSecurityCustomizer Bean
```java
@Bean
public WebSecurityCustomizer webSecurityCustomizer()
```
**Purpose**: Configures web security to ignore certain patterns
**Ignored Paths**:
- `/v2/api-docs`: Swagger 2.x documentation
- `/configuration/**`: Swagger configuration
- `/swagger-resources/**`: Swagger resources
- `/swagger-ui.html`: Swagger UI
- `/webjars/**`: Web JARs for UI libraries

### 7. CommandLineRunner Bean (Data Initialization)
```java
@Bean
public CommandLineRunner initData(RoleRepository roleRepository, 
                                 UserRepository userRepository, 
                                 PasswordEncoder passwordEncoder)
```

**Purpose**: Initializes default roles and test users on application startup

**Initialization Flow**:

1. **Role Creation**:
   - Creates ROLE_USER if not exists
   - Creates ROLE_SELLER if not exists
   - Creates ROLE_ADMIN if not exists

2. **Test User Creation**:
   - **user1**: Basic user with ROLE_USER
   - **seller1**: Seller with ROLE_SELLER
   - **admin**: Administrator with all roles (USER, SELLER, ADMIN)

**Test Credentials**:
```
Username: user1, Password: password1, Role: USER
Username: seller1, Password: password2, Role: SELLER
Username: admin, Password: adminPass, Roles: USER, SELLER, ADMIN
```

## Security Considerations

### Strengths:
1. Stateless JWT authentication
2. Role-based access control
3. BCrypt password encryption
4. Proper exception handling
5. CORS support

### Areas for Improvement:
1. Test users should not be created in production
2. H2 console access should be disabled in production
3. Consider implementing method-level security with @PreAuthorize
4. Add rate limiting for authentication endpoints
5. Implement refresh token mechanism

## Usage Example

This configuration is automatically loaded by Spring Boot and applies to all HTTP requests. The security rules are enforced in the following order:

1. Request arrives
2. AuthTokenFilter checks for JWT
3. If valid JWT, authentication is set
4. Authorization rules are checked
5. If authorized, request proceeds to controller
6. If not authorized, 401/403 response is returned

## Related Components
- **AuthTokenFilter**: JWT validation filter
- **AuthEntryPointJwt**: Error handling
- **UserDetailsServiceImpl**: User loading
- **JwtUtils**: Token operations 