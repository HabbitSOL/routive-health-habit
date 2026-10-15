package com.routive.healthhabit.user.api;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Pattern;
public record WithdrawRequest(@NotBlank @Pattern(regexp="DELETE", message="confirmation must be DELETE") String confirmation) {}
