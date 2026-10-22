package com.routive.healthhabit.auth.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class ConsoleAccountMailSenderTest {
    @Test void neverLogsRecipientTokenOrLink(CapturedOutput output) {
        var sender = new ConsoleAccountMailSender(new MailDeliveryProperties("console", "",
                "https://example.invalid/private-verification", "https://example.invalid/private-reset"));
        sender.sendVerification("fake-recipient@example.invalid", "fake-verification-secret");
        sender.sendPasswordReset("fake-recipient@example.invalid", "fake-reset-secret");
        assertThat(output.getAll()).contains("delivery skipped")
                .doesNotContain("fake-recipient", "fake-verification-secret", "fake-reset-secret", "https://example.invalid");
    }
}
