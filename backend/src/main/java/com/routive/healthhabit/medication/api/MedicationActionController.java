package com.routive.healthhabit.medication.api;

import com.routive.healthhabit.common.api.ApiResponse;
import com.routive.healthhabit.medication.application.MedicationActionService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medication-occurrences/{id}/actions")
public class MedicationActionController {
    private final MedicationActionService service;
    public MedicationActionController(MedicationActionService service) { this.service = service; }
    @PostMapping
    ApiResponse<MedicationActionResponse> record(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody MedicationActionRequest request) {
        return ApiResponse.success(service.record(UUID.fromString(jwt.getSubject()), id, key, request));
    }
    @GetMapping
    ApiResponse<Map<String, Object>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(Map.of("items", service.list(UUID.fromString(jwt.getSubject()), id)));
    }
}
