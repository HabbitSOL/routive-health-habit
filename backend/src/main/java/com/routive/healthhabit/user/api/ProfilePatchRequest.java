package com.routive.healthhabit.user.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record ProfilePatchRequest(
        @Size(min = 1, max = 30) @Pattern(regexp = ".*\\S.*") String nickname,
        @PastOrPresent LocalDate birthDate,
        @Pattern(regexp = "MALE|FEMALE|OTHER|UNSPECIFIED") String sex,
        @DecimalMin("1.00") @DecimalMax("300.00") @Digits(integer = 3, fraction = 2) BigDecimal heightCm,
        @Size(max = 3) Set<@NotNull @Pattern(regexp = "birthDate|sex|heightCm") String> clearFields
) {}
