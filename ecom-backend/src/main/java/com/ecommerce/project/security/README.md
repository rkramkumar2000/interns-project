# Spring Security Package Documentation

## Overview
This package contains the complete Spring Security implementation for the e-commerce application, providing JWT-based authentication, authorization, and security configuration.

## Package Structure

```
security/
├── jwt/                    # JWT token handling components
│   ├── AuthEntryPointJwt.java
│   ├── AuthTokenFilter.java
│   └── JwtUtils.java
├── request/               # Authentication request DTOs
│   ├── LoginRequest.java
│   └── SignupRequest.java
├── response/              # Authentication response DTOs
│   ├── MessageResponse.java
│   └── UserInfoResponse.java
├── services/              # User details service implementation
│   ├── UserDetailsImpl.java
│   └── UserDetailsServiceImpl.java
├── WebConfig.java         # CORS configuration
└── WebSecurityConfig.java # Main security configuration
```

## Core Components

### 1. Security Configuration
- **WebSecurityConfig.java**: Main Spring Security configuration defining authentication, authorization rules, and security filters
- **WebConfig.java**: CORS configuration for cross-origin requests

### 2. JWT Components
- **JwtUtils.java**: Utility class for JWT token generation, validation, and parsing
- **AuthTokenFilter.java**: Security filter that validates JWT tokens on each request
- **AuthEntryPointJwt.java**: Handles authentication exceptions and returns appropriate error responses

### 3. User Details Service
- **UserDetailsServiceImpl.java**: Loads user information from the database
- **UserDetailsImpl.java**: Implementation of Spring Security's UserDetails interface

### 4. Request/Response DTOs
- **LoginRequest.java**: DTO for login credentials
- **SignupRequest.java**: DTO for user registration
- **MessageResponse.java**: Generic message response
- **UserInfoResponse.java**: User information response after successful authentication

## Security Flow

1. **Authentication**: Users authenticate via `/api/auth/signin` endpoint
2. **Token Generation**: Successful authentication generates a JWT token
3. **Request Filtering**: All subsequent requests are filtered through `AuthTokenFilter`
4. **Authorization**: Access to endpoints is controlled based on user roles (USER, SELLER, ADMIN)

## Key Features

- JWT-based stateless authentication
- Role-based access control (RBAC)
- Support for both cookie and header-based token transmission
- BCrypt password encryption
- CORS configuration for frontend integration
- Automatic role initialization on startup

## Security Best Practices

- Passwords are encrypted using BCrypt
- JWT tokens have configurable expiration
- Unauthorized access returns proper HTTP 401 responses
- Flexible token extraction from cookies or Authorization header

## Configuration Properties

The following properties should be configured in `application.properties`:
```properties
spring.app.jwtSecret=yourSecretKey
spring.app.jwtExpirationMs=86400000
spring.ecom.app.jwtCookieName=ecom-jwt
frontend.url=http://localhost:3000
```

For detailed documentation of each component, see the individual file documentation in this directory. 