package com.routive.healthhabit.auth.application;

import com.routive.healthhabit.common.error.BusinessException;
import com.routive.healthhabit.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Per-process protection, not a replacement for a shared gateway limit in a cluster. */
@Component
public class LoginAttemptLimiter {
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    public LoginAttemptLimiter() { this(Clock.systemUTC()); }
    LoginAttemptLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(String normalizedEmail, String remoteAddress) {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().started >= 60_000);
        String account = "account:" + digest(normalizedEmail);
        // Deliberately ignore untrusted X-Forwarded-For headers.
        String address = "address:" + digest(remoteAddress);
        if (windows.size() >= 10_000) throw limited();
        checkWindow("global", 300, now);
        checkWindow(address, 60, now);
        checkWindow(account, 10, now);
    }

    private void checkWindow(String key, int limit, long now) {
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        if (window.count >= limit) throw limited();
        window.count++;
    }
    private static BusinessException limited() { return new BusinessException(ErrorCode.TOO_MANY_REQUESTS); }
    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static final class Window {
        final long started;
        int count;
        Window(long started) { this.started = started; }
    }
}
