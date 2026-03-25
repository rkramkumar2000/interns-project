package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;

public interface AuthService {
    
    JwtResponse authenticateUser(LoginRequest loginRequest);
    
    MessageResponse registerUser(SignupRequest signupRequest);
    
    MessageResponse logoutUser();
    
    JwtResponse refreshToken(String refreshToken);
    
    UserDTO getCurrentUser();
    
    UserDTO getUserById(Long userId);
    
    UserDTO getUserByUsername(String username);
    
    MessageResponse changePassword(Long userId, String oldPassword, String newPassword);
    
    MessageResponse resetPassword(String email);
    
    MessageResponse verifyEmail(String token);
    
    MessageResponse resendVerificationEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
} 