package com.healthcare.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Service for sending email notifications.
 * OTP/password-reset emails are sent SYNCHRONOUSLY so failures propagate to the caller.
 * Welcome/notification emails are @Async so they don't block the response.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private String mailPort;

    @Value("${spring.mail.password}")
    private String mailPassword;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Runs after dependency injection to verify and fix SMTP configuration.
     * This ensures the JavaMailSender has the correct credentials at runtime,
     * especially important when environment variables override application.properties.
     */
    @PostConstruct
    public void initMailSender() {
        log.info("========== EMAIL SERVICE INITIALIZATION ==========");
        log.info("Mail Host: {}", mailHost);
        log.info("Mail Port: {}", mailPort);
        log.info("Mail Username: {}", fromEmail);
        
        // Mask password for logging (show first 4 chars only)
        String maskedPw = mailPassword != null && mailPassword.length() > 4 
            ? mailPassword.substring(0, 4) + "****" : "NOT_SET";
        log.info("Mail Password (masked): {}", maskedPw);
        log.info("Mail Password length: {}", mailPassword != null ? mailPassword.length() : 0);

        // If the auto-configured sender is a JavaMailSenderImpl, force-apply the
        // resolved properties so that env-var overrides actually take effect.
        if (mailSender instanceof JavaMailSenderImpl impl) {
            impl.setHost(mailHost);
            impl.setPort(Integer.parseInt(mailPort));
            impl.setUsername(fromEmail);
            
            // Strip any stray spaces from the password (Gmail app passwords are
            // displayed with spaces for readability, e.g. "abcd efgh ijkl mnop",
            // but the actual password is "abcdefghijklmnop").
            String cleanPassword = mailPassword != null ? mailPassword.replaceAll("\\s+", "") : mailPassword;
            impl.setPassword(cleanPassword);
            
            log.info("Password cleaned: original length={}, cleaned length={}", 
                     mailPassword != null ? mailPassword.length() : 0,
                     cleanPassword != null ? cleanPassword.length() : 0);

            Properties props = impl.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.connectiontimeout", "15000");
            props.put("mail.smtp.timeout", "15000");
            props.put("mail.smtp.writetimeout", "15000");
            // Enable debug output for SMTP session
            props.put("mail.debug", "true");
            
            log.info("✅ JavaMailSenderImpl reconfigured successfully");
            log.info("   Final host={}, port={}, username={}", 
                     impl.getHost(), impl.getPort(), impl.getUsername());
        } else {
            log.warn("⚠️ mailSender is NOT JavaMailSenderImpl — type: {}. Cannot force-reconfigure.",
                     mailSender.getClass().getName());
        }
        log.info("========== EMAIL SERVICE INITIALIZATION COMPLETE ==========");
    }

    /**
     * Diagnostic method to verify SMTP configuration at runtime.
     */
    public String getSmtpDiagnostics() {
        String maskedFrom = fromEmail != null && fromEmail.length() > 4 
            ? fromEmail.substring(0, 4) + "****" + fromEmail.substring(fromEmail.indexOf("@")) 
            : "NOT_SET";
        
        String pwInfo = "NOT_SET";
        if (mailPassword != null) {
            String cleaned = mailPassword.replaceAll("\\s+", "");
            pwInfo = String.format("length=%d, cleaned_length=%d", mailPassword.length(), cleaned.length());
        }

        String senderInfo = "unknown";
        if (mailSender instanceof JavaMailSenderImpl impl) {
            senderInfo = String.format("host=%s, port=%d, username=%s", 
                impl.getHost(), impl.getPort(), impl.getUsername());
        }
        
        return String.format(
            "SMTP Config -> host=%s, port=%s, from=%s, password=%s, sender=[%s], sender_class=%s",
            mailHost, mailPort, maskedFrom, pwInfo, senderInfo, mailSender.getClass().getSimpleName());
    }

    /**
     * Test SMTP connectivity by sending a test email to the sender's own address.
     * Returns a diagnostic string.
     */
    public String testSmtpConnection() {
        try {
            log.info("Testing SMTP connection...");
            if (mailSender instanceof JavaMailSenderImpl impl) {
                impl.testConnection();
                log.info("✅ SMTP connection test PASSED");
                return "SMTP connection test PASSED. Server is reachable and credentials are valid.";
            }
            return "Cannot test: mailSender is not JavaMailSenderImpl";
        } catch (Exception e) {
            log.error("❌ SMTP connection test FAILED: {}", e.getMessage(), e);
            return "SMTP connection test FAILED: " + e.getMessage();
        }
    }

    // ========== 1. Welcome Email on Sign-Up (Async - fire & forget) ==========
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        String subject = "Welcome to MedBiz Marketplace!";
        String body = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#f4f4f4;'>"
            + "<div style='max-width:600px;margin:30px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#0d9488,#14b8a6);padding:30px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:28px;'>Welcome to MedBiz!</h1>"
            + "</div>"
            + "<div style='padding:30px;'>"
            + "<h2 style='color:#333;'>Hello " + fullName + ",</h2>"
            + "<p style='color:#555;font-size:16px;line-height:1.6;'>Thank you for registering on <strong>MedBiz Marketplace</strong> — India's premier healthcare business portal.</p>"
            + "<p style='color:#555;font-size:16px;line-height:1.6;'>You can now:</p>"
            + "<ul style='color:#555;font-size:15px;line-height:1.8;'>"
            + "<li>Browse and post listings for hospitals, pharma companies, diagnostic centres & equipment</li>"
            + "<li>Find and post healthcare job opportunities</li>"
            + "<li>Connect with verified buyers and sellers</li>"
            + "</ul>"
            + "<div style='text-align:center;margin:30px 0;'>"
            + "<a href='https://medbiz.in/dashboard' style='background:#0d9488;color:#fff;padding:14px 30px;text-decoration:none;border-radius:6px;font-size:16px;font-weight:bold;'>Go to Dashboard</a>"
            + "</div>"
            + "<p style='color:#999;font-size:13px;'>If you did not create this account, please ignore this email.</p>"
            + "</div>"
            + "<div style='background:#f9fafb;padding:15px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='color:#9ca3af;font-size:12px;margin:0;'>© 2026 MedBiz Marketplace. All rights reserved.</p>"
            + "</div></div></body></html>";

        try {
            sendHtmlEmail(toEmail, subject, body);
        } catch (Exception e) {
            // Async method - log error but don't propagate (welcome email is non-critical)
            log.error("❌ Failed to send welcome email to {} (non-critical, will not block registration)", toEmail, e);
        }
    }

    // ========== 2. Buyer Verified / Approved Email (Async) ==========
    @Async
    public void sendBuyerApprovedEmail(String toEmail, String fullName) {
        String subject = "Your Buyer Verification is Approved — MedBiz";
        String body = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#f4f4f4;'>"
            + "<div style='max-width:600px;margin:30px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#059669,#10b981);padding:30px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:28px;'>✅ Verified Buyer</h1>"
            + "</div>"
            + "<div style='padding:30px;'>"
            + "<h2 style='color:#333;'>Congratulations, " + fullName + "!</h2>"
            + "<p style='color:#555;font-size:16px;line-height:1.6;'>Your KYC verification has been <strong style='color:#059669;'>approved</strong> by our admin team.</p>"
            + "<p style='color:#555;font-size:16px;line-height:1.6;'>As a verified buyer, you now have access to:</p>"
            + "<ul style='color:#555;font-size:15px;line-height:1.8;'>"
            + "<li>View full details of <strong>confidential listings</strong></li>"
            + "<li>Contact sellers directly for confidential deals</li>"
            + "<li>Priority access to premium listings</li>"
            + "</ul>"
            + "<div style='text-align:center;margin:30px 0;'>"
            + "<a href='https://medbiz.in/listings' style='background:#059669;color:#fff;padding:14px 30px;text-decoration:none;border-radius:6px;font-size:16px;font-weight:bold;'>Browse Listings</a>"
            + "</div>"
            + "</div>"
            + "<div style='background:#f9fafb;padding:15px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='color:#9ca3af;font-size:12px;margin:0;'>© 2026 MedBiz Marketplace. All rights reserved.</p>"
            + "</div></div></body></html>";

        try {
            sendHtmlEmail(toEmail, subject, body);
        } catch (Exception e) {
            log.error("❌ Failed to send buyer approved email to {} (non-critical)", toEmail, e);
        }
    }

    // ========== 3. Inquiry Notification to Seller (Async) ==========
    @Async
    public void sendInquiryNotificationToSeller(String sellerEmail, String sellerName,
            String buyerName, String buyerEmail, String buyerPhone, String message, String listingTitle) {
        String subject = "New Inquiry for Your Listing: " + listingTitle;
        String body = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#f4f4f4;'>"
            + "<div style='max-width:600px;margin:30px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#2563eb,#3b82f6);padding:30px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:28px;'>📩 New Inquiry Received</h1>"
            + "</div>"
            + "<div style='padding:30px;'>"
            + "<h2 style='color:#333;'>Hello " + sellerName + ",</h2>"
            + "<p style='color:#555;font-size:16px;line-height:1.6;'>You have received a new inquiry for your listing <strong>\"" + listingTitle + "\"</strong>.</p>"
            + "<div style='background:#f0f9ff;border:1px solid #bae6fd;border-radius:8px;padding:20px;margin:20px 0;'>"
            + "<h3 style='color:#0369a1;margin-top:0;'>Buyer Details</h3>"
            + "<table style='width:100%;font-size:15px;color:#555;'>"
            + "<tr><td style='padding:5px 0;font-weight:bold;width:100px;'>Name:</td><td>" + buyerName + "</td></tr>"
            + "<tr><td style='padding:5px 0;font-weight:bold;'>Email:</td><td><a href='mailto:" + buyerEmail + "' style='color:#2563eb;'>" + buyerEmail + "</a></td></tr>"
            + "<tr><td style='padding:5px 0;font-weight:bold;'>Phone:</td><td><a href='tel:" + buyerPhone + "' style='color:#2563eb;'>" + buyerPhone + "</a></td></tr>"
            + "</table>"
            + "<h3 style='color:#0369a1;margin-bottom:5px;'>Message</h3>"
            + "<p style='color:#555;font-size:15px;line-height:1.6;background:#fff;padding:12px;border-radius:6px;border:1px solid #e0f2fe;'>" + message + "</p>"
            + "</div>"
            + "<div style='text-align:center;margin:25px 0;'>"
            + "<a href='https://medbiz.in/dashboard/inquiries' style='background:#2563eb;color:#fff;padding:14px 30px;text-decoration:none;border-radius:6px;font-size:16px;font-weight:bold;'>View Inquiries</a>"
            + "</div>"
            + "</div>"
            + "<div style='background:#f9fafb;padding:15px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='color:#9ca3af;font-size:12px;margin:0;'>© 2026 MedBiz Marketplace. All rights reserved.</p>"
            + "</div></div></body></html>";

        try {
            sendHtmlEmail(sellerEmail, subject, body);
        } catch (Exception e) {
            log.error("❌ Failed to send inquiry notification to {} (non-critical)", sellerEmail, e);
        }
    }

    // ========== 4. Job Application Notification to Employer (Async) ==========
    @Async
    public void sendJobApplicationNotification(String employerEmail, String employerName,
            String seekerName, String seekerEmail, String jobTitle) {
        String subject = "New Application for: " + jobTitle;
        String body = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#f4f4f4;'>"
            + "<div style='max-width:600px;margin:30px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#7c3aed,#8b5cf6);padding:30px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:28px;'>📋 New Job Application</h1>"
            + "</div>"
            + "<div style='padding:30px;'>"
            + "<h2 style='color:#333;'>Hello " + employerName + ",</h2>"
            + "<p style='color:#555;font-size:16px;line-height:1.6;'>A new candidate has applied for your job posting <strong>\"" + jobTitle + "\"</strong>.</p>"
            + "<div style='background:#f5f3ff;border:1px solid #ddd6fe;border-radius:8px;padding:20px;margin:20px 0;'>"
            + "<h3 style='color:#6d28d9;margin-top:0;'>Applicant Details</h3>"
            + "<table style='width:100%;font-size:15px;color:#555;'>"
            + "<tr><td style='padding:5px 0;font-weight:bold;width:100px;'>Name:</td><td>" + seekerName + "</td></tr>"
            + "<tr><td style='padding:5px 0;font-weight:bold;'>Email:</td><td><a href='mailto:" + seekerEmail + "' style='color:#7c3aed;'>" + seekerEmail + "</a></td></tr>"
            + "</table>"
            + "</div>"
            + "<div style='text-align:center;margin:25px 0;'>"
            + "<a href='https://medbiz.in/dashboard/applications' style='background:#7c3aed;color:#fff;padding:14px 30px;text-decoration:none;border-radius:6px;font-size:16px;font-weight:bold;'>View Applications</a>"
            + "</div>"
            + "</div>"
            + "<div style='background:#f9fafb;padding:15px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='color:#9ca3af;font-size:12px;margin:0;'>© 2026 MedBiz Marketplace. All rights reserved.</p>"
            + "</div></div></body></html>";

        try {
            sendHtmlEmail(employerEmail, subject, body);
        } catch (Exception e) {
            log.error("❌ Failed to send job application notification to {} (non-critical)", employerEmail, e);
        }
    }

    // ========== 5. OTP Verification Email (SYNCHRONOUS — must propagate errors) ==========
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("=== SENDING OTP EMAIL === to: {}, otp: {}", toEmail, otp);
        log.info("SMTP diagnostics: {}", getSmtpDiagnostics());
        
        String subject = "Your MedBiz Verification Code: " + otp;
        String body = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#f4f4f4;'>"
            + "<div style='max-width:600px;margin:30px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#0d9488,#14b8a6);padding:30px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:28px;'>🔐 Email Verification</h1>"
            + "</div>"
            + "<div style='padding:30px;text-align:center;'>"
            + "<p style='color:#555;font-size:16px;line-height:1.6;'>Use the following verification code to complete your registration on <strong>MedBiz Marketplace</strong>.</p>"
            + "<div style='background:linear-gradient(135deg,#f0fdfa,#ccfbf1);border:2px dashed #14b8a6;border-radius:12px;padding:25px;margin:25px auto;max-width:280px;'>"
            + "<p style='font-size:40px;font-weight:bold;color:#0d9488;letter-spacing:12px;margin:0;font-family:monospace;'>" + otp + "</p>"
            + "</div>"
            + "<p style='color:#999;font-size:14px;'>This code expires in <strong>5 minutes</strong>.</p>"
            + "<p style='color:#999;font-size:13px;margin-top:20px;'>If you did not request this code, please ignore this email.</p>"
            + "</div>"
            + "<div style='background:#f9fafb;padding:15px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='color:#9ca3af;font-size:12px;margin:0;'>© 2026 MedBiz Marketplace. All rights reserved.</p>"
            + "</div></div></body></html>";

        // This is SYNCHRONOUS — exception will propagate to the controller
        sendHtmlEmail(toEmail, subject, body);
        log.info("=== OTP EMAIL SENT SUCCESSFULLY === to: {}", toEmail);
    }

    /**
     * Send password reset email (SYNCHRONOUS — must propagate errors)
     */
    public void sendForgotPasswordEmail(String toEmail, String otp) {
        log.info("=== SENDING FORGOT PASSWORD EMAIL === to: {}", toEmail);
        String subject = "Reset Your Password - MedBiz";
        String body = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#f4f4f4;'>"
            + "<div style='max-width:600px;margin:30px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#4f46e5,#6366f1);padding:30px;text-align:center;'>"
            + "<h1 style='color:#fff;margin:0;font-size:28px;'>🔑 Password Reset</h1>"
            + "</div>"
            + "<div style='padding:40px;text-align:center;'>"
            + "<p style='color:#374151;font-size:18px;font-weight:600;'>Hello,</p>"
            + "<p style='color:#4b5563;font-size:16px;line-height:1.6;'>We received a request to reset your password for your MedBiz account. Your verification code is:</p>"
            + "<div style='background:linear-gradient(135deg,#eef2ff,#e0e7ff);border:2px dashed #6366f1;border-radius:12px;padding:25px;margin:25px auto;max-width:280px;'>"
            + "<p style='font-size:40px;font-weight:bold;color:#4f46e5;letter-spacing:12px;margin:0;font-family:monospace;'>" + otp + "</p>"
            + "</div>"
            + "<p style='color:#9ca3af;font-size:14px;'>This code expires in <strong>5 minutes</strong>.</p>"
            + "<p style='color:#ef4444;font-size:14px;margin-top:25px;border-top:1px solid #f3f4f6;padding-top:20px;'>If you did not request a password reset, please ignore this email.</p>"
            + "</div>"
            + "<div style='background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;'>"
            + "<p style='color:#9ca3af;font-size:12px;margin:0;'>© 2026 MedBiz Marketplace. All rights reserved.</p>"
            + "</div></div></body></html>";

        sendHtmlEmail(toEmail, subject, body);
        log.info("=== FORGOT PASSWORD EMAIL SENT SUCCESSFULLY === to: {}", toEmail);
    }

    // ========== Helper ==========
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            log.info("Attempting to send email to: {} | Subject: {} | From: {} | Host: {}:{}", 
                     to, subject, fromEmail, mailHost, mailPort);
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("✅ Email sent successfully to: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("❌ FAILED to send email to: {} | Subject: {} | Error: {}", to, subject, e.getMessage());
            log.error("❌ Full stack trace:", e);
            throw new RuntimeException("Failed to send email to " + to + ". SMTP Error: " + e.getMessage(), e);
        }
    }
}
