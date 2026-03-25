# Spring Security Package - Documentation Index

This directory contains comprehensive documentation for all Spring Security components in the e-commerce application. Each file has been documented with detailed explanations of its purpose, implementation, and usage.

## 📚 Documentation Files

### Main Documentation
- **[README.md](README.md)** - Overview of the security package structure and components

### Configuration Documentation
1. **[WebSecurityConfig.md](WebSecurityConfig.md)** - Main Spring Security configuration
   - Security rules and filters
   - Authentication and authorization setup
   - Role-based access control
   - Bean definitions and initialization

2. **[WebConfig.md](WebConfig.md)** - CORS configuration
   - Cross-origin resource sharing setup
   - Allowed origins, methods, and headers

### JWT Components Documentation
3. **[jwt/JwtUtils.md](jwt/JwtUtils.md)** - JWT token utilities
   - Token generation and validation
   - Cookie management
   - Security considerations

4. **[jwt/AuthTokenFilter.md](jwt/AuthTokenFilter.md)** - JWT authentication filter
   - Request interception and validation
   - Authentication context setup
   - Performance considerations

5. **[jwt/AuthEntryPointJwt.md](jwt/AuthEntryPointJwt.md)** - Authentication error handler
   - 401 Unauthorized responses
   - Error response formatting

### Services Documentation
6. **[services/UserDetailsServiceImpl.md](services/UserDetailsServiceImpl.md)** - User loading service
   - Database user retrieval
   - Integration with Spring Security
   - Performance optimization suggestions

7. **[services/UserDetailsImpl.md](services/UserDetailsImpl.md)** - User details implementation
   - Spring Security UserDetails adapter
   - Authority management
   - User principal representation

### Request DTOs Documentation
8. **[request/LoginRequest.md](request/LoginRequest.md)** - Login credentials DTO
   - Validation constraints
   - Usage in authentication flow

9. **[request/SignupRequest.md](request/SignupRequest.md)** - Registration DTO
   - User registration validation
   - Role assignment logic

### Response DTOs Documentation
10. **[response/MessageResponse.md](response/MessageResponse.md)** - Generic message response
    - Success/error message handling
    - Client-side integration

11. **[response/UserInfoResponse.md](response/UserInfoResponse.md)** - User information response
    - Authentication response structure
    - JWT token delivery
    - Role information

## 🔍 Quick Reference

### Authentication Flow
1. User submits credentials via `LoginRequest`
2. `AuthController` receives request
3. `AuthService` validates via `AuthenticationManager`
4. `UserDetailsServiceImpl` loads user from database
5. `JwtUtils` generates JWT token
6. `UserInfoResponse` returns user data + token

### Request Processing Flow
1. `AuthTokenFilter` intercepts all requests
2. Extracts JWT from cookie/header
3. Validates token using `JwtUtils`
4. Loads user via `UserDetailsServiceImpl`
5. Sets authentication in `SecurityContext`
6. Request proceeds to controllers

### Error Handling
- Authentication failures handled by `AuthEntryPointJwt`
- Returns JSON error responses with 401 status
- Validation errors return 400 with field details

## 📋 Common Tasks

### Adding a New Role
1. Add to `AppRole` enum
2. Initialize in `WebSecurityConfig`
3. Update role assignment in `AuthService`
4. Add endpoint rules in security configuration

### Customizing JWT
1. Modify token generation in `JwtUtils`
2. Update cookie settings for security
3. Add custom claims if needed
4. Adjust expiration time in properties

### Implementing Security Features
- **Rate Limiting**: Add to `AuthTokenFilter`
- **Token Refresh**: Extend `JwtUtils` with refresh logic
- **Account Locking**: Implement in `UserDetailsImpl`
- **Audit Logging**: Add to service implementations

## 🚀 Best Practices

1. **Always** validate input in request DTOs
2. **Never** expose passwords in responses
3. **Use** HTTPS in production (update cookie settings)
4. **Implement** user caching for performance
5. **Monitor** authentication failures
6. **Update** dependencies regularly
7. **Test** security configurations thoroughly

## ⚠️ Security Warnings

Current implementation has several areas needing attention:
- JWT cookies are not httpOnly (XSS vulnerability)
- No refresh token mechanism
- No token revocation/blacklist
- User loaded from DB on every request
- Test users created on startup

See individual documentation files for specific recommendations and fixes. 