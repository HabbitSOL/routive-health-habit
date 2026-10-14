package com.routive.healthhabit.drug.api;

import com.routive.healthhabit.drug.domain.MedicationProduct;
import java.util.UUID;

public record DrugSearchItem(
        UUID drugId,
        String name,
        String manufacturer,
        String dosageForm,
        String imageUrl
) {

    public static DrugSearchItem from(MedicationProduct product) {
        String dosageFormName = product.getDosageForm() == null
                ? null
                : product.getDosageForm().getName();
        return new DrugSearchItem(
                product.getId(),
                product.getName(),
                product.getManufacturer(),
                dosageFormName,
                product.getImageUrl()
        );
    }
}
