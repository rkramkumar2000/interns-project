# UserInfoResponse.java Documentation

## Overview
`UserInfoResponse.java` is a comprehensive response DTO that encapsulates authenticated user information along with their JWT token. It's primarily used after successful login and for returning current user details.

## Class Declaration
```java
public class UserInfoResponse
```

### Class Type:
- Plain Java class (POJO)
- Response DTO for authentication endpoints
- Contains both user data and authentication token

## Fields

### 1. ID Field
```java
private Long id;
```
- **Type**: Long
- **Purpose**: User's unique identifier from database
- **Usage**: Client-side user identification

### 2. JWT Token Field
```java
private String jwtToken;
```
- **Type**: String
- **Purpose**: Authentication token for subsequent requests
- **Format**: JWT token string (can be used in cookie or header)

### 3. Username Field
```java
private String username;
```
- **Type**: String
- **Purpose**: User's login name
- **Display**: Often shown in UI for user identification

### 4. Email Field
```java
private String email;
```
- **Type**: String
- **Purpose**: User's email address
- **Usage**: Contact info and account verification

### 5. Roles Field
```java
private List<String> roles;
```
- **Type**: List<String>
- **Purpose**: User's authorities/permissions
- **Format**: List of role names (e.g., ["ROLE_USER", "ROLE_ADMIN"])

## Constructors

### Full Constructor
```java
public UserInfoResponse(Long id, String username, List<String> roles, 
                       String email, String jwtToken)
```

**Purpose**: Creates complete response with all user information
**Parameters**:
- `Long id`: User ID
- `String username`: Username
- `List<String> roles`: User roles
- `String email`: Email address
- `String jwtToken`: JWT token

**Usage**: After successful login

### Partial Constructor
```java
public UserInfoResponse(Long id, String username, List<String> roles)
```

**Purpose**: Creates response without email and token
**Parameters**:
- `Long id`: User ID
- `String username`: Username
- `List<String> roles`: User roles

**Usage**: For endpoints that return user info without token

## Methods

### Getters and Setters

All fields have standard getter and setter methods:

- `getId()` / `setId(Long id)`
- `getJwtToken()` / `setJwtToken(String jwtToken)`
- `getUsername()` / `setUsername(String username)`
- `getEmail()` / `setEmail(String email)`
- `getRoles()` / `setRoles(List<String> roles)`

## Usage in Authentication Flow

### 1. Login Response
```java
// In AuthService.login()
List<String> roles = userDetails.getAuthorities().stream()
    .map(item -> item.getAuthority())
    .collect(Collectors.toList());

UserInfoResponse response = new UserInfoResponse(
    userDetails.getId(),
    userDetails.getUsername(), 
    roles,
    userDetails.getEmail(),
    jwtCookie.toString()  // JWT token as string
);
```

### 2. Current User Endpoint
```java
// In AuthService.getCurrentUserDetails()
UserInfoResponse response = new UserInfoResponse(
    userDetails.getId(),
    userDetails.getUsername(), 
    roles
);
```

## JSON Response Examples

### Full Login Response:
```json
{
    "id": 123,
    "username": "john_doe",
    "email": "john@example.com",
    "roles": ["ROLE_USER", "ROLE_SELLER"],
    "jwtToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTY0MjA5..."
}
```

### Partial User Info Response:
```json
{
    "id": 123,
    "username": "john_doe",
    "roles": ["ROLE_USER", "ROLE_SELLER"],
    "email": null,
    "jwtToken": null
}
```

## Client-Side Usage

### JavaScript/React Example:
```javascript
// Login handling
const login = async (credentials) => {
    const response = await fetch('/api/auth/signin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials)
    });
    
    const userInfo = await response.json();
    
    // Store token
    localStorage.setItem('token', userInfo.jwtToken);
    
    // Store user info
    localStorage.setItem('user', JSON.stringify({
        id: userInfo.id,
        username: userInfo.username,
        email: userInfo.email,
        roles: userInfo.roles
    }));
    
    // Check roles
    const isAdmin = userInfo.roles.includes('ROLE_ADMIN');
    const isSeller = userInfo.roles.includes('ROLE_SELLER');
};
```

### Angular Example:
```typescript
interface UserInfoResponse {
    id: number;
    username: string;
    email: string;
    roles: string[];
    jwtToken: string;
}

login(credentials: LoginRequest): Observable<UserInfoResponse> {
    return this.http.post<UserInfoResponse>('/api/auth/signin', credentials)
        .pipe(
            tap(response => {
                // Store token and user info
                this.tokenService.saveToken(response.jwtToken);
                this.userService.saveUser(response);
            })
        );
}
```

## Security Considerations

### Current Implementation:
✅ Returns roles for client-side authorization
✅ Includes all necessary user identification
✅ JWT token included for immediate use

### Potential Issues:

1. **Token in Response Body**:
   - Token exposed in response logs
   - Consider using HTTP-only cookies instead

2. **No Token Expiration Info**:
   - Client doesn't know when token expires
   - Could add `expiresIn` field

3. **Role Names Exposed**:
   - Full role names visible to client
   - Consider using role codes or permissions

## Improvements and Best Practices

### 1. Add Token Metadata:
```java
public class UserInfoResponse {
    // ... existing fields ...
    private Long expiresIn;
    private String tokenType = "Bearer";
}
```

### 2. Use Lombok:
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private String jwtToken;
}
```

### 3. Add Permissions:
```java
public class UserInfoResponse {
    // ... existing fields ...
    private Set<String> permissions; // Granular permissions
}
```

### 4. Add User Profile Info:
```java
public class UserInfoResponse {
    // ... existing fields ...
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private LocalDateTime lastLogin;
}
```

### 5. Separate Token Response:
```java
// Better separation of concerns
public class AuthResponse {
    private UserInfoResponse user;
    private TokenResponse token;
}

public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
```

## Testing

### Unit Test Example:
```java
@Test
public void testUserInfoResponseCreation() {
    // Arrange
    Long id = 1L;
    String username = "testuser";
    List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
    String email = "test@example.com";
    String token = "test-jwt-token";
    
    // Act
    UserInfoResponse response = new UserInfoResponse(
        id, username, roles, email, token
    );
    
    // Assert
    assertEquals(id, response.getId());
    assertEquals(username, response.getUsername());
    assertEquals(email, response.getEmail());
    assertEquals(token, response.getJwtToken());
    assertEquals(2, response.getRoles().size());
    assertTrue(response.getRoles().contains("ROLE_USER"));
}
```

### Integration Test:
```java
@Test
public void testLoginResponseStructure() throws Exception {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("testuser");
    loginRequest.setPassword("password");
    
    mockMvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").exists())
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.jwtToken").exists());
}
```

## Common Patterns

### 1. Role Checking Utility:
```java
public class UserInfoResponse {
    // ... existing code ...
    
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
    
    public boolean isSeller() {
        return hasRole("ROLE_SELLER");
    }
}
```

### 2. JSON Ignore Null:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoResponse {
    // Fields with null values won't be included in JSON
}
```

### 3. Validation:
```java
public UserInfoResponse {
    // ... existing fields ...
    
    public void validate() {
        if (id == null || username == null || roles == null) {
            throw new IllegalStateException("Invalid user info response");
        }
    }
}
```

## Related Components
- **AuthController**: Returns UserInfoResponse on login
- **AuthService**: Creates UserInfoResponse instances
- **UserDetailsImpl**: Source of user data
- **JwtUtils**: Generates the JWT token
- **MessageResponse**: Simpler response for basic messages 