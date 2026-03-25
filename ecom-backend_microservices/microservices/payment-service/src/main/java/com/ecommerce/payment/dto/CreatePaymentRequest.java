package com.ecommerce.payment.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency = "USD";
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // CARD, PAYPAL, etc.
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    private String description;
    
    // Billing address
    private AddressDTO billingAddress;
    
    // For saved payment methods
    private String savedPaymentMethodId;
    
    // For new card payments
    private CardDetailsDTO cardDetails;
    
    // Additional metadata
    private String metadata;
    
    // Return URLs for 3D Secure or other redirects
    private String returnUrl;
    private String cancelUrl;
} 