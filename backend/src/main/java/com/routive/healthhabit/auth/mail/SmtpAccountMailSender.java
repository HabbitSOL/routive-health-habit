package com.routive.healthhabit.auth.mail;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage; import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
public class SmtpAccountMailSender implements AccountMailSender {
    private final JavaMailSender mailSender; private final MailDeliveryProperties properties;
    public SmtpAccountMailSender(JavaMailSender mailSender, MailDeliveryProperties properties) { this.mailSender = mailSender; this.properties = properties; }
    public void sendVerification(String email, String token) { send(email, "이메일 인증", properties.verificationBaseUrl() + "#token=" + token); }
    public void sendPasswordReset(String email, String token) { send(email, "비밀번호 재설정", properties.passwordResetBaseUrl() + "#token=" + token); }
    private void send(String email, String subject, String link) {
        SimpleMailMessage message = new SimpleMailMessage(); message.setFrom(properties.from()); message.setTo(email);
        message.setSubject("[루티브] " + subject); message.setText(subject + " 링크입니다.\n\n" + link + "\n\n본인이 요청하지 않았다면 무시하세요.");
        mailSender.send(message);
    }
}
