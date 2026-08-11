package com.uptimepulse.application.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailAlertService {

    private static final Logger log = LoggerFactory.getLogger(EmailAlertService.class);
    private final JavaMailSender mailSender;

    public EmailAlertService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendEmailAlert(String toEmail, String monitorName, String monitorUrl, String status, String alertMessage) {
        if (mailSender == null) {
            log.info("[EMAIL SUPPRESSED] JavaMailSender is not configured.");
            return;
        }
        if (toEmail == null || toEmail.isBlank()) return;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("alerts@uptimepulse.com", "UptimePulse Alerts");
            helper.setTo(toEmail);
            helper.setSubject("🚨 UptimePulse Alert: " + monitorName + " is " + status);

            String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #030712; color: #f1f5f9; padding: 20px;">
                  <div style="max-width: 520px; margin: 0 auto; background: #0a0f1e; border: 1px solid #334155; border-radius: 12px; padding: 24px;">
                    <h2 style="color: #6366f1; margin-top: 0;">UptimePulse Downtime Notification</h2>
                    <p style="font-size: 16px;">Monitor <strong style="color: #f43f5e;">%s</strong> is currently <span style="background: rgba(244,63,94,0.2); color: #f43f5e; padding: 3px 8px; border-radius: 4px; font-weight: bold;">%s</span>.</p>
                    <table style="width: 100%%; margin: 20px 0; border-collapse: collapse; color: #94a3b8; font-size: 14px;">
                      <tr><td style="padding: 8px 0; border-bottom: 1px solid #1e293b;"><strong>URL:</strong></td><td style="padding: 8px 0; border-bottom: 1px solid #1e293b; color: #f1f5f9;">%s</td></tr>
                      <tr><td style="padding: 8px 0; border-bottom: 1px solid #1e293b;"><strong>Alert Details:</strong></td><td style="padding: 8px 0; border-bottom: 1px solid #1e293b; color: #f1f5f9;">%s</td></tr>
                    </table>
                    <a href="%s" style="display: inline-block; background: #6366f1; color: #ffffff; text-decoration: none; padding: 10px 18px; border-radius: 8px; font-weight: bold; margin-top: 10px;">Visit Website</a>
                  </div>
                </body>
                </html>
                """, escapeHtml(monitorName), escapeHtml(status), escapeHtml(monitorUrl), escapeHtml(alertMessage), escapeHtml(monitorUrl));

            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("[EMAIL SENT] Alert delivered to {}", toEmail);

        } catch (Exception e) {
            log.warn("[EMAIL ALERT SUPPRESSED] SMTP delivery failed to {}: {}", toEmail, e.getMessage());
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
