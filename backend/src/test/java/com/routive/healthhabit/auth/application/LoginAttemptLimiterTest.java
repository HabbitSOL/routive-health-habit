package com.routive.healthhabit.auth.application;

import com.routive.healthhabit.common.error.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LoginAttemptLimiterTest {
    @Test void boundsAccountEvenAcrossAddresses() {
        var limiter = new LoginAttemptLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 10; i++) limiter.check("fake@example.invalid", "fake-address-" + i);
        assertThatThrownBy(() -> limiter.check("fake@example.invalid", "other-address"))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> limiter.check("other@example.invalid", "other-address")).doesNotThrowAnyException();
    }
    @Test void boundsAddressAcrossAccounts() {
        var limiter = new LoginAttemptLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 60; i++) limiter.check("fake-" + i, "same-address");
        assertThatThrownBy(() -> limiter.check("other", "same-address")).isInstanceOf(BusinessException.class);
    }
    @Test void windowExpires() {
        var base = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        var milliseconds = new java.util.concurrent.atomic.AtomicLong();
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return base.instant().plusMillis(milliseconds.get()); }
        };
        var limiter = new LoginAttemptLimiter(clock);
        for (int i = 0; i < 10; i++) limiter.check("fake", "address");
        milliseconds.set(60_000);
        assertThatCode(() -> limiter.check("fake", "address")).doesNotThrowAnyException();
    }
}
