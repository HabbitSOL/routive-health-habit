package com.routive.healthhabit.drug.api;

import com.routive.healthhabit.common.api.ApiResponse;
import com.routive.healthhabit.common.api.PageResponse;
import com.routive.healthhabit.drug.application.DrugDictionaryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/drugs")
public class DrugDictionaryController {

    private final DrugDictionaryService drugDictionaryService;

    public DrugDictionaryController(DrugDictionaryService drugDictionaryService) {
        this.drugDictionaryService = drugDictionaryService;
    }

    @GetMapping
    ApiResponse<PageResponse<DrugSearchItem>> search(
            @RequestParam @NotBlank @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(drugDictionaryService.search(keyword, page, size));
    }

    @GetMapping("/{drugId}")
    ApiResponse<DrugDetailResponse> get(@PathVariable UUID drugId) {
        return ApiResponse.success(drugDictionaryService.get(drugId));
    }
}
