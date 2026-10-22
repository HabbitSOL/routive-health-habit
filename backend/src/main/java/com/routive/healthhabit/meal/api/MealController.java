package com.routive.healthhabit.meal.api;

import com.routive.healthhabit.common.api.ApiResponse;
import com.routive.healthhabit.meal.application.MealService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class MealController {
    private final MealService service;
    public MealController(MealService service){this.service=service;}
    @PostMapping("/meals")
    ApiResponse<MealResponse> create(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody MealWriteRequest body) {
        return ApiResponse.success(service.create(user(jwt),key,body));
    }
    @GetMapping("/meals/{id}")
    ApiResponse<MealResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        return ApiResponse.success(service.get(user(jwt),id));
    }
    @GetMapping("/meals")
    ApiResponse<Map<String,Object>> list(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,
            @RequestParam LocalDate to,@RequestParam(defaultValue="COMPLETED") String status,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(service.list(user(jwt),from,to,status,page,size));
    }
    @DeleteMapping("/meals/{id}")
    ApiResponse<MealResponse> cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam long version) {
        return ApiResponse.success(service.cancel(user(jwt),id,version));
    }
    @GetMapping("/statistics/meals")
    ApiResponse<Map<String,Object>> statistics(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return ApiResponse.success(service.statistics(user(jwt),from,to));
    }
    private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
