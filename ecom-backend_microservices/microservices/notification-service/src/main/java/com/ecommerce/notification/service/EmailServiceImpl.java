package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.EmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("Simple email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("HTML email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void sendTemplateEmail(String to, String subject, String templateName, Object model) {
        try {
            Context context = new Context();
            if (model instanceof Map) {
                ((Map<String, Object>) model).forEach(context::setVariable);
            }
            
            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmail(to, subject, htmlContent);
            
            log.info("Template email sent to: {} using template: {}", to, templateName);
        } catch (Exception e) {
            log.error("Failed to send template email to: {}", to, e);
            throw new RuntimeException("Failed to send template email", e);
        }
    }
    
    @Override
    public void sendEmail(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            
            // Set CC and BCC if provided
            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }
            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }
            
            // Process template or use provided content
            if (request.getTemplateName() != null) {
                Context context = new Context();
                if (request.getTemplateVariables() != null) {
                    request.getTemplateVariables().forEach(context::setVariable);
                }
                String htmlContent = templateEngine.process(request.getTemplateName(), context);
                helper.setText(htmlContent, true);
            } else if (request.getHtmlBody() != null) {
                helper.setText(request.getHtmlBody(), true);
            } else {
                helper.setText(request.getBody(), false);
            }
            
            // Add attachments if any
            if (request.getAttachments() != null) {
                for (EmailRequest.Attachment attachment : request.getAttachments()) {
                    helper.addAttachment(attachment.getFilename(), 
                        () -> new java.io.ByteArrayInputStream(attachment.getContent()), 
                        attachment.getContentType());
                }
            }
            
            // Set priority
            if (request.isHighPriority()) {
                message.setHeader("X-Priority", "1");
                message.setHeader("Priority", "urgent");
            }
            
            mailSender.send(message);
            log.info("Complex email sent to: {}", request.getTo());
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    @Override
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
} 