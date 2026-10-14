package com.routive.healthhabit.auth.application;

import com.routive.healthhabit.auth.domain.AuthSessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SessionTokenValidator implements OAuth2TokenValidator<Jwt> {
    private final AuthSessionRepository sessions;
    public SessionTokenValidator(AuthSessionRepository sessions) { this.sessions = sessions; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            if ("access".equals(jwt.getClaimAsString("token_type"))
                    && jwt.getExpiresAt() != null && jwt.getExpiresAt().isAfter(Instant.now())
                    && sessions.existsByIdAndUserIdAndUserStatusAndRevokedAtIsNullAndExpiresAtAfter(
                            UUID.fromString(jwt.getClaimAsString("sid")),
                            UUID.fromString(jwt.getSubject()), "ACTIVE", Instant.now())) {
                return OAuth2TokenValidatorResult.success();
            }
        } catch (IllegalArgumentException | NullPointerException exception) {
            // Missing or malformed claims are authentication failures.
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Inactive session", null));
    }
}
