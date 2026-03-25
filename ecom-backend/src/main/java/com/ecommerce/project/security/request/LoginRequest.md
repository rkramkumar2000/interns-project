# LoginRequest.java Documentation

## Overview
`LoginRequest.java` is a Data Transfer Object (DTO) that encapsulates user login credentials. It serves as the request body for authentication endpoints and includes validation constraints.

## Class Declaration
```java
public class LoginRequest
```

### Class Type:
- Plain Java class (POJO)
- No inheritance or interface implementation
- Serves as request DTO

## Fields

### 1. Username Field
```java
@NotBlank
private String username;
```

**Annotations**:
- **@NotBlank**: JSR-303 validation constraint
  - Ensures field is not null
  - Ensures field is not empty
  - Ensures field is not only whitespace

**Purpose**: Stores the user's login identifier

### 2. Password Field
```java
@NotBlank
private String password;
```

**Annotations**:
- **@NotBlank**: Same validation as username

**Purpose**: Stores the user's password for authentication

## Methods

### Getters and Setters

#### getUsername()
```java
public String getUsername() {
    return username;
}
```
- **Returns**: The username value
- **Access Level**: Public

#### setUsername()
```java
public void setUsername(String username) {
    this.username = username;
}
```
- **Parameters**: `String username` - username to set
- **Access Level**: Public

#### getPassword()
```java
public String getPassword() {
    return password;
}
```
- **Returns**: The password value
- **Access Level**: Public
- **Security Note**: Returns plain text password (before encryption)

#### setPassword()
```java
public void setPassword(String password) {
    this.password = password;
}
```
- **Parameters**: `String password` - password to set
- **Access Level**: Public

## Usage in Authentication Flow

### 1. Controller Level
```java
@PostMapping("/signin")
public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
    // loginRequest is automatically deserialized from JSON
    // Validation occurs before method execution
}
```

### 2. Service Level
```java
// In AuthService
public AuthenticationResult login(LoginRequest loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getUsername(),
            loginRequest.getPassword()
        )
    );
    // ... rest of authentication logic
}
```

## JSON Request Format

### Valid Request Example:
```json
{
    "username": "user1",
    "password": "password123"
}
```

### Invalid Request Examples:

#### Missing Username:
```json
{
    "password": "password123"
}
```
**Error**: Validation failure - username is required

#### Empty Password:
```json
{
    "username": "user1",
    "password": ""
}
```
**Error**: Validation failure - password cannot be blank

#### Whitespace Only:
```json
{
    "username": "   ",
    "password": "password123"
}
```
**Error**: Validation failure - username cannot be blank

## Validation Details

### @NotBlank Constraint:
- **Null Check**: `username != null`
- **Empty Check**: `!username.isEmpty()`
- **Whitespace Check**: `!username.trim().isEmpty()`

### Validation Error Response:
When validation fails, Spring returns:
```json
{
    "timestamp": "2024-01-01T12:00:00.000+00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "errors": [
        {
            "field": "username",
            "defaultMessage": "must not be blank"
        }
    ]
}
```

## Security Considerations

### Current Implementation:
✅ Password field is private
✅ Validation prevents empty credentials
✅ No password exposed in toString (if implemented)

### Recommendations:

1. **Add Password Constraints**:
```java
@NotBlank
@Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
private String password;
```

2. **Add Username Constraints**:
```java
@NotBlank
@Size(min = 3, max = 50)
@Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "Username can only contain letters, numbers, and underscores")
private String username;
```

3. **Implement Lombok**:
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    // ... fields
}
```

4. **Add Custom Validation**:
```java
@ValidCredentials // Custom validator
public class LoginRequest {
    // ... implementation
}
```

## Testing

### Unit Test Example:
```java
@Test
public void testLoginRequestValidation() {
    // Valid request
    LoginRequest validRequest = new LoginRequest();
    validRequest.setUsername("testuser");
    validRequest.setPassword("password123");
    
    Set<ConstraintViolation<LoginRequest>> violations = 
        validator.validate(validRequest);
    assertTrue(violations.isEmpty());
    
    // Invalid request - blank username
    LoginRequest invalidRequest = new LoginRequest();
    invalidRequest.setUsername("");
    invalidRequest.setPassword("password123");
    
    violations = validator.validate(invalidRequest);
    assertFalse(violations.isEmpty());
    assertEquals(1, violations.size());
}
```

### Integration Test:
```java
@Test
public void testLoginEndpointValidation() throws Exception {
    String invalidJson = "{\"username\":\"\",\"password\":\"test\"}";
    
    mockMvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").exists());
}
```

## Best Practices

### 1. Immutability Option:
```java
public final class LoginRequest {
    private final String username;
    private final String password;
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    // Only getters, no setters
}
```

### 2. Builder Pattern:
```java
@Builder
public class LoginRequest {
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;
}
```

### 3. Record Class (Java 14+):
```java
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
```

## Common Issues

### Issue 1: Case Sensitivity
**Problem**: Username "User1" vs "user1"
**Solution**: Consider case-insensitive authentication or document requirements

### Issue 2: Trimming Whitespace
**Problem**: Users accidentally add spaces
**Solution**: 
```java
public void setUsername(String username) {
    this.username = username != null ? username.trim() : null;
}
```

### Issue 3: Special Characters
**Problem**: Some systems don't support special characters in usernames
**Solution**: Add regex validation pattern

## Logging and Monitoring

### Security Logging:
```java
// Don't log passwords!
logger.info("Login attempt for username: {}", loginRequest.getUsername());
// Never: logger.info("Login request: {}", loginRequest);
```

### Metrics to Track:
- Failed login attempts per username
- Login request rate
- Validation failure rate

## Related Components
- **AuthController**: Receives LoginRequest
- **AuthService**: Processes authentication
- **SignupRequest**: Similar DTO for registration
- **UserDetailsImpl**: Result of successful authentication 