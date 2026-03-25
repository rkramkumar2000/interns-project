package com.ecommerce.auth.service;

import com.ecommerce.auth.exception.TokenRefreshException;
import com.ecommerce.auth.model.RefreshToken;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    
    @Value("${jwt.refresh-expiration-ms}")
    private int refreshTokenExpirationMs;
    
    public RefreshToken createRefreshToken(Long userId) {
        // Delete any existing refresh token for this user
        refreshTokenRepository.findByUserId(userId)
                .ifPresent(refreshTokenRepository::delete);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        log.info("Created new refresh token for user: {}", userId);
        
        return refreshToken;
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expired for user: {}", token.getUserId());
            throw new TokenRefreshException("Refresh token has expired. Please login again.");
        }
        return token;
    }
    
    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Deleted refresh token for user: {}", userId);
    }
    
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens(Instant.now());
        log.info("Cleaned up expired refresh tokens");
    }
} 