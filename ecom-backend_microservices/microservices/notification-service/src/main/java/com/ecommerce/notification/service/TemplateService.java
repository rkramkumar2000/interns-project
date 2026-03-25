package com.ecommerce.notification.service;

import java.util.Map;

public interface TemplateService {
    
    String processTemplate(String templateName, Map<String, Object> variables);
    
    String processInlineTemplate(String template, Map<String, Object> variables);
    
    boolean templateExists(String templateName);
    
    void validateTemplate(String templateName);
} 