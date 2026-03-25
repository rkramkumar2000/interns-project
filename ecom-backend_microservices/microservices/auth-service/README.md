# Auth Service

## Overview
The Auth Service is a critical microservice responsible for user authentication, authorization, and session management in the e-commerce platform. It provides JWT-based authentication with refresh tokens, user registration, role-based access control, and integrates with Redis for session caching.

## Architecture Position
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│ API Gateway │────▶│Auth Service │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                         ┌─────┴─────┐
                                         │           │
                                    ┌────▼───┐ ┌────▼───┐
                                    │PostgreSQL│ │ Redis  │
                                    └────────┘ └────────┘
```

## Technologies Used

### Core Framework
- **Spring Boot 3.2.0** - Modern Java framework for building microservices
- **Spring Security 6.2** - Comprehensive security framework for authentication and authorization
- **Spring Data JPA** - ORM for database operations with PostgreSQL

### Security & Authentication
- **JWT (JSON Web Tokens)** - Stateless authentication mechanism
  - Why: Enables distributed authentication across microservices without server-side sessions
  - Implementation: Dual token system with short-lived access tokens (15 min) and long-lived refresh tokens (7 days)
- **Refresh Tokens** - Secure token refresh mechanism
  - Why: Reduces security risk while maintaining user convenience
  - Features: Token rotation, database persistence, automatic cleanup
- **BCrypt** - Password hashing algorithm
  - Why: Industry-standard for secure password storage
- **Spring Security** - Authentication and authorization framework
  - Why: Provides comprehensive security features out-of-the-box

### Data Storage
- **PostgreSQL 15** - Primary relational database
  - Why: ACID compliance, excellent performance, and native JSON support
- **Redis 7** - In-memory data store for caching
  - Why: Fast session storage and user data caching

### Service Discovery & Configuration
- **Eureka Client** - Service registration
  - Why: Dynamic service discovery in microservices architecture
- **Spring Cloud Config Client** - Centralized configuration
  - Why: Externalized configuration management

### Messaging & Events
- **Apache Kafka** - Event streaming platform
  - Why: Reliable asynchronous communication for auth events

### Monitoring & Documentation
- **Spring Boot Actuator** - Production-ready features
  - Why: Health checks, metrics, and monitoring endpoints
- **Swagger/OpenAPI 3** - API documentation
  - Why: Interactive API documentation and testing

## Project Structure

```
auth-service/
├── src/main/java/com/ecommerce/auth/
│   ├── AuthServiceApplication.java          # Main application class
│   ├── config/
│   │   └── ApplicationConfig.java           # Bean configurations
│   ├── controller/
│   │   └── AuthController.java              # REST endpoints
│   ├── dto/                                 # Data Transfer Objects
│   │   ├── JwtResponse.java                # JWT token response
│   │   ├── LoginRequest.java               # Login credentials
│   │   ├── MessageResponse.java            # Generic message
│   │   ├── SignupRequest.java              # Registration data
│   │   └── UserDTO.java                    # User profile data
│   ├── event/
│   │   ├── AuthEvent.java                  # Base auth event
│   │   └── UserRegisteredEvent.java        # Registration event
│   ├── exception/
│   │   └── GlobalExceptionHandler.java     # Centralized error handling
│   ├── model/                              # JPA Entities
│   │   ├── AppRole.java                    # Role enumeration
│   │   ├── Role.java                       # Role entity
│   │   └── User.java                       # User entity
│   ├── repository/                         # Data access layer
│   │   ├── RoleRepository.java             # Role operations
│   │   └── UserRepository.java             # User operations
│   ├── security/                           # Security components
│   │   ├── AuthEntryPointJwt.java         # Unauthorized handler
│   │   ├── AuthTokenFilter.java           # JWT filter
│   │   ├── JwtUtils.java                  # JWT utilities
│   │   ├── UserDetailsImpl.java           # User details adapter
│   │   ├── UserDetailsServiceImpl.java    # User loading service
│   │   └── WebSecurityConfig.java         # Security configuration
│   └── service/                            # Business logic
│       ├── AuthService.java                # Service interface
│       └── AuthServiceImpl.java            # Service implementation
├── src/main/resources/
│   ├── bootstrap.yml                       # Config server connection
│   └── application.yml                     # Local configuration
└── pom.xml                                 # Maven dependencies
```

## Key Components

### 1. AuthController
Main REST controller handling authentication endpoints:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest)
    
    @PostMapping("/signup") 
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest)
    
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String token)
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token)
    
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser()
    
    @PutMapping("/user")
    public ResponseEntity<?> updateUser(@Valid @RequestBody UserDTO userDTO)
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request)
    
    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsernameAvailability(@PathVariable String username)
    
    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmailAvailability(@PathVariable String email)
}
```

### 2. JwtUtils
Handles JWT token generation and validation:

```java
@Component
public class JwtUtils {
    
    public String generateJwtToken(Authentication authentication)
    public String generateTokenFromUsername(String username)
    public String getUserNameFromJwtToken(String token)
    public Long getUserIdFromJwtToken(String token)
    public List<String> getRolesFromJwtToken(String token)
    public boolean validateJwtToken(String authToken)
}
```

### 3. AuthService
Core business logic for authentication:

```java
public interface AuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    MessageResponse registerUser(SignupRequest signupRequest);
    MessageResponse logoutUser(String token);
    JwtResponse refreshToken(String token);
    UserDTO getCurrentUser(String username);
    MessageResponse updateUser(String username, UserDTO userDTO);
    MessageResponse changePassword(String username, ChangePasswordRequest request);
    boolean checkUsernameAvailability(String username);
    boolean checkEmailAvailability(String email);
}
```

## Database Schema

### Tables

#### users
| Column | Type | Description |
|--------|------|-------------|
| user_id | BIGINT | Primary key |
| username | VARCHAR(50) | Unique username |
| email | VARCHAR(100) | Unique email |
| password | VARCHAR(120) | BCrypt hash |
| first_name | VARCHAR(50) | User's first name |
| last_name | VARCHAR(50) | User's last name |
| phone_number | VARCHAR(20) | Contact number |
| active | BOOLEAN | Account status |
| email_verified | BOOLEAN | Email verification |
| created_at | TIMESTAMP | Registration date |
| updated_at | TIMESTAMP | Last update |
| last_login | TIMESTAMP | Last login time |

#### roles
| Column | Type | Description |
|--------|------|-------------|
| role_id | INTEGER | Primary key |
| role_name | VARCHAR(20) | Role name (ROLE_USER, ROLE_ADMIN, ROLE_SELLER) |

#### user_roles
| Column | Type | Description |
|--------|------|-------------|
| user_id | BIGINT | Foreign key to users |
| role_id | INTEGER | Foreign key to roles |

## API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signin` | User login (returns access & refresh tokens) |
| POST | `/api/auth/signup` | User registration |
| POST | `/api/auth/refresh?refreshToken={token}` | Refresh access token |
| GET | `/api/auth/check-username/{username}` | Check username availability |
| GET | `/api/auth/check-email/{email}` | Check email availability |

### Protected Endpoints (JWT Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/logout` | User logout (invalidates refresh token) |
| GET | `/api/auth/user` | Get current user profile |
| PUT | `/api/auth/user` | Update user profile |
| POST | `/api/auth/change-password` | Change password |

## Refresh Token Implementation

The auth service now implements a secure refresh token mechanism:

### Features
- **Dual Token System**: Short-lived access tokens (15 min) and long-lived refresh tokens (7 days)
- **Token Rotation**: New refresh token issued on each refresh (prevents replay attacks)
- **Database Persistence**: Refresh tokens stored in PostgreSQL with proper indexing
- **Automatic Cleanup**: Scheduled task removes expired tokens daily
- **Secure Logout**: Refresh tokens are invalidated on logout

### Token Flow
1. **Login**: Returns both access token and refresh token
2. **API Calls**: Use access token in Authorization header
3. **Token Refresh**: When access token expires, use refresh token to get new tokens
4. **Logout**: Invalidates refresh token in database

### Security Benefits
- Reduced attack window with short-lived access tokens
- Token rotation prevents token replay attacks
- Database storage allows token revocation
- One active refresh token per user

For detailed implementation guide, see [REFRESH_TOKEN_IMPLEMENTATION.md](./REFRESH_TOKEN_IMPLEMENTATION.md)

## Configuration

### Environment Variables
```yaml
# Database
POSTGRES_HOST: localhost
POSTGRES_PORT: 5432
POSTGRES_DB: auth_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres

# Redis
REDIS_HOST: localhost
REDIS_PORT: 6379

# JWT
JWT_SECRET: mySecretKey
JWT_EXPIRATION_MS: 900000  # 15 minutes (access token)
JWT_REFRESH_EXPIRATION_MS: 604800000  # 7 days (refresh token)

# Kafka
KAFKA_BOOTSTRAP_SERVERS: localhost:9092
```

### Application Properties
```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service
  
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/auth_db
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

app:
  jwt:
    secret: ${JWT_SECRET:mySecretKey}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}
    refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:604800000}
```

## Events Published

The Auth Service publishes the following events to Kafka:

### USER_REGISTERED
```json
{
  "eventType": "USER_REGISTERED",
  "timestamp": "2024-01-20T10:30:00Z",
  "userId": 123,
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"]
}
```

### USER_UPDATED
```json
{
  "eventType": "USER_UPDATED",
  "timestamp": "2024-01-20T10:35:00Z",
  "userId": 123,
  "updatedFields": ["firstName", "lastName", "phoneNumber"]
}
```

### PASSWORD_CHANGED
```json
{
  "eventType": "PASSWORD_CHANGED",
  "timestamp": "2024-01-20T10:40:00Z",
  "userId": 123,
  "username": "john_doe"
}
```

## Security Features

### 1. JWT Token Structure
```json
{
  "sub": "john_doe",
  "userId": 123,
  "email": "john@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1705750200,
  "exp": 1705836600
}
```

### 2. Password Requirements
- Minimum 6 characters
- Must contain at least one uppercase letter
- Must contain at least one lowercase letter
- Must contain at least one digit

### 3. Session Management
- JWT tokens stored in Redis with TTL
- Automatic session invalidation on logout
- Token refresh mechanism for extended sessions

### 4. CORS Configuration
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing with cURL

1. **Register a new user:**
```bash
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

2. **Login:**
```bash
curl -X POST http://localhost:8081/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'
```

3. **Get user profile:**
```bash
curl -X GET http://localhost:8081/api/auth/user \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Monitoring

### Health Check
```
GET http://localhost:8081/actuator/health
```

### Metrics
```
GET http://localhost:8081/actuator/metrics
```

### Prometheus Metrics
```
GET http://localhost:8081/actuator/prometheus
```

## Troubleshooting

### Common Issues

1. **JWT Token Invalid**
   - Check token expiration
   - Verify JWT secret matches across services
   - Ensure proper Bearer token format

2. **User Not Found**
   - Verify database connection
   - Check if user exists in database
   - Ensure proper username/email format

3. **Redis Connection Failed**
   - Verify Redis is running
   - Check Redis connection settings
   - Monitor Redis memory usage

## Best Practices

1. **Security**
   - Never log sensitive information
   - Use strong JWT secrets in production
   - Implement rate limiting for auth endpoints
   - Enable HTTPS in production

2. **Performance**
   - Cache user details in Redis
   - Use connection pooling for database
   - Implement pagination for user lists
   - Monitor JWT token generation time

3. **Reliability**
   - Implement retry logic for Redis operations
   - Use circuit breakers for external calls
   - Maintain audit logs for auth events
   - Regular backup of user data

## Future Enhancements

1. **Multi-Factor Authentication (MFA)**
   - SMS-based OTP
   - TOTP (Google Authenticator)
   - Email verification codes

2. **Social Login Integration**
   - OAuth2 with Google
   - Facebook login
   - GitHub authentication

3. **Advanced Security Features**
   - Account lockout after failed attempts
   - IP-based access control
   - Device fingerprinting
   - Anomaly detection

4. **User Management**
   - Bulk user import/export
   - User activity tracking
   - Password policy enforcement
   - Account recovery workflows 