package com.routive.healthhabit.auth.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    List<AuthSession> findAllByUserIdAndRevokedAtIsNull(UUID userId);
    boolean existsByIdAndUserIdAndUserStatusAndRevokedAtIsNullAndExpiresAtAfter(
            UUID id, UUID userId, String status, Instant now);
}
