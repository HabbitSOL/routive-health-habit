package com.routive.healthhabit.medication.application;

import com.routive.healthhabit.common.error.BusinessException;
import com.routive.healthhabit.medication.domain.PersonalMedicationRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Bounded ID-keyset scan; each medication is processed in a separate service transaction. */
@Component
public class MedicationGenerationJob {
    public static final int BATCH_SIZE = 50;
    private final PersonalMedicationRepository medications;
    private final MedicationScheduleService schedules;
    private UUID cursor;

    public MedicationGenerationJob(PersonalMedicationRepository medications, MedicationScheduleService schedules) {
        this.medications = medications;
        this.schedules = schedules;
    }

    public synchronized Result runBatch() {
        // No surrounding transaction: one invalid schedule must not roll back another user's records.
        var candidates = medications.findGenerationCandidates(cursor, PageRequest.of(0, BATCH_SIZE));
        int created = 0, rejected = 0, failed = 0;
        for (var candidate : candidates) {
            try {
                created += schedules.generateToday(candidate.getUserId(), candidate.getId());
            } catch (BusinessException expected) {
                // Consent withdrawal, stop/deletion races or invalid local time. Do not log health data/IDs.
                rejected++;
            } catch (RuntimeException unexpected) {
                // Retry this item on the next scan. Never expose exception messages or SQL parameters.
                failed++;
            }
        }
        cursor = candidates.hasNext() ? candidates.getContent().getLast().getId() : null;
        return new Result(candidates.getNumberOfElements(), created, rejected, failed, !candidates.hasNext());
    }

    public record Result(int processed, int created, int rejected, int failed, boolean scanCompleted) {}
}
