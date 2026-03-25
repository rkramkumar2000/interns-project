package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.exception.TokenRefreshException;
import com.ecommerce.auth.model.AppRole;
import com.ecommerce.auth.model.RefreshToken;
import com.ecommerce.auth.model.Role;
import com.ecommerce.auth.model.User;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtUtils;
import com.ecommerce.auth.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RefreshTokenService refreshTokenService;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate access token
        String accessToken = jwtUtils.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        
        // Generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
        
        // Update last login
        userRepository.updateLastLogin(userDetails.getId(), LocalDateTime.now());
        
        // Publish login event
        publishAuthEvent("USER_LOGIN", userDetails.getId(), Map.of(
                "username", userDetails.getUsername(),
                "timestamp", LocalDateTime.now()
        ));

        return new JwtResponse(
                accessToken,
                refreshToken.getToken(),
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles);
    }

    @Override
    public MessageResponse registerUser(SignupRequest signupRequest) {
        log.info("Registering new user: {}", signupRequest.getUsername());
        
        if (userRepository.existsByUserName(signupRequest.getUsername())) {
            return new MessageResponse("Error: Username is already taken!", false);
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return new MessageResponse("Error: Email is already in use!", false);
        }

        // Create new user
        User user = new User(signupRequest.getUsername(),
                signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword()));
        
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setPhoneNumber(signupRequest.getPhoneNumber());

        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "seller":
                        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(sellerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        
        // Publish registration event
        publishAuthEvent("USER_REGISTERED", savedUser.getUserId(), Map.of(
                "username", savedUser.getUserName(),
                "email", savedUser.getEmail(),
                "roles", roles.stream().map(r -> r.getRoleName().name()).collect(Collectors.toList())
        ));

        return new MessageResponse("User registered successfully!");
    }

    @Override
    public MessageResponse logoutUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        Long userId = userDetails.getId();
        
        // Delete refresh token from database
        refreshTokenService.deleteByUserId(userId);
        
        // Publish logout event
        publishAuthEvent("USER_LOGOUT", userId, Map.of(
                "username", userDetails.getUsername(),
                "timestamp", LocalDateTime.now()
        ));
        
        SecurityContextHolder.clearContext();
        
        return new MessageResponse("User logged out successfully!");
    }

    @Override
    public JwtResponse refreshToken(String refreshTokenString) {
        return refreshTokenService.findByToken(refreshTokenString)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .map(userId -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
                    
                    // Create new access token
                    String accessToken = jwtUtils.generateTokenFromUsername(user.getUserName());
                    
                    // Create new refresh token (rotate refresh tokens for better security)
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(userId);
                    
                    List<String> roles = user.getRoles().stream()
                            .map(role -> role.getRoleName().name())
                            .collect(Collectors.toList());
                    
                    log.info("Refreshed tokens for user: {}", user.getUserName());
                    
                    return new JwtResponse(
                            accessToken,
                            newRefreshToken.getToken(),
                            user.getUserId(), 
                            user.getUserName(), 
                            user.getEmail(), 
                            roles);
                })
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found or invalid"));
    }

    @Override
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return getUserById(userDetails.getId());
        }
        throw new RuntimeException("No authenticated user found");
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return convertToDTO(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return convertToDTO(user);
    }

    @Override
    public MessageResponse changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return new MessageResponse("Error: Old password is incorrect!", false);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Publish password change event
        publishAuthEvent("PASSWORD_CHANGED", userId, Map.of(
                "timestamp", LocalDateTime.now()
        ));
        
        return new MessageResponse("Password changed successfully!");
    }

    @Override
    public MessageResponse resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Generate reset token (simplified for now)
        String resetToken = UUID.randomUUID().toString();
        
        // Publish password reset event
        publishAuthEvent("PASSWORD_RESET_REQUESTED", user.getUserId(), Map.of(
                "email", email,
                "resetToken", resetToken,
                "timestamp", LocalDateTime.now()
        ));
        
        return new MessageResponse("Password reset link sent to your email!");
    }

    @Override
    public MessageResponse verifyEmail(String token) {
        // Simplified email verification
        // In production, you would validate the token and update user's email verified status
        
        return new MessageResponse("Email verified successfully!");
    }

    @Override
    public MessageResponse resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        if (user.getEmailVerified()) {
            return new MessageResponse("Email is already verified!", false);
        }
        
        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();
        
        // Publish email verification event
        publishAuthEvent("EMAIL_VERIFICATION_REQUESTED", user.getUserId(), Map.of(
                "email", email,
                "verificationToken", verificationToken,
                "timestamp", LocalDateTime.now()
        ));
        
        return new MessageResponse("Verification email sent!");
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUserName(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setActive(user.getActive());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRoles(user.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toSet()));
        
        return dto;
    }
    
    private void publishAuthEvent(String eventType, Long userId, Map<String, Object> details) {
        try {
            AuthEvent event = new AuthEvent(eventType, userId, details, LocalDateTime.now());
            kafkaTemplate.send("auth-events", event);
            log.info("Published {} event for user: {}", eventType, userId);
        } catch (Exception e) {
            log.error("Failed to publish auth event", e);
        }
    }
    
    // Event class
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AuthEvent {
        private String eventType;
        private Long userId;
        private Map<String, Object> details;
        private LocalDateTime timestamp;
    }
} 