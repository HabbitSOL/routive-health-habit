package com.routive.healthhabit.medication.api;
import com.routive.healthhabit.medication.domain.MedicationOccurrence;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
public record MedicationOccurrenceResponse(UUID occurrenceId,UUID medicationId,UUID scheduleId,
    String medicationName,LocalDate scheduledDate,Instant scheduledAt,String timezone,
    String mealRelation,BigDecimal doseAmount,String doseUnitCode,String status){
    public static MedicationOccurrenceResponse from(MedicationOccurrence m){
        return new MedicationOccurrenceResponse(m.getId(),m.getMedicationId(),m.getScheduleId(),
            m.getMedicationName(),m.getScheduledDate(),m.getScheduledAt(),m.getTimezone(),"NONE",
            m.getDoseAmount(),m.getDoseUnitCode(),m.getStatus());
    }
}
