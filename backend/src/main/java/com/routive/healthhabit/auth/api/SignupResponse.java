package com.routive.healthhabit.auth.api;
import java.util.UUID;
public record SignupResponse(UUID userId, String status, boolean verificationRequired) {}
