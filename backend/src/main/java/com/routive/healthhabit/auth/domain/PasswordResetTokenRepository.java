package com.routive.healthhabit.auth.domain;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(byte[] tokenHash);
    @Query("select t.user.id from PasswordResetToken t where t.tokenHash = :hash")
    Optional<UUID> findUserIdByTokenHash(byte[] hash);
    List<PasswordResetToken> findAllByUserIdAndUsedAtIsNull(UUID userId);
    boolean existsByUserIdAndCreatedAtAfter(UUID userId, Instant since);
}
