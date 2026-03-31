package com.healthcare.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for sending email notifications.
 * All methods are @Async so they run in a background thread.
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

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Diagnostic method to verify SMTP configuration at runtime.
     */
    public String getSmtpDiagnostics() {
        // Mask password for security but show enough to verify it's loaded
        String maskedFrom = fromEmail != null && fromEmail.length() > 4 
            ? fromEmail.substring(0, 4) + "****" + fromEmail.substring(fromEmail.indexOf("@")) 
            : "NOT_SET";
        return String.format("SMTP Config -> host=%s, port=%s, from=%s, sender_class=%s",
                mailHost, mailPort, maskedFrom, mailSender.getClass().getSimpleName());
    }

    // ========== 1. Welcome Email on Sign-Up ==========
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

        sendHtmlEmail(toEmail, subject, body);
    }

    // ========== 2. Buyer Verified / Approved Email ==========
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

        sendHtmlEmail(toEmail, subject, body);
    }

    // ========== 3. Inquiry Notification to Seller ==========
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

        sendHtmlEmail(sellerEmail, subject, body);
    }

    // ========== 4. Job Application Notification to Employer ==========
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

        sendHtmlEmail(employerEmail, subject, body);
    }

    // ========== 5. OTP Verification Email ==========
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

        sendHtmlEmail(toEmail, subject, body);
        log.info("=== OTP EMAIL SENT SUCCESSFULLY === to: {}", toEmail);
    }

    /**
     * Send password reset email with verification link
     */
    public void sendForgotPasswordEmail(String toEmail, String otp) {
        log.info("=== SENDING FORGOT PASSWORD EMAIL === to: {}", toEmail);
        String resetUrl = "https://medbiz.in/reset-password?email=" + toEmail + "&otp=" + otp;
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
