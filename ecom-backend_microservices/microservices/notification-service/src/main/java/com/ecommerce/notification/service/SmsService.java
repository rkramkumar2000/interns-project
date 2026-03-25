package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.SmsRequest;

public interface SmsService {
    
    void sendSms(String phoneNumber, String message);
    
    void sendSms(SmsRequest request);
    
    boolean isValidPhoneNumber(String phoneNumber);
    
    String formatPhoneNumber(String phoneNumber);
} 