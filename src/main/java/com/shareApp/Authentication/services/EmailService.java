// EmailService.java
package com.shareApp.Authentication.services;

import com.shareApp.Authentication.entities.OTP;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from}")
    private String fromEmail;
    
    @Value("${app.mail.fromName}")
    private String fromName;
    
    @Async
    public void sendOTPEmail(String toEmail, String otp, OTP.OTPType type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(getEmailSubject(type));
            helper.setText(buildEmailContent(otp, type), true);
            
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
    
    private String getEmailSubject(OTP.OTPType type) {
        return switch (type) {
            case SIGNUP -> "Your SafeShare Account Verification Code";
            case PASSWORD_RESET -> "Reset Your SafeShare Password";
            case EMAIL_VERIFICATION -> "Verify Your SafeShare Email";
        };
    }
    
    private String buildEmailContent(String otp, OTP.OTPType type) {
        String title = getEmailTitle(type);
        String description = getEmailDescription(type);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8f9fa;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: white; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden;">
                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px 20px; text-align: center;">
                            <h1 style="color: white; font-size: 28px; margin: 0; font-weight: 600;">SafeShare</h1>
                        </div>
                        
                        <!-- Content -->
                        <div style="padding: 40px 30px;">
                            <h2 style="color: #323130; font-size: 24px; margin: 0 0 20px 0; font-weight: 600;">%s</h2>
                            
                            <p style="color: #605e5c; font-size: 16px; line-height: 1.6; margin-bottom: 30px;">%s</p>
                            
                            <!-- OTP Box -->
                            <div style="background: linear-gradient(135deg, #f8f9ff 0%%, #e8f2ff 100%%); border: 2px solid #e3f2fd; border-radius: 12px; padding: 30px; text-align: center; margin: 30px 0;">
                                <div style="font-size: 36px; font-weight: bold; color: #1976d2; letter-spacing: 8px; font-family: 'Courier New', monospace;">%s</div>
                            </div>
                            
                            <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 15px; margin: 20px 0;">
                                <p style="color: #856404; font-size: 14px; margin: 0; font-weight: 500;">
                                    ⚠️ This code will expire in 10 minutes. If you didn't request this code, please ignore this email.
                                </p>
                            </div>
                            
                            <div style="text-align: center; margin-top: 30px;">
                                <p style="color: #6c757d; font-size: 14px; margin: 0;">
                                    Having trouble? Contact our support team at 
                                    <a href="mailto:support@safeshare.com" style="color: #1976d2; text-decoration: none;">support@safeshare.com</a>
                                </p>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background-color: #f8f9fa; padding: 20px 30px; border-top: 1px solid #e9ecef;">
                            <p style="color: #6c757d; font-size: 12px; text-align: center; margin: 0;">
                                This is an automated message from SafeShare. Please do not reply to this email.
                            </p>
                            <p style="color: #6c757d; font-size: 12px; text-align: center; margin: 10px 0 0 0;">
                                © 2025 SafeShare. All rights reserved.
                            </p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, 
            getEmailSubject(type), 
            title, 
            description, 
            otp
        );
    }
    
    private String getEmailTitle(OTP.OTPType type) {
        return switch (type) {
            case SIGNUP -> "Verify your email address";
            case PASSWORD_RESET -> "Reset your password";
            case EMAIL_VERIFICATION -> "Verify your email";
        };
    }
    
    private String getEmailDescription(OTP.OTPType type) {
        return switch (type) {
            case SIGNUP -> "To complete your account setup, please use this verification code:";
            case PASSWORD_RESET -> "Use this code to reset your password:";
            case EMAIL_VERIFICATION -> "Use this code to verify your email address:";
        };
    }
}