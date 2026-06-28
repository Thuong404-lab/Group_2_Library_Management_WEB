package com.lms.service.impl;

import com.lms.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * EmailServiceImpl - Triển khai dịch vụ gửi email
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Service
public class EmailServiceImpl implements EmailService {

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

        mailSender.send(message);
    }
}
