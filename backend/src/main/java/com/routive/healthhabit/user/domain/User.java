package com.routive.healthhabit.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "email_normalized", nullable = false, unique = true, length = 320)
    private String emailNormalized;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false, length = 32)
    private String locale;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }

    private User(String email, String passwordHash, String status, Instant now) {
        this.id = UUID.randomUUID();
        this.email = email.trim();
        this.emailNormalized = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.status = status;
        this.timezone = "Asia/Seoul";
        this.locale = "ko-KR";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static User createLocal(String email, String passwordHash, Instant now) {
        return new User(email, passwordHash, "PENDING_EMAIL_VERIFICATION", now);
    }

    public static User createSocial(String email, Instant now) {
        return new User(email, null, "ACTIVE", now);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLocale() {
        return locale;
    }

    public void verifyEmail(Instant now) {
        if ("PENDING_EMAIL_VERIFICATION".equals(status)) {
            this.status = "ACTIVE";
            this.updatedAt = now;
        }
    }

    public void changePassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.updatedAt = now;
    }

    public void withdraw(Instant now) {
        this.status = "DELETED";
        this.deletedAt = now;
        this.updatedAt = now;
        this.email = "deleted+" + id + "@invalid.local";
        this.emailNormalized = this.email;
        this.passwordHash = null;
    }
}
