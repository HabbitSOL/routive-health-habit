package com.routive.healthhabit.user.api;

import jakarta.validation.constraints.*;

public record ConsentChangeRequest(
        @NotNull @Pattern(regexp = "TERMS_OF_SERVICE|PRIVACY|SENSITIVE_HEALTH_DATA|MARKETING") String type,
        @NotBlank @Size(max = 128) String version,
        @NotNull Boolean agreed
) {}
