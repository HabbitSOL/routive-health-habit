package com.routive.healthhabit.auth.domain;

import com.routive.healthhabit.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "social_accounts")
public class SocialAccount {
    public java.util.UUID getId() { return id; }
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 32)
    private String provider;
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;
    @Column(name = "provider_email", length = 320)
    private String providerEmail;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    protected SocialAccount() {}

    public static SocialAccount create(User user, String provider, String providerUserId, String email, Instant now) {
        SocialAccount account = new SocialAccount();
        account.id = UUID.randomUUID();
        account.user = user;
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.providerEmail = email;
        account.createdAt = now;
        account.lastLoginAt = now;
        return account;
    }

    public void markLogin(Instant now) { this.lastLoginAt = now; }
    public User getUser() { return user; }
}
