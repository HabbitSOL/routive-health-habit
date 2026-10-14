package com.routive.healthhabit.auth.domain;

import com.routive.healthhabit.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AuthSession session;

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "BINARY(32)")
    private byte[] tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private RefreshToken replacedBy;

    protected RefreshToken() {
    }

    private RefreshToken(User user, byte[] tokenHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.tokenHash = tokenHash.clone();
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(User user, byte[] tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(user, tokenHash, now, expiresAt);
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void rotateTo(RefreshToken replacement, Instant now) {
        this.revokedAt = now;
        this.replacedBy = replacement;
    }

    public void revoke(Instant now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public User getUser() {
        return user;
    }

    public void attachSession(AuthSession session) { this.session = session; }
    public AuthSession getSession() { return session; }
}
