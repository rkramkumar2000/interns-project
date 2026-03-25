# JwtUtils.java Documentation

## Overview
`JwtUtils.java` is a utility class that handles all JWT (JSON Web Token) operations including token generation, validation, parsing, and cookie management. It serves as the central component for JWT-based authentication in the application.

## Class Declaration
```java
@Component
public class JwtUtils
```

### Annotations:
- **@Component**: Makes this class a Spring-managed bean, available for dependency injection

## Class Members

### Logger
```java
private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
```
- **Purpose**: Logs JWT-related operations and errors
- **Level**: Uses SLF4J for logging framework abstraction

### Configuration Properties
```java
@Value("${spring.app.jwtSecret}")
private String jwtSecret;

@Value("${spring.app.jwtExpirationMs}")
private int jwtExpirationMs;

@Value("${spring.ecom.app.jwtCookieName}")
private String jwtCookie;
```

- **jwtSecret**: Secret key for signing JWT tokens (Base64 encoded)
- **jwtExpirationMs**: Token expiration time in milliseconds
- **jwtCookie**: Name of the cookie storing JWT token

## Methods

### 1. getJwtFromCookies
```java
public String getJwtFromCookies(HttpServletRequest request)
```

**Purpose**: Extracts JWT token from HTTP cookies

**Parameters**:
- `HttpServletRequest request`: The incoming HTTP request

**Returns**: 
- JWT token string if cookie exists
- `null` if cookie not found

**Implementation**:
```java
Cookie cookie = WebUtils.getCookie(request, jwtCookie);
if (cookie != null) {
    return cookie.getValue();
} else {
    return null;
}
```

**Usage**: Called by AuthTokenFilter to retrieve JWT from cookie-based authentication

### 2. getJwtFromHeader
```java
public String getJwtFromHeader(HttpServletRequest request)
```

**Purpose**: Extracts JWT token from Authorization header

**Parameters**:
- `HttpServletRequest request`: The incoming HTTP request

**Returns**:
- JWT token string (without "Bearer " prefix)
- `null` if header not found or invalid format

**Implementation**:
```java
String bearerToken = request.getHeader("Authorization");
if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
    return bearerToken.substring(7);
}
return null;
```

**Header Format**: `Authorization: Bearer <token>`

### 3. generateJwtCookie
```java
public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal)
```

**Purpose**: Generates a JWT token and wraps it in an HTTP cookie

**Parameters**:
- `UserDetailsImpl userPrincipal`: The authenticated user details

**Returns**: `ResponseCookie` object containing JWT

**Cookie Configuration**:
```java
ResponseCookie cookie = ResponseCookie.from(jwtCookie, jwt)
        .path("/api")            // Cookie path
        .maxAge(24 * 60 * 60)    // 24 hours
        .httpOnly(false)         // Accessible via JavaScript
        .secure(false)           // Not HTTPS-only
        .build();
```

**Security Concerns**:
- `httpOnly(false)`: Makes cookie vulnerable to XSS attacks
- `secure(false)`: Vulnerable to man-in-the-middle attacks
- Should be `httpOnly(true)` and `secure(true)` in production

### 4. getCleanJwtCookie
```java
public ResponseCookie getCleanJwtCookie()
```

**Purpose**: Creates an empty cookie to clear JWT on logout

**Returns**: Empty `ResponseCookie` that overwrites existing JWT cookie

**Implementation**:
```java
ResponseCookie cookie = ResponseCookie.from(jwtCookie, null)
        .path("/api")
        .build();
```

**Usage**: Called during logout to remove JWT cookie from browser

### 5. generateTokenFromUsername
```java
public String generateTokenFromUsername(String username)
```

**Purpose**: Generates a JWT token for a given username

**Parameters**:
- `String username`: The username to embed in token

**Returns**: Signed JWT token string

**Token Structure**:
```java
return Jwts.builder()
        .subject(username)                              // Token subject
        .issuedAt(new Date())                          // Issue time
        .expiration(new Date((new Date()).getTime() + jwtExpirationMs))  // Expiry
        .signWith(key())                               // Digital signature
        .compact();
```

**JWT Claims**:
- **Subject**: Username
- **Issued At**: Current timestamp
- **Expiration**: Current time + configured expiration milliseconds
- **Signature**: HMAC with secret key

### 6. getUserNameFromJwtToken
```java
public String getUserNameFromJwtToken(String token)
```

**Purpose**: Extracts username from a JWT token

**Parameters**:
- `String token`: The JWT token to parse

**Returns**: Username (subject) from the token

**Implementation**:
```java
return Jwts.parser()
        .verifyWith((SecretKey) key())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
```

**Process**:
1. Creates parser with secret key
2. Verifies signature
3. Parses claims
4. Extracts subject (username)

### 7. key (Private Method)
```java
private Key key()
```

**Purpose**: Generates the secret key for JWT operations

**Returns**: HMAC secret key

**Implementation**:
```java
return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
```

**Security**: 
- Decodes Base64 encoded secret
- Creates HMAC-SHA key
- Used for both signing and verification

### 8. validateJwtToken
```java
public boolean validateJwtToken(String authToken)
```

**Purpose**: Validates JWT token integrity and expiration

**Parameters**:
- `String authToken`: Token to validate

**Returns**: 
- `true` if token is valid
- `false` if token is invalid, expired, or malformed

**Validation Checks**:

1. **Signature Verification**:
   ```java
   Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
   ```

2. **Exception Handling**:
   - **MalformedJwtException**: Invalid JWT structure
   - **ExpiredJwtException**: Token has expired
   - **UnsupportedJwtException**: Unsupported JWT type
   - **IllegalArgumentException**: Empty claims string

**Error Logging**:
```java
logger.error("Invalid JWT token: {}", e.getMessage());
logger.error("JWT token is expired: {}", e.getMessage());
logger.error("JWT token is unsupported: {}", e.getMessage());
logger.error("JWT claims string is empty: {}", e.getMessage());
```

## Security Best Practices

### Current Issues:
1. **Insecure Cookie Settings**: 
   - `httpOnly(false)` exposes to XSS
   - `secure(false)` allows HTTP transmission

2. **No Token Refresh**: 
   - Single token with fixed expiration
   - No refresh token mechanism

3. **No Token Revocation**: 
   - Cannot invalidate tokens before expiration

### Recommendations:

1. **Secure Cookie Configuration**:
   ```java
   .httpOnly(true)
   .secure(true)
   .sameSite(Cookie.SameSite.STRICT)
   ```

2. **Token Refresh Pattern**:
   - Short-lived access tokens (15 minutes)
   - Long-lived refresh tokens
   - Token rotation on refresh

3. **Token Blacklist**:
   - Store revoked tokens in Redis
   - Check blacklist during validation

4. **Additional Claims**:
   ```java
   .claim("roles", userRoles)
   .claim("userId", userId)
   ```

## Configuration Example

In `application.properties`:
```properties
spring.app.jwtSecret=mySecretKeyBase64Encoded
spring.app.jwtExpirationMs=86400000  # 24 hours
spring.ecom.app.jwtCookieName=ecom-jwt
```

## Integration Points

1. **AuthService**: Uses for token generation during login
2. **AuthTokenFilter**: Uses for token validation on each request
3. **AuthController**: Uses for logout (clean cookie)

## Testing JWT Operations

### Generate Token Test:
```java
String token = jwtUtils.generateTokenFromUsername("testuser");
assertNotNull(token);
```

### Validate Token Test:
```java
String token = jwtUtils.generateTokenFromUsername("testuser");
assertTrue(jwtUtils.validateJwtToken(token));
```

### Extract Username Test:
```java
String token = jwtUtils.generateTokenFromUsername("testuser");
assertEquals("testuser", jwtUtils.getUserNameFromJwtToken(token));
```

## Common Issues

1. **Invalid Signature**: Wrong secret key
2. **Token Expired**: Check expiration time
3. **Malformed Token**: Verify token format
4. **Base64 Decode Error**: Ensure secret is properly encoded 