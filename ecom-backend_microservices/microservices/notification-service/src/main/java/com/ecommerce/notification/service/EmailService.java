package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.EmailRequest;

public interface EmailService {
    
    void sendSimpleEmail(String to, String subject, String body);
    
    void sendHtmlEmail(String to, String subject, String htmlBody);
    
    void sendTemplateEmail(String to, String subject, String templateName, Object model);
    
    void sendEmail(EmailRequest request);
    
    boolean isValidEmail(String email);
} 