# AuthEntryPointJwt.java Documentation

## Overview
`AuthEntryPointJwt.java` implements Spring Security's `AuthenticationEntryPoint` interface to handle authentication exceptions. It provides a custom response when users try to access protected resources without proper authentication.

## Class Declaration
```java
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint
```

### Annotations:
- **@Component**: Registers as a Spring bean for dependency injection

### Interface:
- **AuthenticationEntryPoint**: Spring Security interface for handling authentication exceptions

## Dependencies

```java
private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);
```

- **logger**: SLF4J logger for error logging

## Core Method: commence

```java
@Override
public void commence(HttpServletRequest request, 
                    HttpServletResponse response, 
                    AuthenticationException authException)
        throws IOException, ServletException
```

### Method Signature:
- **@Override**: Implements interface method
- **Parameters**:
  - `HttpServletRequest request`: The request that resulted in authentication failure
  - `HttpServletResponse response`: Response object to write error details
  - `AuthenticationException authException`: The exception that triggered this entry point

### Method Implementation:

#### 1. Error Logging
```java
logger.error("Unauthorized error: {}", authException.getMessage());
```
- Logs the authentication error with exception message
- Useful for debugging authentication failures

#### 2. Response Configuration
```java
response.setContentType(MediaType.APPLICATION_JSON_VALUE);
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
```

**Settings**:
- **Content-Type**: `application/json` - Returns JSON response
- **Status Code**: `401 Unauthorized` - Standard HTTP status for authentication failure

#### 3. Response Body Construction
```java
final Map<String, Object> body = new HashMap<>();
body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
body.put("error", "Unauthorized");
body.put("message", authException.getMessage());
body.put("path", request.getServletPath());
```

**Response Fields**:
- **status**: HTTP status code (401)
- **error**: Error type ("Unauthorized")
- **message**: Detailed error message from exception
- **path**: The requested path that triggered the error

#### 4. JSON Serialization
```java
final ObjectMapper mapper = new ObjectMapper();
mapper.writeValue(response.getOutputStream(), body);
```

- Uses Jackson ObjectMapper for JSON conversion
- Writes directly to response output stream

## Response Format

### Example Response:
```json
{
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource",
    "path": "/api/products"
}
```

## When This Component is Triggered

### 1. Missing Authentication:
- Request to protected endpoint without JWT token
- Example: `GET /api/admin/users` without token

### 2. Invalid Token:
- Expired JWT token
- Malformed token structure
- Invalid signature

### 3. Insufficient Permissions:
- Valid authentication but missing required role
- Though this typically returns 403 Forbidden instead

## Integration with Security Configuration

### In WebSecurityConfig:
```java
.exceptionHandling(exception -> 
    exception.authenticationEntryPoint(unauthorizedHandler))
```

This configuration ensures all authentication exceptions are handled by this component.

## Common Authentication Exception Messages

1. **"Full authentication is required to access this resource"**
   - No authentication token provided

2. **"Bad credentials"**
   - Invalid username/password during login

3. **"JWT token is expired"**
   - Token validation failed due to expiration

4. **"JWT token is invalid"**
   - Token signature verification failed

## Security Best Practices

### Current Implementation:
✅ Returns JSON response for API consistency
✅ Includes request path for debugging
✅ Uses standard HTTP 401 status code
✅ Logs errors for monitoring

### Potential Improvements:

1. **Sanitize Error Messages**:
   ```java
   body.put("message", sanitizeMessage(authException.getMessage()));
   ```
   - Avoid exposing sensitive information

2. **Add Timestamp**:
   ```java
   body.put("timestamp", new Date());
   ```

3. **Include Request ID**:
   ```java
   body.put("requestId", MDC.get("requestId"));
   ```
   - For request tracing

4. **Custom Error Codes**:
   ```java
   body.put("errorCode", "AUTH_001");
   ```
   - For client-side error handling

## Error Response Examples

### No Token Provided:
```json
{
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource",
    "path": "/api/products"
}
```

### Expired Token:
```json
{
    "status": 401,
    "error": "Unauthorized", 
    "message": "JWT token is expired",
    "path": "/api/orders"
}
```

### Invalid Token Format:
```json
{
    "status": 401,
    "error": "Unauthorized",
    "message": "JWT strings must contain exactly 2 period characters",
    "path": "/api/cart"
}
```

## Client-Side Handling

### JavaScript/Axios Example:
```javascript
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Redirect to login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### React Error Handling:
```javascript
const handleApiError = (error) => {
  if (error.response?.data?.status === 401) {
    // Clear stored token
    localStorage.removeItem('token');
    // Redirect to login
    navigate('/login');
  }
};
```

## Testing

### Unit Test Example:
```java
@Test
public void testUnauthorizedResponse() throws Exception {
    // Mock objects
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    when(request.getServletPath()).thenReturn("/api/test");
    when(response.getOutputStream()).thenReturn(
        new ServletOutputStream() {
            @Override
            public void write(int b) {
                outputStream.write(b);
            }
        }
    );
    
    // Execute
    AuthenticationException authEx = 
        new InsufficientAuthenticationException("Test error");
    entryPoint.commence(request, response, authEx);
    
    // Verify
    verify(response).setStatus(401);
    verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    
    // Check JSON response
    String jsonResponse = outputStream.toString();
    assertTrue(jsonResponse.contains("\"status\":401"));
    assertTrue(jsonResponse.contains("\"error\":\"Unauthorized\""));
}
```

## Common Issues and Solutions

### Issue 1: CORS Errors
**Problem**: Browser shows CORS error instead of 401
**Solution**: Ensure CORS is configured before security filters

### Issue 2: HTML Response Instead of JSON
**Problem**: Default Spring error page returned
**Solution**: Verify this component is properly configured in WebSecurityConfig

### Issue 3: Missing Error Details
**Problem**: Generic error messages
**Solution**: Enable debug logging for detailed exception information

## Monitoring and Alerting

### Metrics to Track:
1. **401 Response Rate**: High rate may indicate token issues
2. **Error Message Distribution**: Identify common failure patterns
3. **Path Analysis**: Which endpoints trigger most 401s

### Log Analysis:
```bash
grep "Unauthorized error" app.log | awk -F': ' '{print $2}' | sort | uniq -c
```

## Related Components
- **WebSecurityConfig**: Configures this as the authentication entry point
- **AuthTokenFilter**: May trigger this when token validation fails
- **JwtUtils**: Token validation that can lead to authentication exceptions 