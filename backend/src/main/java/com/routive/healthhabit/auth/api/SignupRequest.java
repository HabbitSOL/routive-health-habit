package com.routive.healthhabit.auth.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SignupRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty @Size(max = 32) List<@jakarta.validation.constraints.NotNull @Valid ConsentRequest> consents
) {

    public record ConsentRequest(
            @NotBlank @Size(max = 64) String type,
            @NotBlank @Size(max = 128) String version,
            boolean agreed
    ) {
    }
}
