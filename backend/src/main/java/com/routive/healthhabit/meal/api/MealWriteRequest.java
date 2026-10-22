package com.routive.healthhabit.meal.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public record MealWriteRequest(
        @NotNull @Pattern(regexp="BREAKFAST|LUNCH|DINNER|SNACK|LATE_NIGHT") String mealType,
        @NotNull Instant eatenAt,
        @NotNull @Pattern(regexp="COMPLETED") String status,
        @Size(max=1000) String memo,
        @Size(max=255) String imageFileKey,
        @NotNull @Size(min=1,max=30) List<@NotNull @Valid Item> items) {
    public record Item(UUID foodId, @NotBlank @Size(max=255) String foodName,
            @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal amount,
            @NotNull @Pattern(regexp="G|ML|SERVING|PIECE") String unitCode,
            @Valid Nutrients nutrients) {}
    public record Nutrients(
            @PositiveOrZero @Digits(integer=9,fraction=3) BigDecimal caloriesKcal,
            @PositiveOrZero @Digits(integer=9,fraction=3) BigDecimal carbohydrateG,
            @PositiveOrZero @Digits(integer=9,fraction=3) BigDecimal proteinG,
            @PositiveOrZero @Digits(integer=9,fraction=3) BigDecimal fatG,
            @PositiveOrZero @Digits(integer=9,fraction=3) BigDecimal sugarG,
            @PositiveOrZero @Digits(integer=9,fraction=3) BigDecimal caffeineMg) {}
}
