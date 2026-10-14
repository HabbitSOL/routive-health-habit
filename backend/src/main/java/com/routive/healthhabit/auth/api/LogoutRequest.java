package com.routive.healthhabit.auth.api;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record LogoutRequest(@NotBlank @Size(max=512) String refreshToken) {}
