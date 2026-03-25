# AuthTokenFilter.java Documentation

## Overview
`AuthTokenFilter.java` is a critical security component that extends Spring's `OncePerRequestFilter` to intercept every HTTP request and validate JWT tokens. It serves as the primary authentication filter in the security chain.

## Class Declaration
```java
@Component
public class AuthTokenFilter extends OncePerRequestFilter
```

### Annotations:
- **@Component**: Registers this filter as a Spring bean

### Parent Class:
- **OncePerRequestFilter**: Ensures filter executes exactly once per request

## Dependencies

```java
@Autowired
private JwtUtils jwtUtils;

@Autowired
private UserDetailsServiceImpl userDetailsService;

private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
```

- **jwtUtils**: Handles JWT parsing and validation
- **userDetailsService**: Loads user details from database
- **logger**: For debugging and error logging

## Core Method: doFilterInternal

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain)
        throws ServletException, IOException
```

### Method Signature:
- **@Override**: Implements abstract method from OncePerRequestFilter
- **Protected**: Can be overridden by subclasses
- **Parameters**:
  - `HttpServletRequest request`: Incoming HTTP request
  - `HttpServletResponse response`: HTTP response object
  - `FilterChain filterChain`: Chain of filters to execute

### Method Flow:

#### 1. Debug Logging
```java
logger.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());
```
- Logs the requested URI for debugging
- Helps track which endpoints are being accessed

#### 2. JWT Processing Block
```java
try {
    String jwt = parseJwt(request);
    if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
        // Authentication logic
    }
} catch (Exception e) {
    logger.error("Cannot set user authentication: {}", e);
}
```

**Process**:
1. Extract JWT from request
2. Validate token if found
3. Handle any exceptions gracefully

#### 3. Authentication Setup
```java
if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
    String username = jwtUtils.getUserNameFromJwtToken(jwt);
    
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails,
                    null,
                    userDetails.getAuthorities());
    logger.debug("Roles from JWT: {}", userDetails.getAuthorities());
    
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

**Step-by-Step Process**:

1. **Extract Username**: Get username from validated JWT
2. **Load User Details**: Fetch complete user info from database
3. **Create Authentication Token**:
   - Principal: userDetails
   - Credentials: null (already authenticated via JWT)
   - Authorities: User's roles/permissions
4. **Set Request Details**: Attach request metadata
5. **Update Security Context**: Make authentication available to Spring Security

#### 4. Continue Filter Chain
```java
filterChain.doFilter(request, response);
```
- Passes request to next filter in chain
- Executes regardless of authentication success/failure

## Helper Method: parseJwt

```java
private String parseJwt(HttpServletRequest request)
```

### Current Implementation:
```java
private String parseJwt(HttpServletRequest request) {
    String jwtFromCookie = jwtUtils.getJwtFromCookies(request);
    if (jwtFromCookie != null) {
        return jwtFromCookie;
    }
    
    String jwtFromHeader = jwtUtils.getJwtFromHeader(request);
    if (jwtFromHeader != null) {
        return jwtFromHeader;
    }
    
    return null;
}
```

### Token Extraction Priority:
1. **First**: Check cookies for JWT
2. **Second**: Check Authorization header
3. **Fallback**: Return null if not found

### Commented Legacy Code:
```java
//    private String parseJwt(HttpServletRequest request) {
//        String jwt = jwtUtils.getJwtFromCookies(request);
//        logger.debug("AuthTokenFilter.java: {}", jwt);
//        return jwt;
//    }
```
- Previous version only checked cookies
- Current version supports both cookie and header authentication

## Security Flow Diagram

```
Request → AuthTokenFilter → Parse JWT → Validate Token
                                ↓
                         Token Invalid → Continue (Unauthenticated)
                                ↓
                         Token Valid → Load User → Set Authentication → Continue
```

## Exception Handling

```java
} catch (Exception e) {
    logger.error("Cannot set user authentication: {}", e);
}
```

**Handled Scenarios**:
- Invalid JWT format
- User not found in database
- Database connection issues
- Any unexpected errors

**Behavior**: Continues request processing without authentication

## Integration with Spring Security

### Filter Chain Position:
```java
// In WebSecurityConfig.java
http.addFilterBefore(authenticationJwtTokenFilter(), 
                     UsernamePasswordAuthenticationFilter.class);
```
- Executes before Spring's default authentication
- Ensures JWT authentication takes precedence

### Security Context Usage:
- Controllers can access authenticated user via `@AuthenticationPrincipal`
- Security annotations like `@PreAuthorize` work with set authentication

## Common Use Cases

### 1. API Request with JWT Cookie:
```http
GET /api/products
Cookie: ecom-jwt=eyJhbGciOiJIUzI1NiJ9...
```

### 2. API Request with Authorization Header:
```http
GET /api/products
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 3. Public Endpoint Access:
- Filter runs but doesn't require valid JWT
- Security config determines if authentication needed

## Performance Considerations

### Database Queries:
- Loads user from database on EVERY authenticated request
- No caching mechanism implemented

### Optimization Recommendations:
1. **Cache User Details**:
   ```java
   @Cacheable("users")
   public UserDetails loadUserByUsername(String username)
   ```

2. **Store More in JWT**:
   - Include roles in JWT claims
   - Reduce database lookups

3. **Conditional Loading**:
   - Only load full user for specific endpoints
   - Use JWT claims for basic operations

## Security Best Practices

### Current Implementation:
✅ Validates JWT signature and expiration
✅ Graceful error handling
✅ Supports multiple token sources
✅ Debug logging for troubleshooting

### Improvements Needed:
❌ No rate limiting
❌ No token blacklist checking
❌ Loads full user on every request
❌ No request correlation ID

## Testing the Filter

### Unit Test Example:
```java
@Test
public void testValidJwtAuthentication() {
    // Mock request with valid JWT
    when(request.getCookies()).thenReturn(new Cookie[]{
        new Cookie("ecom-jwt", validJwt)
    });
    
    // Execute filter
    authTokenFilter.doFilterInternal(request, response, filterChain);
    
    // Verify authentication set
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
}
```

### Integration Test:
```java
@Test
@WithMockUser
public void testProtectedEndpoint() {
    mockMvc.perform(get("/api/products")
            .cookie(new Cookie("ecom-jwt", validJwt)))
            .andExpect(status().isOk());
}
```

## Troubleshooting

### Common Issues:

1. **Authentication Not Set**:
   - Check JWT format
   - Verify token not expired
   - Ensure user exists in database

2. **403 Forbidden After Authentication**:
   - Check user roles match endpoint requirements
   - Verify authorities loaded correctly

3. **Performance Issues**:
   - Monitor database query time
   - Consider implementing caching

### Debug Tips:
- Enable debug logging: `logging.level.com.ecommerce.project.security=DEBUG`
- Check SecurityContext: `SecurityContextHolder.getContext().getAuthentication()`
- Monitor filter execution time

## Related Components
- **JwtUtils**: Token validation and parsing
- **UserDetailsServiceImpl**: User loading
- **WebSecurityConfig**: Filter registration
- **AuthEntryPointJwt**: Handles authentication failures 