package com.routive.healthhabit.medication.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationActionRepository extends JpaRepository<MedicationAction, UUID> {
    Optional<MedicationAction> findByUserIdAndRequestKey(UUID userId, String key);
    Optional<MedicationAction> findByOccurrenceIdAndUserId(UUID occurrenceId, UUID userId);
    void deleteAllByUserId(UUID userId);
}
