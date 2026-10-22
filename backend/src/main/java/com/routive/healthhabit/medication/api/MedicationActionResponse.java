package com.routive.healthhabit.medication.api;

import com.routive.healthhabit.medication.domain.MedicationAction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MedicationActionResponse(UUID actionId, UUID occurrenceId, String action, String status,
        Instant actualAt, BigDecimal actualDoseAmount, String doseUnitCode, String memo, Instant recordedAt) {
    public static MedicationActionResponse from(MedicationAction value) {
        return new MedicationActionResponse(value.getId(), value.getOccurrenceId(), value.getAction(), value.status(),
                value.getActualAt(), value.getActualDoseAmount(), value.getDoseUnitCode(), value.getMemo(), value.getRecordedAt());
    }
}
