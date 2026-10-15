package com.routive.healthhabit.user.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    List<UserConsent> findAllByUserIdOrderByConsentedAtDesc(UUID userId);
}
