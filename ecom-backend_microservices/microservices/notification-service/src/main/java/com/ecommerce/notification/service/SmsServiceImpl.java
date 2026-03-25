package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.SmsRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;
    
    @Value("${twilio.enabled:true}")
    private boolean twilioEnabled;
    
    @PostConstruct
    public void init() {
        if (twilioEnabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized");
        } else {
            log.warn("Twilio SMS service is disabled");
        }
    }
    
    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!twilioEnabled) {
            log.warn("SMS not sent - Twilio is disabled. Would send to: {} - Message: {}", 
                    phoneNumber, message);
            return;
        }
        
        try {
            String formattedNumber = formatPhoneNumber(phoneNumber);
            
            Message twilioMessage = Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(fromPhoneNumber),
                    message
            ).create();
            
            log.info("SMS sent successfully. SID: {}, To: {}", 
                    twilioMessage.getSid(), phoneNumber);
            
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
    
    @Override
    public void sendSms(SmsRequest request) {
        if (!twilioEnabled) {
            log.warn("SMS not sent - Twilio is disabled. Would send to: {}", 
                    request.getPhoneNumber());
            return;
        }
        
        try {
            String formattedNumber = formatPhoneNumber(request.getPhoneNumber());
            
            MessageCreator creator = Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(fromPhoneNumber),
                    request.getMessage()
            );
            
            // Set optional parameters
            if (request.getCallbackUrl() != null) {
                creator.setStatusCallback(request.getCallbackUrl());
            }
            
            Message twilioMessage = creator.create();
            
            log.info("SMS sent successfully. SID: {}, To: {}", 
                    twilioMessage.getSid(), request.getPhoneNumber());
            
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", request.getPhoneNumber(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
    
    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        
        // Basic validation - starts with + and has 10-15 digits
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        return cleaned.matches("^\\+?[1-9]\\d{9,14}$");
    }
    
    @Override
    public String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("Phone number cannot be null");
        }
        
        // Remove all non-numeric characters except +
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        
        // Add + if not present (assuming it's needed)
        if (!cleaned.startsWith("+")) {
            // Default to US country code if no country code present
            if (cleaned.length() == 10) {
                cleaned = "+1" + cleaned;
            } else {
                cleaned = "+" + cleaned;
            }
        }
        
        return cleaned;
    }
} 