package com.routive.healthhabit.user.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(UUID id);

    @org.springframework.data.jpa.repository.Query("select u.id from User u where u.emailNormalized = :email")
    Optional<UUID> findIdByEmail(String email);

    boolean existsByEmailNormalized(String emailNormalized);

    Optional<User> findByEmailNormalizedAndStatus(String emailNormalized, String status);

    Optional<User> findByEmailNormalized(String emailNormalized);
}
