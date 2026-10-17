package com.routive.healthhabit.medication.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record FixedScheduleRequest(
    @NotNull @PositiveOrZero Long medicationVersion,
    @NotNull LocalDate effectiveFrom,
    @NotBlank @Size(max=64) String timezone,
    @NotNull @Size(min=1,max=7) List<@NotNull @Min(1) @Max(7) Integer> daysOfWeek,
    @NotNull @Size(min=1,max=8) List<@NotNull @Valid Time> times) {
    public record Time(@NotNull @Min(1) @Max(8) Integer sequence,
        @NotNull @Pattern(regexp="([01][0-9]|2[0-3]):[0-5][0-9]") String fixedTime,
        @NotNull @DecimalMin("0.001") @DecimalMax("999999999.999") @Digits(integer=9,fraction=3) BigDecimal doseAmount,
        @NotNull @Pattern(regexp="TABLET|CAPSULE|ML|MG|G|DROP|SACHET") String doseUnitCode,
        @Pattern(regexp="NONE") String mealRelation,
        String mealType, Integer offsetMinutes) {}
}
