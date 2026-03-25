package com.ecommerce.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {
    
    private final TemplateEngine templateEngine;
    
    // Predefined template names
    private static final Set<String> AVAILABLE_TEMPLATES = Set.of(
            "welcome",
            "order-confirmation",
            "order-shipped",
            "order-delivered",
            "payment-success",
            "payment-failed",
            "password-reset",
            "account-verification",
            "promotional",
            "cart-abandoned"
    );
    
    @Override
    public String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }
            
            // Add common variables
            context.setVariable("appName", "E-Commerce Store");
            context.setVariable("supportEmail", "support@ecommerce.com");
            context.setVariable("year", java.time.Year.now().getValue());
            
            String processedContent = templateEngine.process(templateName, context);
            log.debug("Template {} processed successfully", templateName);
            
            return processedContent;
        } catch (TemplateInputException e) {
            log.error("Template not found or error processing: {}", templateName, e);
            throw new RuntimeException("Template processing failed: " + templateName, e);
        } catch (Exception e) {
            log.error("Unexpected error processing template: {}", templateName, e);
            throw new RuntimeException("Template processing failed", e);
        }
    }
    
    @Override
    public String processInlineTemplate(String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }
            
            // Process inline template string
            return templateEngine.process(template, context);
        } catch (Exception e) {
            log.error("Error processing inline template", e);
            throw new RuntimeException("Inline template processing failed", e);
        }
    }
    
    @Override
    public boolean templateExists(String templateName) {
        return AVAILABLE_TEMPLATES.contains(templateName);
    }
    
    @Override
    public void validateTemplate(String templateName) {
        if (!templateExists(templateName)) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
    }
} 