package com.routive.healthhabit.auth.application;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "auth")
public record AccountSecurityProperties(long emailTokenSeconds, long passwordResetTokenSeconds,
        @org.springframework.boot.context.properties.bind.DefaultValue("60") long mailCooldownSeconds) {}
