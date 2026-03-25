package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.CardDetailsDTO;
import com.ecommerce.payment.dto.CreatePaymentRequest;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;

import java.math.BigDecimal;
import java.util.Map;

public interface StripeService {
    
    // Create payment intent
    PaymentIntent createPaymentIntent(CreatePaymentRequest request) throws Exception;
    
    // Create payment method
    PaymentMethod createPaymentMethod(CardDetailsDTO cardDetails, Customer customer) throws Exception;
    
    // Create or get customer
    Customer createOrGetCustomer(String email, String name) throws Exception;
    
    // Attach payment method to customer
    PaymentMethod attachPaymentMethodToCustomer(String paymentMethodId, String customerId) throws Exception;
    
    // Confirm payment intent
    PaymentIntent confirmPaymentIntent(String paymentIntentId) throws Exception;
    
    // Cancel payment intent
    PaymentIntent cancelPaymentIntent(String paymentIntentId) throws Exception;
    
    // Create refund
    Refund createRefund(String paymentIntentId, BigDecimal amount, String reason) throws Exception;
    
    // Retrieve payment intent
    PaymentIntent retrievePaymentIntent(String paymentIntentId) throws Exception;
    
    // List customer payment methods
    PaymentMethodCollection listCustomerPaymentMethods(String customerId) throws Exception;
    
    // Create checkout session (for Stripe Checkout)
    Session createCheckoutSession(Map<String, Object> params) throws Exception;
    
    // Handle webhook event
    Event constructEvent(String payload, String sigHeader) throws Exception;
} 