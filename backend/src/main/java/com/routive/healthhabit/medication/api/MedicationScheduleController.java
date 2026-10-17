package com.routive.healthhabit.medication.api;
import com.routive.healthhabit.common.api.ApiResponse;
import com.routive.healthhabit.medication.application.MedicationScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class MedicationScheduleController {
    private final MedicationScheduleService service;
    public MedicationScheduleController(MedicationScheduleService s){service=s;}
    @PostMapping("/medications/{id}/schedules")
    ApiResponse<Map<String,Object>> save(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
        @Valid @RequestBody FixedScheduleRequest request){
        return ApiResponse.success(service.save(UUID.fromString(jwt.getSubject()),id,request));
    }
    @GetMapping("/medications/{id}/schedules")
    ApiResponse<List<Map<String,Object>>> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        return ApiResponse.success(service.list(UUID.fromString(jwt.getSubject()),id));
    }
    @PostMapping("/medications/{id}/occurrences")
    ApiResponse<Map<String,Object>> generate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
        @RequestParam LocalDate date){
        return ApiResponse.success(service.generate(UUID.fromString(jwt.getSubject()),id,date));
    }
    @GetMapping("/medication-occurrences")
    ApiResponse<Map<String,Object>> occurrences(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate date){
        return ApiResponse.success(service.occurrences(UUID.fromString(jwt.getSubject()),date));
    }
}
