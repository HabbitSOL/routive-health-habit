package com.routive.healthhabit.medication.api;
import com.routive.healthhabit.common.api.ApiResponse;
import com.routive.healthhabit.medication.application.PersonalMedicationService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medications")
public class MedicationController {
    private final PersonalMedicationService service;
    public MedicationController(PersonalMedicationService service){this.service=service;}
    @PostMapping
    ApiResponse<MedicationResponse> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody MedicationWriteRequest request){
        return ApiResponse.success(service.create(UUID.fromString(jwt.getSubject()),key,request));
    }
    @GetMapping
    ApiResponse<Map<String,Object>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        return ApiResponse.success(service.list(UUID.fromString(jwt.getSubject()),status,page,size));
    }
    @GetMapping("/{id}")
    ApiResponse<MedicationResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        return ApiResponse.success(service.get(UUID.fromString(jwt.getSubject()),id));
    }
    @PatchMapping("/{id}")
    ApiResponse<MedicationResponse> patch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
            @Valid @RequestBody MedicationWriteRequest request){
        return ApiResponse.success(service.patch(UUID.fromString(jwt.getSubject()),id,request));
    }
    @PostMapping("/{id}/stop")
    ApiResponse<MedicationResponse> stop(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        return ApiResponse.success(service.close(UUID.fromString(jwt.getSubject()),id,"STOPPED"));
    }
    @PostMapping("/{id}/complete")
    ApiResponse<MedicationResponse> complete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        return ApiResponse.success(service.close(UUID.fromString(jwt.getSubject()),id,"COMPLETED"));
    }
}
