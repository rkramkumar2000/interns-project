package com.ecommerce.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotBlank(message = "Message content is required")
    private String message;
    
    private String senderId; // Custom sender ID if supported
    
    private boolean unicode; // Support for non-ASCII characters
    
    private boolean flash; // Flash SMS that appears on screen
    
    private String callbackUrl; // Webhook for delivery status
} 