package com.ecommerce.payment.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {
    
    private final Long userId;
    
    public JwtAuthenticationToken(String username, Long userId, 
                                 Collection<? extends GrantedAuthority> authorities) {
        super(username, null, authorities);
        this.userId = userId;
        setAuthenticated(true);
    }
    
    public Long getUserId() {
        return userId;
    }
} 