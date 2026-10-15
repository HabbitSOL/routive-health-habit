package com.routive.healthhabit.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        long accessTokenSeconds,
        long refreshTokenSeconds
) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        if (accessTokenSeconds <= 0 || refreshTokenSeconds <= 0) {
            throw new IllegalArgumentException("Token expiration must be positive");
        }
    }
}
