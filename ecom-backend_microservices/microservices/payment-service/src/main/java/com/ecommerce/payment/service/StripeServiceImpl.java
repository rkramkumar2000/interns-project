package com.ecommerce.payment.service;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ecommerce.payment.dto.CardDetailsDTO;
import com.ecommerce.payment.dto.CreatePaymentRequest;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeServiceImpl implements StripeService {
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @Value("${stripe.success.url}")
    private String successUrl;
    
    @Value("${stripe.cancel.url}")
    private String cancelUrl;
    
    @Override
    public PaymentIntent createPaymentIntent(CreatePaymentRequest request) throws Exception {
        try {
            // Create or get customer
            Customer customer = createOrGetCustomer(request.getCustomerEmail(), request.getCustomerName());
            
            // Build payment intent parameters
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                .setCurrency(request.getCurrency().toLowerCase())
                .setCustomer(customer.getId())
                .setDescription(request.getDescription())
                .setReceiptEmail(request.getCustomerEmail())
                .putMetadata("orderId", request.getOrderId().toString());
            
            // Add payment method if provided
            if (request.getSavedPaymentMethodId() != null) {
                paramsBuilder.setPaymentMethod(request.getSavedPaymentMethodId());
                paramsBuilder.setConfirm(true);
            } else if (request.getCardDetails() != null) {
                // For new card, create payment method first
                PaymentMethod paymentMethod = createPaymentMethod(request.getCardDetails(), customer);
                paramsBuilder.setPaymentMethod(paymentMethod.getId());
                
                if (request.getCardDetails().isSaveCard()) {
                    // Attach payment method to customer for future use
                    attachPaymentMethodToCustomer(paymentMethod.getId(), customer.getId());
                }
            }
            
            // Set return URL for 3D Secure
            if (request.getReturnUrl() != null) {
                paramsBuilder.setReturnUrl(request.getReturnUrl());
            }
            
            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());
            log.info("Created payment intent: {}", paymentIntent.getId());
            
            return paymentIntent;
            
        } catch (StripeException e) {
            log.error("Stripe error creating payment intent", e);
            throw new Exception("Failed to create payment intent: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentMethod createPaymentMethod(CardDetailsDTO cardDetails, Customer customer) throws Exception {
        try {
            PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(PaymentMethodCreateParams.CardDetails.builder()
                    .setNumber(cardDetails.getCardNumber())
                    .setExpMonth(Long.parseLong(cardDetails.getExpiryMonth()))
                    .setExpYear(Long.parseLong(cardDetails.getExpiryYear()))
                    .setCvc(cardDetails.getCvv())
                    .build())
                .setBillingDetails(PaymentMethodCreateParams.BillingDetails.builder()
                    .setName(cardDetails.getCardHolderName())
                    .setEmail(customer.getEmail())
                    .build())
                .build();
            
            return PaymentMethod.create(params);
            
        } catch (StripeException e) {
            log.error("Stripe error creating payment method", e);
            throw new Exception("Failed to create payment method: " + e.getMessage());
        }
    }
    
    @Override
    public Customer createOrGetCustomer(String email, String name) throws Exception {
        try {
            // Search for existing customer
            CustomerSearchParams searchParams = CustomerSearchParams.builder()
                .setQuery("email:'" + email + "'")
                .build();
            
            CustomerSearchResult searchResult = Customer.search(searchParams);
            
            if (!searchResult.getData().isEmpty()) {
                return searchResult.getData().get(0);
            }
            
            // Create new customer
            CustomerCreateParams createParams = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .build();
            
            return Customer.create(createParams);
            
        } catch (StripeException e) {
            log.error("Stripe error creating/getting customer", e);
            throw new Exception("Failed to create/get customer: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentMethod attachPaymentMethodToCustomer(String paymentMethodId, String customerId) throws Exception {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            return paymentMethod.attach(PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build());
                
        } catch (StripeException e) {
            log.error("Stripe error attaching payment method", e);
            throw new Exception("Failed to attach payment method: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws Exception {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent.confirm();
            
        } catch (StripeException e) {
            log.error("Stripe error confirming payment intent", e);
            throw new Exception("Failed to confirm payment intent: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws Exception {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent.cancel();
            
        } catch (StripeException e) {
            log.error("Stripe error cancelling payment intent", e);
            throw new Exception("Failed to cancel payment intent: " + e.getMessage());
        }
    }
    
    @Override
    public Refund createRefund(String paymentIntentId, BigDecimal amount, String reason) throws Exception {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                .setReason(mapRefundReason(reason))
                .build();
            
            return Refund.create(params);
            
        } catch (StripeException e) {
            log.error("Stripe error creating refund", e);
            throw new Exception("Failed to create refund: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws Exception {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
            
        } catch (StripeException e) {
            log.error("Stripe error retrieving payment intent", e);
            throw new Exception("Failed to retrieve payment intent: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentMethodCollection listCustomerPaymentMethods(String customerId) throws Exception {
        try {
            PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
            
            return PaymentMethod.list(params);
            
        } catch (StripeException e) {
            log.error("Stripe error listing payment methods", e);
            throw new Exception("Failed to list payment methods: " + e.getMessage());
        }
    }
    
    @Override
    public Session createCheckoutSession(Map<String, Object> params) throws Exception {
        try {
            SessionCreateParams.Builder sessionParamsBuilder = SessionCreateParams.builder()
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setMode(SessionCreateParams.Mode.PAYMENT);
            
            // Add line items from params
            if (params.containsKey("amount") && params.containsKey("currency")) {
                sessionParamsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency((String) params.get("currency"))
                                .setUnitAmount(((BigDecimal) params.get("amount"))
                                    .multiply(BigDecimal.valueOf(100)).longValue())
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName((String) params.getOrDefault("productName", "Order Payment"))
                                        .build()
                                )
                                .build()
                        )
                        .setQuantity(1L)
                        .build()
                );
            }
            
            // Add customer if provided
            if (params.containsKey("customerId")) {
                sessionParamsBuilder.setCustomer((String) params.get("customerId"));
            }
            
            return Session.create(sessionParamsBuilder.build());
            
        } catch (StripeException e) {
            log.error("Stripe error creating checkout session", e);
            throw new Exception("Failed to create checkout session: " + e.getMessage());
        }
    }
    
    @Override
    public Event constructEvent(String payload, String sigHeader) throws Exception {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
            
        } catch (Exception e) {
            log.error("Error constructing webhook event", e);
            throw new Exception("Invalid webhook signature");
        }
    }
    
    private RefundCreateParams.Reason mapRefundReason(String reason) {
        return switch (reason.toUpperCase()) {
            case "DUPLICATE" -> RefundCreateParams.Reason.DUPLICATE;
            case "FRAUDULENT" -> RefundCreateParams.Reason.FRAUDULENT;
            case "REQUESTED_BY_CUSTOMER" -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
            default -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        };
    }
} 