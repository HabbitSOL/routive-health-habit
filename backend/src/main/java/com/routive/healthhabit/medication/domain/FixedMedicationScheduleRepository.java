package com.routive.healthhabit.medication.domain;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FixedMedicationScheduleRepository extends JpaRepository<FixedMedicationSchedule,UUID> {
    List<FixedMedicationSchedule> findAllByMedicationIdOrderByEffectiveFromAsc(UUID id);
    boolean existsByMedicationId(UUID id);
}
