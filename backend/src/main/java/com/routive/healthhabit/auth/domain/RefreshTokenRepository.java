package com.routive.healthhabit.auth.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @org.springframework.data.jpa.repository.Query("select t.user.id from RefreshToken t where t.tokenHash = :hash")
    Optional<UUID> findUserIdByTokenHash(byte[] hash);

    List<RefreshToken> findAllBySessionIdAndRevokedAtIsNull(UUID sessionId);

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(byte[] tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
