package com.routive.healthhabit.auth.mail;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "app.mail")
public record MailDeliveryProperties(String mode, String from, String verificationBaseUrl, String passwordResetBaseUrl) {}
