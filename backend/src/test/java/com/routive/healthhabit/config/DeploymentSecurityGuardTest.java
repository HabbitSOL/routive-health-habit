package com.routive.healthhabit.config;

import com.routive.healthhabit.auth.application.JwtProperties;
import com.routive.healthhabit.auth.mail.MailDeliveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class DeploymentSecurityGuardTest {
    private final JwtProperties privateKey = new JwtProperties("test", "isolated-fixture-private-signing-key-123456789", 60, 600);
    private final MailDeliveryProperties smtp = new MailDeliveryProperties("smtp", "sender@example.invalid",
            "https://example.invalid/verify", "https://example.invalid/reset");
    private MockEnvironment secure() {
        return new MockEnvironment().withProperty("spring.mail.properties.mail.smtp.starttls.enable", "true")
                .withProperty("spring.mail.properties.mail.smtp.starttls.required", "true");
    }
    @Test void deploymentAcceptsExplicitSecureConfiguration() {
        assertThatCode(() -> new DeploymentSecurityGuard(secure(), privateKey, smtp)).doesNotThrowAnyException();
    }
    @Test void defaultAndProductionRejectDevelopmentKey() {
        var key = new JwtProperties("test", "local-development-fixture-not-a-real-key-12345", 60, 600);
        assertThatThrownBy(() -> new DeploymentSecurityGuard(secure(), key, smtp)).isInstanceOf(IllegalStateException.class);
        var mixed = secure(); mixed.setActiveProfiles("local", "prod");
        assertThatThrownBy(() -> new DeploymentSecurityGuard(mixed, key, smtp)).isInstanceOf(IllegalStateException.class);
    }
    @Test void deploymentRejectsConsolePlaintextLinksAndOptionalTls() {
        assertThatThrownBy(() -> new DeploymentSecurityGuard(secure(), privateKey,
                new MailDeliveryProperties("console", "", "", ""))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new DeploymentSecurityGuard(secure(), privateKey,
                new MailDeliveryProperties("smtp", "", "http://example.invalid/verify", smtp.passwordResetBaseUrl())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new DeploymentSecurityGuard(new MockEnvironment(), privateKey, smtp))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void isolatedLocalProfileAllowsConsole() {
        var local = new MockEnvironment(); local.setActiveProfiles("local");
        assertThatCode(() -> new DeploymentSecurityGuard(local, privateKey,
                new MailDeliveryProperties("console", "", "", ""))).doesNotThrowAnyException();
    }
}
