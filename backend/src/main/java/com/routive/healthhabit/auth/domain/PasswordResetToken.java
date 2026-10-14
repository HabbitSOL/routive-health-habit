package com.routive.healthhabit.auth.domain;

import com.routive.healthhabit.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "BINARY(32)") private byte[] tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "used_at") private Instant usedAt;
    protected PasswordResetToken() {}
    public static PasswordResetToken issue(User user, byte[] hash, Instant now, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken(); token.id = UUID.randomUUID(); token.user = user;
        token.tokenHash = hash.clone(); token.createdAt = now; token.expiresAt = expiresAt; return token;
    }
    public boolean isUsableAt(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
    public void use(Instant now) { usedAt = now; }
    public User getUser() { return user; }
}
