package com.routive.healthhabit.auth.domain;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(byte[] tokenHash);
    @Query("select t.user.id from EmailVerificationToken t where t.tokenHash = :hash")
    Optional<UUID> findUserIdByTokenHash(byte[] hash);
    List<EmailVerificationToken> findAllByUserIdAndUsedAtIsNull(UUID userId);
    boolean existsByUserIdAndCreatedAtAfter(UUID userId, Instant since);
}
