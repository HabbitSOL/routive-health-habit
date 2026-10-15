package com.routive.healthhabit.user.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    private String nickname;
    private LocalDate birthDate;
    @Column(length = 64) private String sex;
    @Column(precision = 6, scale = 2) private BigDecimal heightCm;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    private Instant onboardingCompletedAt;

    protected UserProfile() {}
    public UserProfile(User user, Instant now) {
        this.user = user; this.createdAt = now; this.updatedAt = now;
    }
    public String getNickname() { return nickname; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getSex() { return sex; }
    public BigDecimal getHeightCm() { return heightCm; }
    public Instant getOnboardingCompletedAt() { return onboardingCompletedAt; }
    public void update(String nickname, LocalDate birthDate, String sex, BigDecimal heightCm, Instant now) {
        this.nickname = nickname; this.birthDate = birthDate; this.sex = sex;
        this.heightCm = heightCm; this.updatedAt = now;
    }
    public void completeOnboarding(Instant now) {
        if (onboardingCompletedAt == null) {
            onboardingCompletedAt = now.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            updatedAt = onboardingCompletedAt;
        }
    }
}
