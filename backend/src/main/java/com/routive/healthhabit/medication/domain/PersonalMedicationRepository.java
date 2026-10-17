package com.routive.healthhabit.medication.domain;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PersonalMedicationRepository extends JpaRepository<PersonalMedication,UUID> {
    void deleteAllByUserId(UUID userId);
    Optional<PersonalMedication> findByIdAndUserId(UUID id,UUID userId);
    Optional<PersonalMedication> findByUserIdAndRequestKey(UUID userId,String requestKey);
    Page<PersonalMedication> findAllByUserId(UUID userId,Pageable pageable);
    Page<PersonalMedication> findAllByUserIdAndStatus(UUID userId,String status,Pageable pageable);
}
