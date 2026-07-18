package com.lms.service.impl;

import com.lms.exception.ExternalServiceException;
import com.lms.service.EmailService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * EmailServiceImpl - Triển khai dịch vụ gửi email
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final JavaMailSender mailSender;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String text) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        // Cần cấu hình spring.mail.username trong application.properties
        // message.setFrom("your-email@example.com"); // Có thể set từ đây hoặc cấu hình trong properties

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ExternalServiceException(messages.get("backend.email.sendFailed"), ex);
        }
    }
}
