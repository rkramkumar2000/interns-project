package com.ecommerce.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRequest {
    
    @NotBlank(message = "Email recipient is required")
    @Email(message = "Invalid email format")
    private String to;
    
    private List<String> cc;
    
    private List<String> bcc;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String body;
    
    private String htmlBody;
    
    private String templateName;
    
    private Map<String, Object> templateVariables;
    
    private List<Attachment> attachments;
    
    private Map<String, String> headers;
    
    private boolean highPriority;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attachment {
        private String filename;
        private byte[] content;
        private String contentType;
    }
} 