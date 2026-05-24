package com.lms.notification.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("LMS - Password Reset OTP");
            message.setText(
                    "Hello,\n\n" +
                    "Your OTP for password reset is: " + otp + "\n\n" +
                    "This OTP is valid for 10 minutes.\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Regards,\nLibrary Management System"
            );
            message.setFrom("rameez0811@gmail.com");
            mailSender.send(message);
            System.out.println("[EmailService] OTP email sent to: " + toEmail);
        } catch (Exception e) {
            // Log the OTP to console as fallback (for development/testing)
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("  [EMAIL FALLBACK] Failed to send email.");
            System.out.println("  To: " + toEmail);
            System.out.println("  OTP: " + otp);
            System.out.println("  Error: " + e.getMessage());
            System.out.println("═══════════════════════════════════════════════");
        }
    }
}
