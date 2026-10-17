package com.routive.healthhabit.medication.api;
import com.routive.healthhabit.medication.domain.PersonalMedication;
import java.time.*;
import java.util.UUID;
public record MedicationResponse(UUID medicationId, UUID drugId, String name, String customName,
        String medicationType, String purpose, LocalDate startDate, LocalDate endDate,
        String memo, String status, Long version, Instant createdAt, Instant updatedAt, Instant closedAt) {
    public static MedicationResponse from(PersonalMedication m) {
        return new MedicationResponse(m.getId(),m.getDrugId(),m.getDisplayName(),m.getCustomName(),
            m.getMedicationType(),m.getPurpose(),m.getStartDate(),m.getEndDate(),m.getMemo(),
            m.getStatus(),m.getVersion(),m.getCreatedAt(),m.getUpdatedAt(),m.getClosedAt());
    }
}
