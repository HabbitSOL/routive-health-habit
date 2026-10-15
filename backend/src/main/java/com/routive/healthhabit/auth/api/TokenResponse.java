package com.routive.healthhabit.auth.api;

import com.routive.healthhabit.auth.application.TokenService;
import java.util.UUID;

public record TokenResponse(
        UUID userId,
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {

    public static TokenResponse of(UUID userId, TokenService.IssuedTokenPair pair) {
        return new TokenResponse(userId, pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    public static TokenResponse from(TokenService.RotatedTokenPair pair) {
        return new TokenResponse(
                pair.userId(), pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }
}
