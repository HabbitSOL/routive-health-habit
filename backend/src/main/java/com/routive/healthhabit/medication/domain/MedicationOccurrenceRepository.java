package com.routive.healthhabit.medication.domain;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MedicationOccurrenceRepository extends JpaRepository<MedicationOccurrence,UUID> {
    boolean existsByScheduleIdAndScheduledDateAndSlotNo(UUID schedule,LocalDate date,int slot);
    List<MedicationOccurrence> findAllByUserIdAndScheduledDateOrderByScheduledAtAscIdAsc(UUID user,LocalDate date);
    List<MedicationOccurrence> findAllByMedicationIdAndScheduledAtGreaterThanEqual(UUID medication,Instant cutoff);
    void deleteAllByUserId(UUID user);
}
