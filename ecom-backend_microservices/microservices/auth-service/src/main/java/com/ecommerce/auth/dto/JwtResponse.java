package com.ecommerce.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private List<String> roles;

    // Constructor for backward compatibility (will be deprecated)
    public JwtResponse(String token, Long id, String username, String email, List<String> roles) {
        this.accessToken = token;
        this.refreshToken = null;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
    
    // New constructor with both tokens
    public JwtResponse(String accessToken, String refreshToken, Long id, String username, String email, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
    
    // For backward compatibility with existing code that expects 'token' field
    public String getToken() {
        return accessToken;
    }
    
    public void setToken(String token) {
        this.accessToken = token;
    }
} 