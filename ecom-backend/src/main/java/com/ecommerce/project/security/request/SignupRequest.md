# SignupRequest.java Documentation

## Overview
`SignupRequest.java` is a Data Transfer Object (DTO) that encapsulates user registration data. It uses Lombok annotations for boilerplate code generation and includes comprehensive validation constraints.

## Class Declaration
```java
@Data
public class SignupRequest
```

### Annotations:
- **@Data**: Lombok annotation that generates:
  - All getters and setters
  - `toString()` method
  - `equals()` and `hashCode()` methods
  - Constructor for required (final) fields

## Fields

### 1. Username Field
```java
@NotBlank
@Size(min = 3, max = 20)
private String username;
```

**Annotations**:
- **@NotBlank**: Cannot be null, empty, or whitespace
- **@Size**: Length must be between 3 and 20 characters

**Constraints**:
- Minimum length: 3 characters
- Maximum length: 20 characters
- No whitespace-only values

### 2. Email Field
```java
@NotBlank
@Size(max = 50)
@Email
private String email;
```

**Annotations**:
- **@NotBlank**: Required field
- **@Size**: Maximum 50 characters
- **@Email**: Must be valid email format

**Email Validation**:
- Format: `user@domain.com`
- Validates against RFC 5322 standard

### 3. Role Field
```java
private Set<String> role;
```

**Characteristics**:
- Optional field (no validation annotations)
- Set prevents duplicate roles
- Expects role names as strings (e.g., "user", "admin", "seller")

### 4. Password Field
```java
@NotBlank
@Size(min = 6, max = 40)
private String password;
```

**Annotations**:
- **@NotBlank**: Required field
- **@Size**: Between 6 and 40 characters

**Security Notes**:
- Stored as plain text in DTO
- Encrypted before database storage
- Minimum 6 characters (should be increased)

## Methods

### Role Getter (Explicit)
```java
public Set<String> getRole() {
    return this.role;
}
```
- Explicitly defined despite @Data
- Returns the set of role strings

### Role Setter (Explicit)
```java
public void setRole(Set<String> role) {
    this.role = role;
}
```
- Explicitly defined despite @Data
- Sets the role collection

### Lombok-Generated Methods

Thanks to @Data annotation:
- `getUsername()`, `setUsername(String)`
- `getEmail()`, `setEmail(String)`
- `getPassword()`, `setPassword(String)`
- `toString()` - includes all fields
- `equals(Object)` - based on all fields
- `hashCode()` - based on all fields

## Usage in Registration Flow

### 1. Controller Level
```java
@PostMapping("/signup")
public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    return authService.register(signUpRequest);
}
```

### 2. Service Level Processing
```java
// In AuthService
public ResponseEntity<MessageResponse> register(SignupRequest signUpRequest) {
    // Check username availability
    if (userRepository.existsByUserName(signUpRequest.getUsername())) {
        return ResponseEntity.badRequest()
            .body(new MessageResponse("Error: Username is already taken!"));
    }
    
    // Create new user
    User user = new User(
        signUpRequest.getUsername(),
        signUpRequest.getEmail(),
        encoder.encode(signUpRequest.getPassword())
    );
    
    // Process roles
    Set<String> strRoles = signUpRequest.getRole();
    // ... role assignment logic
}
```

## JSON Request Examples

### Basic User Registration:
```json
{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "secure123"
}
```

### Registration with Roles:
```json
{
    "username": "seller123",
    "email": "seller@example.com",
    "password": "secure123",
    "role": ["seller"]
}
```

### Multiple Roles:
```json
{
    "username": "admin_user",
    "email": "admin@example.com",
    "password": "adminPass123",
    "role": ["admin", "seller", "user"]
}
```

## Role Processing Logic

From AuthServiceImpl:
```java
if (strRoles == null) {
    // Default to ROLE_USER
    Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
    roles.add(userRole);
} else {
    strRoles.forEach(role -> {
        switch (role) {
            case "admin":
                // Assign ROLE_ADMIN
                break;
            case "seller":
                // Assign ROLE_SELLER
                break;
            default:
                // Assign ROLE_USER
        }
    });
}
```

## Validation Error Responses

### Invalid Username Length:
```json
{
    "timestamp": "2024-01-01T12:00:00.000+00:00",
    "status": 400,
    "error": "Bad Request",
    "errors": [{
        "field": "username",
        "defaultMessage": "size must be between 3 and 20"
    }]
}
```

### Invalid Email Format:
```json
{
    "errors": [{
        "field": "email",
        "defaultMessage": "must be a well-formed email address"
    }]
}
```

### Multiple Validation Errors:
```json
{
    "errors": [
        {
            "field": "username",
            "defaultMessage": "must not be blank"
        },
        {
            "field": "password",
            "defaultMessage": "size must be between 6 and 40"
        }
    ]
}
```

## Security Considerations

### Current Implementation:
✅ Password length validation
✅ Email format validation
✅ Username length limits
✅ Plain text password in DTO (encrypted in service)

### Recommendations:

1. **Stronger Password Requirements**:
```java
@NotBlank
@Size(min = 8, max = 100)
@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
         message = "Password must contain at least one digit, lowercase, uppercase, and special character")
private String password;
```

2. **Username Pattern Validation**:
```java
@Pattern(regexp = "^[a-zA-Z0-9_-]*$", 
         message = "Username can only contain letters, numbers, underscores, and hyphens")
private String username;
```

3. **Role Validation**:
```java
@ValidRoles // Custom validator
private Set<String> role;
```

4. **Password Confirmation**:
```java
@NotBlank
private String confirmPassword;

@AssertTrue(message = "Passwords must match")
private boolean isPasswordsMatch() {
    return password != null && password.equals(confirmPassword);
}
```

## Testing

### Unit Test Example:
```java
@Test
public void testSignupValidation() {
    SignupRequest request = new SignupRequest();
    request.setUsername("ab"); // Too short
    request.setEmail("invalid-email");
    request.setPassword("123"); // Too short
    
    Set<ConstraintViolation<SignupRequest>> violations = 
        validator.validate(request);
    
    assertEquals(3, violations.size());
}

@Test
public void testValidSignupRequest() {
    SignupRequest request = new SignupRequest();
    request.setUsername("validuser");
    request.setEmail("valid@email.com");
    request.setPassword("validPass123");
    request.setRole(Set.of("user"));
    
    Set<ConstraintViolation<SignupRequest>> violations = 
        validator.validate(request);
    
    assertTrue(violations.isEmpty());
}
```

## Common Issues and Solutions

### Issue 1: Duplicate Username/Email
**Problem**: Registration fails with "already exists"
**Solution**: Add unique validation before processing:
```java
@UniqueUsername // Custom validator
private String username;

@UniqueEmail // Custom validator
private String email;
```

### Issue 2: Invalid Role Names
**Problem**: Typos in role names (e.g., "admn" instead of "admin")
**Solution**: Validate against enum values:
```java
private boolean isValidRole(String role) {
    return Arrays.asList("user", "admin", "seller").contains(role.toLowerCase());
}
```

### Issue 3: ToString Security
**Problem**: @Data generates toString with password
**Solution**: Override toString:
```java
@Override
public String toString() {
    return "SignupRequest{" +
            "username='" + username + '\'' +
            ", email='" + email + '\'' +
            ", role=" + role +
            ", password='[PROTECTED]'" +
            '}';
}
```

## Best Practices

### 1. Use DTOs for Validation Groups:
```java
public interface OnCreate {}

@NotBlank(groups = OnCreate.class)
@Size(min = 3, max = 20, groups = OnCreate.class)
private String username;
```

### 2. Add Audit Fields:
```java
private String ipAddress;
private String userAgent;
private LocalDateTime registrationTime;
```

### 3. Implement Rate Limiting:
Track registration attempts per IP to prevent spam

## Related Components
- **AuthController**: Receives SignupRequest
- **AuthService**: Processes registration
- **User**: Entity created from SignupRequest
- **Role**: Assigned based on role field
- **LoginRequest**: Similar DTO for authentication 