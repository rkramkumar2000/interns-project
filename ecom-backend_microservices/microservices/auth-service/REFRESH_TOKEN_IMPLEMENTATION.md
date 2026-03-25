# Refresh Token Implementation Guide

## Overview

This document describes the complete refresh token implementation in the Auth Service microservice. The implementation provides secure token refresh capabilities with token rotation and persistence.

## Key Features

1. **Dual Token System**
   - Short-lived access tokens (15 minutes)
   - Long-lived refresh tokens (7 days)
   - Token rotation on refresh for enhanced security

2. **Database Persistence**
   - Refresh tokens stored in PostgreSQL
   - Automatic cleanup of expired tokens
   - One refresh token per user at a time

3. **Security Features**
   - UUID-based refresh tokens (not JWT)
   - Token rotation prevents replay attacks
   - Automatic token cleanup via scheduled task
   - Tokens deleted on logout

## API Endpoints

### 1. Login - `/api/auth/signin`
**Method**: POST  
**Request Body**:
```json
{
  "username": "user@example.com",
  "password": "password123"
}
```
**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "type": "Bearer",
  "id": 1,
  "username": "user@example.com",
  "email": "user@example.com",
  "roles": ["ROLE_USER"]
}
```

### 2. Refresh Token - `/api/auth/refresh`
**Method**: POST  
**Request Parameter**: `refreshToken` (query parameter)  
**Response**: Same as login response with new tokens

### 3. Logout - `/api/auth/signout`
**Method**: POST  
**Headers**: `Authorization: Bearer {accessToken}`  
**Response**:
```json
{
  "message": "User logged out successfully!"
}
```

## Implementation Details

### 1. Database Schema

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
```

### 2. Configuration

In `auth-service.yml`:
```yaml
jwt:
  secret: your-secret-key-here
  expiration-ms: 900000        # 15 minutes for access token
  refresh-expiration-ms: 604800000  # 7 days for refresh token
```

### 3. Token Rotation

When a refresh token is used:
1. Validates the existing refresh token
2. Generates new access token
3. Generates new refresh token (deletes old one)
4. Returns both new tokens

### 4. Scheduled Cleanup

Expired tokens are automatically cleaned up daily at 2 AM:
```java
@Scheduled(cron = "0 0 2 * * ?")
public void cleanupExpiredTokens()
```

## Security Considerations

1. **Token Storage**
   - Never store refresh tokens in browser localStorage
   - Use secure HTTP-only cookies or secure client storage
   - Implement proper CORS configuration

2. **Token Validation**
   - Always validate token existence in database
   - Check token expiration
   - Verify user still exists and is active

3. **Rate Limiting**
   - Implement rate limiting on refresh endpoint
   - Monitor for suspicious refresh patterns

## Frontend Integration

### Storing Tokens
```javascript
// After login
localStorage.setItem('accessToken', response.accessToken);
// Store refresh token securely (consider HTTP-only cookies)
secureStorage.setItem('refreshToken', response.refreshToken);
```

### Using Refresh Token
```javascript
async function refreshAccessToken() {
  const refreshToken = secureStorage.getItem('refreshToken');
  const response = await fetch('/api/auth/refresh?refreshToken=' + refreshToken, {
    method: 'POST'
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('accessToken', data.accessToken);
    secureStorage.setItem('refreshToken', data.refreshToken);
    return data.accessToken;
  }
  
  // Redirect to login if refresh fails
  window.location.href = '/login';
}
```

### Axios Interceptor Example
```javascript
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const newAccessToken = await refreshAccessToken();
      originalRequest.headers['Authorization'] = 'Bearer ' + newAccessToken;
      return axios(originalRequest);
    }
    
    return Promise.reject(error);
  }
);
```

## Migration from Previous Implementation

1. **Database Migration**
   - Run the V2__create_refresh_tokens_table.sql migration
   - No data migration needed (new functionality)

2. **API Changes**
   - Login response now includes both `accessToken` and `refreshToken`
   - Previous `token` field still available for backward compatibility
   - Refresh endpoint now properly validates tokens against database

3. **Configuration Updates**
   - Add `jwt.expiration-ms` and `jwt.refresh-expiration-ms` properties
   - Reduce access token expiration time (recommended: 15 minutes)

## Troubleshooting

### Common Issues

1. **"Refresh token not found or invalid"**
   - Token doesn't exist in database
   - Token may have been used already (rotation)
   - Check database for token existence

2. **"Refresh token has expired"**
   - Token exceeded 7-day lifetime
   - User needs to login again
   - Check token expiry_date in database

3. **Multiple login sessions**
   - Only one refresh token per user
   - New login invalidates previous refresh token
   - Consider implementing device-based tokens for multiple sessions

## Testing

### Unit Tests
```java
@Test
void testRefreshTokenCreation() {
    RefreshToken token = refreshTokenService.createRefreshToken(userId);
    assertNotNull(token);
    assertNotNull(token.getToken());
    assertTrue(token.getExpiryDate().isAfter(Instant.now()));
}

@Test
void testTokenRotation() {
    String oldToken = createAndGetRefreshToken();
    JwtResponse response = authService.refreshToken(oldToken);
    assertNotEquals(oldToken, response.getRefreshToken());
    assertFalse(refreshTokenRepository.existsByToken(oldToken));
}
```

### Integration Tests
- Test login returns both tokens
- Test refresh endpoint with valid token
- Test refresh endpoint with expired token
- Test refresh endpoint with invalid token
- Test logout deletes refresh token

## Monitoring

1. **Metrics to Track**
   - Refresh token usage rate
   - Failed refresh attempts
   - Token expiration patterns
   - Cleanup job execution

2. **Logging**
   - All token operations are logged
   - Monitor for unusual patterns
   - Track failed authentication attempts

## Future Enhancements

1. **Device Management**
   - Support multiple refresh tokens per user
   - Track device information
   - Allow users to revoke specific devices

2. **Enhanced Security**
   - Implement refresh token families
   - Add IP address validation
   - Implement anomaly detection

3. **Performance**
   - Redis caching for token validation
   - Batch token cleanup operations
   - Optimize database queries 