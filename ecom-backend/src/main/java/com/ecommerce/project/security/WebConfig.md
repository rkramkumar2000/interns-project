# WebConfig.java Documentation

## Overview
`WebConfig.java` is responsible for configuring Cross-Origin Resource Sharing (CORS) settings for the application, allowing the frontend application to communicate with the backend API.

## Class Declaration
```java
@Configuration
public class WebConfig implements WebMvcConfigurer
```

### Annotations:
- **@Configuration**: Marks this as a Spring configuration class

### Interface:
- **WebMvcConfigurer**: Provides callback methods to customize the Java-based configuration for Spring MVC

## Properties

```java
@Value("${frontend.url}")
String frontEndUrl;
```

- **@Value**: Injects the frontend URL from application properties
- **frontEndUrl**: The configured frontend application URL (e.g., production frontend URL)
- **Property Key**: `frontend.url` in application.properties

## Method: addCorsMappings

```java
@Override
public void addCorsMappings(CorsRegistry registry)
```

**Purpose**: Configures CORS mappings for the entire application

**Method Signature**:
- **Override**: Implements the WebMvcConfigurer interface method
- **Parameters**: 
  - `CorsRegistry registry`: Registry for CORS configuration

**Configuration Details**:

### 1. Mapping Pattern
```java
registry.addMapping("/**")
```
- **Pattern**: `/**` applies CORS configuration to all endpoints
- **Scope**: Every API endpoint in the application

### 2. Allowed Origins
```java
.allowedOrigins("http://localhost:3000", frontEndUrl)
```
- **localhost:3000**: Development frontend (typically React/Vue/Angular dev server)
- **frontEndUrl**: Production frontend URL from configuration
- **Purpose**: Specifies which domains can access the API

### 3. Allowed HTTP Methods
```java
.allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
```
- **GET**: Read operations
- **POST**: Create operations
- **PUT**: Update operations
- **DELETE**: Delete operations
- **OPTIONS**: Preflight requests

### 4. Allowed Headers
```java
.allowedHeaders("*")
```
- **Pattern**: `*` allows all headers
- **Common Headers**: Content-Type, Authorization, Accept, etc.
- **Purpose**: Permits frontend to send any required headers

### 5. Credentials Support
```java
.allowCredentials(true)
```
- **true**: Allows cookies and authorization headers
- **Important**: Required for JWT token transmission via cookies
- **Security**: Only works with specific origins (not with `*`)

## Complete Configuration Flow

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000", frontEndUrl)
            .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
}
```

## How CORS Works in This Configuration

1. **Preflight Requests**: Browser sends OPTIONS request before actual request
2. **Origin Check**: Server verifies if origin is in allowed list
3. **Method Validation**: Checks if HTTP method is permitted
4. **Header Validation**: Ensures headers are allowed
5. **Credentials**: Allows cookies/auth headers if origin matches

## Configuration Examples

### Development Configuration
In `application.properties`:
```properties
frontend.url=http://localhost:3000
```

### Production Configuration
In `application-prod.properties`:
```properties
frontend.url=https://yourdomain.com
```

## Security Considerations

### Current Implementation:
1. **Specific Origins**: Only allows defined origins (good practice)
2. **Credentials Enabled**: Necessary for cookie-based JWT
3. **All Headers Allowed**: Could be more restrictive

### Recommendations:
1. **Restrict Headers**: Instead of `*`, specify exact headers needed:
   ```java
   .allowedHeaders("Content-Type", "Authorization", "Accept")
   ```

2. **Environment-Specific**: Use profiles for different environments:
   ```java
   @Value("${cors.allowed-origins}")
   private List<String> allowedOrigins;
   ```

3. **Method Restriction**: Consider limiting methods per endpoint

## Common CORS Issues and Solutions

### Issue 1: Credentials with Wildcard
**Problem**: Cannot use `allowCredentials(true)` with `allowedOrigins("*")`
**Solution**: Specify exact origins as done in this configuration

### Issue 2: Missing Headers
**Problem**: Frontend receives CORS error for custom headers
**Solution**: Add specific headers to allowedHeaders or use `*`

### Issue 3: Preflight Failures
**Problem**: OPTIONS requests fail
**Solution**: Ensure OPTIONS is in allowedMethods

## Integration with Spring Security

This CORS configuration works in conjunction with Spring Security:
- WebSecurityConfig has `.cors(cors -> {})` which uses this configuration
- OPTIONS requests are permitted in security config: `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()`

## Testing CORS Configuration

### Browser DevTools
Check Network tab for:
- `Access-Control-Allow-Origin` header
- `Access-Control-Allow-Credentials` header
- `Access-Control-Allow-Methods` header

### CURL Command
```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -H "Access-Control-Request-Headers: X-Requested-With" \
     -X OPTIONS \
     http://localhost:8080/api/products
```

## Best Practices

1. **Never use wildcard origins** with credentials
2. **Be specific with allowed methods** - only what's needed
3. **Use environment variables** for origin configuration
4. **Consider different CORS policies** for different endpoints
5. **Log CORS requests** in development for debugging

## Related Components
- **WebSecurityConfig**: Main security configuration that enables CORS
- **AuthTokenFilter**: Processes requests after CORS validation
- **Frontend Application**: Must include credentials in requests 