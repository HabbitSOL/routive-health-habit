package com.routive.healthhabit.auth.api;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.List;
import com.routive.healthhabit.auth.api.SignupRequest.ConsentRequest;
public record SocialLoginRequest(@NotBlank @Size(max=32) String provider, @NotBlank @Size(max=4096) String token,
        @Size(max=32) List<@NotNull @Valid ConsentRequest> consents) {}
