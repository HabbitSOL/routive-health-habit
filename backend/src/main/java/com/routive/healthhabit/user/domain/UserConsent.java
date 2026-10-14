package com.routive.healthhabit.user.domain;

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
@Table(name = "user_consents")
public class UserConsent {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "consent_type", nullable = false, length = 64)
    private String consentType;

    @Column(nullable = false, length = 128)
    private String version;

    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected UserConsent() {
    }

    private UserConsent(User user, String consentType, String version, Instant now) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.consentType = consentType;
        this.version = version;
        this.consentedAt = now;
    }

    public static UserConsent agreed(User user, String type, String version, Instant now) {
        return new UserConsent(user, type, version, now);
    }

    public String getConsentType() {
        return consentType;
    }

    public String getVersion() {
        return version;
    }

    public Instant getConsentedAt() {
        return consentedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void withdraw(Instant now) {
        if (withdrawnAt == null) withdrawnAt = now;
    }
}
