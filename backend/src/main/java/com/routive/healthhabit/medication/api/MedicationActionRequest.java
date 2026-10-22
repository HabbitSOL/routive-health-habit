package com.routive.healthhabit.medication.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record MedicationActionRequest(
        @NotNull @Pattern(regexp = "TAKE|SKIP|NO_STOCK") String action,
        Instant actualAt,
        @DecimalMin("0.001") @DecimalMax("999999999.999") @Digits(integer = 9, fraction = 3) BigDecimal actualDoseAmount,
        @Pattern(regexp = "TABLET|CAPSULE|ML|MG|G|DROP|SACHET") String doseUnitCode,
        @Size(max = 1000) String memo) { }
