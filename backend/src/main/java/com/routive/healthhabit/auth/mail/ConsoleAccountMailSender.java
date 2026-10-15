package com.routive.healthhabit.auth.mail;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "console", matchIfMissing = true)
public class ConsoleAccountMailSender implements AccountMailSender {
    private static final Logger log = LoggerFactory.getLogger(ConsoleAccountMailSender.class);
    private final MailDeliveryProperties properties;
    public ConsoleAccountMailSender(MailDeliveryProperties properties) { this.properties = properties; }
    public void sendVerification(String email, String token) {
        log.info("LOCAL ONLY email verification for {}: {}#token={}", email, properties.verificationBaseUrl(), token);
    }
    public void sendPasswordReset(String email, String token) {
        log.info("LOCAL ONLY password reset for {}: {}#token={}", email, properties.passwordResetBaseUrl(), token);
    }
}
