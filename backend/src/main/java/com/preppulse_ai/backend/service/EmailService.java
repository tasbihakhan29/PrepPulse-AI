package com.preppulse_ai.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("PrepPulse AI - Reset Password OTP");
            message.setText("Hello,\n\n" +
                    "We received a request to reset your password. Use the following 6-digit OTP code to complete the process:\n\n" +
                    otpCode + "\n\n" +
                    "This OTP is valid for 10 minutes. If you did not request this, you can ignore this email.\n\n" +
                    "Best regards,\n" +
                    "PrepPulse AI Team");
            mailSender.send(message);
            log.info("Reset password OTP email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again later.");
        }
    }
}
