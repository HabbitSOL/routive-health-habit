package com.routive.healthhabit.config;

import com.routive.healthhabit.auth.application.JwtProperties;
import com.routive.healthhabit.auth.mail.MailDeliveryProperties;
import java.net.URI;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Fail closed unless an explicitly selected isolated development/test profile is used. */
@Component
public class DeploymentSecurityGuard {
    public DeploymentSecurityGuard(Environment environment, JwtProperties jwt, MailDeliveryProperties mail) {
        Set<String> profiles = Set.of(environment.getActiveProfiles());
        boolean isolated = profiles.stream().anyMatch(Set.of("local", "test", "mariadb-test")::contains)
                && profiles.stream().allMatch(Set.of("local", "test", "mariadb-test")::contains);
        if (isolated) return;
        if (jwt.secret().startsWith("local-development-") || jwt.secret().startsWith("test-secret-")) {
            throw new IllegalStateException("Deployment requires a private JWT_SECRET");
        }
        if (!"smtp".equals(mail.mode())) {
            throw new IllegalStateException("Deployment requires SMTP mail delivery");
        }
        requireHttps(mail.verificationBaseUrl());
        requireHttps(mail.passwordResetBaseUrl());
        if (!environment.getProperty("spring.mail.properties.mail.smtp.starttls.enable", Boolean.class, false)
                || !environment.getProperty("spring.mail.properties.mail.smtp.starttls.required", Boolean.class, false)) {
            throw new IllegalStateException("Deployment requires mandatory SMTP STARTTLS");
        }
    }

    private static void requireHttps(String value) {
        try {
            URI uri = URI.create(value);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getFragment() == null && uri.getQuery() == null) return;
        } catch (RuntimeException ignored) { }
        throw new IllegalStateException("Deployment authentication links require an HTTPS origin without credentials, query or fragment");
    }
}
