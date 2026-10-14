package com.routive.healthhabit.drug.api;

import com.routive.healthhabit.drug.domain.MedicationProduct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DrugDetailResponse(
        UUID drugId,
        String name,
        String medicationType,
        String manufacturer,
        String efficacy,
        String usage,
        String precautions,
        String sideEffects,
        String storage,
        Appearance appearance,
        List<Ingredient> ingredients,
        String sourceName,
        Instant sourceUpdatedAt,
        String disclaimer
) {

    private static final String DISCLAIMER = "이 정보는 진단이나 처방을 대신하지 않습니다.";

    public static DrugDetailResponse from(MedicationProduct product) {
        String dosageFormName = product.getDosageForm() == null
                ? null
                : product.getDosageForm().getName();
        List<Ingredient> ingredients = product.getIngredients().stream()
                .map(value -> new Ingredient(value.getName(), value.getAmount(), value.getUnitCode()))
                .toList();

        return new DrugDetailResponse(
                product.getId(),
                product.getName(),
                product.getKind(),
                product.getManufacturer(),
                product.getEfficacy(),
                product.getUsageInstructions(),
                product.getPrecautions(),
                product.getSideEffects(),
                product.getStorageMethod(),
                new Appearance(dosageFormName, product.getColor(), product.getShape(), product.getScoreLine()),
                ingredients,
                product.getSourceName(),
                product.getSourceUpdatedAt(),
                DISCLAIMER
        );
    }

    public record Appearance(String dosageForm, String color, String shape, String scoreLine) {
    }

    public record Ingredient(String name, BigDecimal amount, String unitCode) {
    }
}
