package com.routive.healthhabit.medication.api;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

// Null means unchanged on PATCH; clearFields explicitly removes optional values.
public record MedicationWriteRequest(
        UUID drugId, @Size(max=255) String customName,
        @Pattern(regexp="PRESCRIPTION|OTC|SUPPLEMENT") String medicationType,
        @Size(max=1000) String purpose, LocalDate startDate, LocalDate endDate,
        @Size(max=2000) String memo, @PositiveOrZero Long version,
        Set<@Pattern(regexp="purpose|endDate|memo") String> clearFields,
        Object schedule) {}
