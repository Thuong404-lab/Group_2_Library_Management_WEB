package com.lms.service.impl;

import com.lms.exception.ExternalServiceException;
import com.lms.service.EmailService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ExternalServiceException(messages.get("backend.email.sendFailed"), ex);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new ExternalServiceException(messages.get("backend.email.sendFailed"), ex);
        }
    }

    @Override
    public void sendNotificationEmail(String to, String recipientName, String title, String content) {
        String safeName = org.springframework.web.util.HtmlUtils.htmlEscape(recipientName == null || recipientName.isBlank() ? "Độc giả" : recipientName);
        String safeTitle = org.springframework.web.util.HtmlUtils.htmlEscape(title == null ? "Thông báo từ Thư viện" : title);
        String safeContent = org.springframework.web.util.HtmlUtils.htmlEscape(content == null ? "" : content);

        String htmlContent = """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background-color:#f5f1eb;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#33271f;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f5f1eb;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;background-color:#fffdf9;border:1px solid #e4d8ca;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(68,48,35,0.06);">
                          <!-- Header -->
                          <tr>
                            <td style="padding:28px 32px;background:linear-gradient(135deg, #7c4c20 0%%, #563314 100%%);color:#ffffff;text-align:center;">
                              <div style="font-family:'Georgia',serif;font-size:22px;font-weight:bold;letter-spacing:0.5px;">THƯ VIỆN & QUẢN LÝ THÔNG BÁO</div>
                              <div style="font-size:12px;opacity:0.85;margin-top:4px;text-transform:uppercase;letter-spacing:1px;">Hệ thống Thông báo Tự động</div>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:16px;line-height:1.5;margin-top:0;margin-bottom:16px;">Xin chào <strong>%s</strong>,</p>
                              
                              <!-- Notification Box -->
                              <div style="background-color:#fff8ef;border-left:4px solid #7c4c20;border-radius:8px;padding:20px;margin-bottom:24px;">
                                <h3 style="margin:0 0 10px 0;color:#563314;font-size:17px;font-family:'Georgia',serif;">%s</h3>
                                <p style="margin:0;font-size:15px;line-height:1.6;color:#4d3e33;">%s</p>
                              </div>

                              <p style="font-size:14px;color:#78685c;line-height:1.5;margin-bottom:0;">Vui lòng truy cập trang cá nhân của bạn trên Hệ thống Thư viện để theo dõi chi tiết phiếu mượn và số dư ví.</p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="padding:20px 32px;background-color:#f0e8dc;border-top:1px solid #e4d8ca;text-align:center;font-size:12px;color:#8a786c;line-height:1.5;">
                              <p style="margin:0;">Email này được gửi tự động từ Hệ thống Quản lý Thư viện.</p>
                              <p style="margin:4px 0 0 0;">Vui lòng không phản hồi trực tiếp qua email này.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, safeTitle, safeContent);

        sendHtmlEmail(to, safeTitle, htmlContent);
    }

    @Override
    public void sendStaffAccountCredentials(String to,
            String recipientName,
            String username,
            String rawPassword) {
        String subject = messages.get("backend.email.staffCredentials.subject");
        String safeBrand = escape(messages.get("backend.email.staffCredentials.brand"));
        String safeEyebrow = escape(messages.get("backend.email.staffCredentials.eyebrow"));
        String safeGreeting = escape(messages.get(
                "backend.email.staffCredentials.greeting",
                recipientName == null || recipientName.isBlank()
                        ? messages.get("backend.email.staffCredentials.defaultRecipient")
                        : recipientName));
        String safeIntroduction = escape(messages.get("backend.email.staffCredentials.introduction"));
        String safeUsernameLabel = escape(messages.get("backend.email.staffCredentials.username"));
        String safePasswordLabel = escape(messages.get("backend.email.staffCredentials.password"));
        String safeUsername = escape(username);
        String safePassword = escape(rawPassword);
        String safeSecurityNotice = escape(messages.get("backend.email.staffCredentials.securityNotice"));
        String safeFooter = escape(messages.get("backend.email.staffCredentials.footer"));

        String htmlContent = """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                </head>
                <body style="margin:0;padding:0;background:#f5f1eb;font-family:Arial,'Helvetica Neue',Helvetica,sans-serif;color:#33271f;-webkit-font-smoothing:antialiased;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:32px 16px;background:#f5f1eb;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;overflow:hidden;border:1px solid #e4d8ca;border-radius:16px;background:#fffdf9;box-shadow:0 8px 24px rgba(68,48,35,.08);">
                          <tr>
                            <td style="padding:28px 32px;background:linear-gradient(135deg,#8b5a2b 0%%,#513215 100%%);color:#fff;text-align:center;">
                              <div style="font-family:Arial,'Helvetica Neue',Helvetica,sans-serif;font-size:24px;font-weight:700;line-height:1.3;">%s</div>
                              <div style="margin-top:6px;font-size:12px;letter-spacing:1px;text-transform:uppercase;opacity:.86;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 14px;font-size:16px;line-height:1.55;">%s</p>
                              <p style="margin:0 0 22px;color:#66564b;font-size:14px;line-height:1.6;">%s</p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #eadbd1;border-radius:12px;background:#fff8f1;">
                                <tr>
                                  <td style="padding:16px 18px;border-bottom:1px solid #eadbd1;color:#765a49;font-size:13px;">%s</td>
                                  <td style="padding:16px 18px;border-bottom:1px solid #eadbd1;color:#2e1a08;font-size:15px;font-weight:700;text-align:right;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:16px 18px;color:#765a49;font-size:13px;">%s</td>
                                  <td style="padding:16px 18px;color:#2e1a08;font-family:Arial,'Helvetica Neue',Helvetica,sans-serif;font-size:15px;font-weight:700;text-align:right;">%s</td>
                                </tr>
                              </table>
                              <div style="margin-top:22px;padding:14px 16px;border-left:4px solid #c38040;border-radius:8px;background:#fceee7;color:#513215;font-size:13px;line-height:1.55;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px;border-top:1px solid #e4d8ca;background:#f8f3ed;color:#78685c;font-size:12px;line-height:1.5;text-align:center;">%s</td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                safeBrand,
                safeEyebrow,
                safeGreeting,
                safeIntroduction,
                safeUsernameLabel,
                safeUsername,
                safePasswordLabel,
                safePassword,
                safeSecurityNotice,
                safeFooter);

        sendHtmlEmail(to, subject, htmlContent);
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
