package com.routive.healthhabit.auth.application;

import com.routive.healthhabit.auth.domain.*;
import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.user.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class TokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokens;
    private final AuthSessionRepository sessions;
    private final UserRepository users;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, RefreshTokenRepository refreshTokens,
            AuthSessionRepository sessions, UserRepository users) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.users = users;
    }

    public IssuedTokenPair issue(User user, Instant now) {
        User active = lockActive(user.getId());
        AuthSession session = sessions.save(AuthSession.create(active, now, now.plusSeconds(properties.refreshTokenSeconds())));
        String raw = createOpaqueToken();
        RefreshToken refresh = RefreshToken.issue(active, hash(raw), now, session.getExpiresAt());
        refresh.attachSession(session);
        refreshTokens.save(refresh);
        return new IssuedTokenPair(createAccessToken(active, session, now), raw, secondsRemaining(session, now));
    }

    public RotatedTokenPair rotate(String raw, Instant now) {
        byte[] hash = hash(raw);
        UUID userId = refreshTokens.findUserIdByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        users.findLockedById(userId).filter(user -> "ACTIVE".equals(user.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        // Lock the user before reading token state so concurrent mutations serialize.
        RefreshToken current = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        AuthSession session = current.getSession();
        if (!current.isUsableAt(now) || session == null || !session.isUsableAt(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String next = createOpaqueToken();
        RefreshToken replacement = RefreshToken.issue(current.getUser(), hash(next), now, session.getExpiresAt());
        replacement.attachSession(session);
        replacement = refreshTokens.save(replacement);
        current.rotateTo(replacement, now);
        return new RotatedTokenPair(userId, createAccessToken(current.getUser(), session, now),
                next, secondsRemaining(session, now));
    }

    public void revoke(String raw, UUID userId, UUID sessionId, Instant now) {
        lockActive(userId);
        RefreshToken token = refreshTokens.findByTokenHash(hash(raw))
                .filter(value -> value.getUser().getId().equals(userId))
                .filter(value -> value.getSession() != null && value.getSession().getId().equals(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        token.getSession().revoke(now);
        refreshTokens.findAllBySessionIdAndRevokedAtIsNull(sessionId).forEach(value -> value.revoke(now));
    }

    public void revokeAll(UUID userId, Instant now) {
        users.findLockedById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        sessions.findAllByUserIdAndRevokedAtIsNull(userId).forEach(session -> session.revoke(now));
        refreshTokens.findAllByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(now));
    }

    private User lockActive(UUID id) {
        return users.findLockedById(id).filter(user -> "ACTIVE".equals(user.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    }

    private long secondsRemaining(AuthSession session, Instant now) {
        return Math.min(properties.accessTokenSeconds(),
                java.time.Duration.between(now, session.getExpiresAt()).getSeconds());
    }

    private String createAccessToken(User user, AuthSession session, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer()).issuedAt(now)
                .expiresAt(now.plusSeconds(secondsRemaining(session, now))).subject(user.getId().toString())
                .id(UUID.randomUUID().toString()).claim("sid", session.getId().toString())
                .claim("token_type", "access").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }

    public String createOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedTokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}
    public record RotatedTokenPair(UUID userId, String accessToken, String refreshToken, long expiresInSeconds) {}
}
