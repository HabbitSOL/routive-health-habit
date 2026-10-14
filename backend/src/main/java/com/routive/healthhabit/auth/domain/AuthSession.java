package com.routive.healthhabit.auth.domain;

import com.routive.healthhabit.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;

    protected AuthSession() {}
    public static AuthSession create(User user, Instant now, Instant expiresAt) {
        AuthSession session = new AuthSession();
        session.id = UUID.randomUUID();
        session.user = user;
        session.createdAt = now;
        session.expiresAt = expiresAt;
        return session;
    }
    public UUID getId() { return id; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
}
