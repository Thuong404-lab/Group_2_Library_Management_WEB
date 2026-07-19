package com.lms.service;

/**
 * EmailService - Interface định nghĩa dịch vụ gửi email
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
public interface EmailService {
    void sendEmail(String to, String subject, String text);

    void sendHtmlEmail(String to, String subject, String htmlContent);
}
