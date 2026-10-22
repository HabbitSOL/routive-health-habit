package com.routive.healthhabit.medication.domain;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface PersonalMedicationRepository extends JpaRepository<PersonalMedication,UUID> {
    void deleteAllByUserId(UUID userId);
    java.util.List<PersonalMedication> findAllByUserId(UUID userId);
    Optional<PersonalMedication> findByIdAndUserId(UUID id,UUID userId);
    Optional<PersonalMedication> findByUserIdAndRequestKey(UUID userId,String requestKey);
    Page<PersonalMedication> findAllByUserId(UUID userId,Pageable pageable);
    Page<PersonalMedication> findAllByUserIdAndStatus(UUID userId,String status,Pageable pageable);
    interface GenerationCandidate {
        UUID getId();
        UUID getUserId();
    }
    @Query("""
        select m.id as id, m.userId as userId from PersonalMedication m
        where m.status = 'ACTIVE' and (:after is null or m.id > :after)
        and exists (select s.id from FixedMedicationSchedule s where s.medicationId = m.id)
        and exists (select u.id from User u where u.id = m.userId and u.status = 'ACTIVE')
        order by m.id asc
        """)
    Slice<GenerationCandidate> findGenerationCandidates(@Param("after") UUID after, Pageable pageable);
}
