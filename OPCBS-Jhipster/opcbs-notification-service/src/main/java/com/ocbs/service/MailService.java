package com.ocbs.service;

import com.ocbs.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    private final JHipsterProperties jHipsterProperties;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    public MailService(
        JHipsterProperties jHipsterProperties,
        JavaMailSender javaMailSender,
        MessageSource messageSource,
        SpringTemplateEngine templateEngine
    ) {
        this.jHipsterProperties = jHipsterProperties;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    /** Synchronous entry point used by the Rabbit worker so ACK follows SMTP completion. */
    public void sendEmailAndWait(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}'",
            isMultipart,
            isHtml,
            to,
            subject
        );

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            String from = jHipsterProperties.getMail().getFrom();
            if (from == null || from.isBlank() || from.contains("localhost") || from.contains("mindbridge.local")) {
                from = "quanghvhe161464@fpt.edu.vn";
            }
            try {
                message.setFrom(from, "MindBridge");
            } catch (Exception ignored) {
                message.setFrom(from);
            }
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            LOG.info("📧 [JavaMailSender] Successfully sent email to '{}' [Subject: '{}']", to, subject);
        } catch (MailException | MessagingException e) {
            LOG.error("❌ [JavaMailSender] Email could not be sent to user '{}' [Subject: '{}']: {}", to, subject, e.getMessage());
            throw new IllegalStateException("Email delivery failed", e);
        }
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        sendEmailFromTemplateSync(user, templateName, titleKey);
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendActivationEmail(User user) {
        LOG.debug("Sending activation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendActivationOtpEmail(User user, String otp) {
        if (user.getEmail() == null) {
            LOG.warn("Email doesn't exist for user '{}', cannot send OTP", user.getLogin());
            return;
        }

        LOG.info("==================================================================");
        LOG.info(">>> [MindBridge] ACTIVATION OTP FOR USER '{}' ({}): {} <<<", user.getLogin(), user.getEmail(), otp);
        LOG.info("==================================================================");

        String subject = "[MindBridge] Mã OTP Xác Thực Kích Hoạt Tài Khoản: " + otp;
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; color: #0f172a; margin: 0; padding: 20px; }
                .email-card { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 20px rgba(15, 118, 110, 0.08); }
                .email-header { background: linear-gradient(135deg, #0f766e 0%, #115e59 100%); padding: 30px; text-align: center; color: #ffffff; }
                .brand-title { font-size: 24px; font-weight: 800; letter-spacing: -0.5px; margin: 0; }
                .brand-sub { color: #ccfbf1; font-size: 14px; margin-top: 5px; }
                .email-body { padding: 32px 28px; }
                .greeting { font-size: 18px; font-weight: 700; color: #0f172a; margin-bottom: 12px; }
                .otp-box { background: #f0fdfa; border: 2px dashed #0f766e; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0; }
                .otp-label { font-size: 13px; font-weight: 700; text-transform: uppercase; color: #0f766e; letter-spacing: 1px; margin-bottom: 6px; }
                .otp-code { font-size: 34px; font-weight: 900; letter-spacing: 8px; color: #0f172a; font-family: monospace; }
                .warning-text { font-size: 13px; color: #64748b; line-height: 1.5; margin-top: 16px; }
                .email-footer { background: #f8fafc; padding: 18px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="email-card">
                <div class="email-header">
                  <h1 class="brand-title">MindBridge Healthcare</h1>
                  <p class="brand-sub">Nền Tảng Chăm Sóc Sức Khỏe & Tư Vấn Y Tế Tâm Lý</p>
                </div>
                <div class="email-body">
                  <div class="greeting">Xin chào %s,</div>
                  <p>Cảm ơn bạn đã đăng ký tài khoản tại MindBridge. Vui lòng sử dụng mã xác thực OTP dưới đây để hoàn tất việc kích hoạt tài khoản của bạn:</p>
                  
                  <div class="otp-box">
                    <div class="otp-label">MÃ XÁC THỰC OTP (Hiệu lực trong 10 phút)</div>
                    <div class="otp-code">%s</div>
                  </div>

                  <p class="warning-text">
                    ⚠️ <strong>Lưu ý bảo mật:</strong> Tuyệt đối không chia sẻ mã xác thực này cho bất kỳ ai. Đội ngũ MindBridge không bao giờ yêu cầu bạn cung cấp mã OTP.
                  </p>
                </div>
                <div class="email-footer">
                  © 2026 MindBridge Inc. Mọi quyền được bảo lưu.
                </div>
              </div>
            </body>
            </html>
            """.formatted(user.getLogin(), otp);

        sendEmailSync(user.getEmail(), subject, htmlContent, false, true);
    }

    @Async
    public void sendCreationEmail(User user) {
        LOG.debug("Sending creation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title");
    }
}
