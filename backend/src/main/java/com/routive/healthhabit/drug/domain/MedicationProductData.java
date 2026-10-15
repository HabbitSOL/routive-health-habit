package com.routive.healthhabit.drug.domain;

import java.time.Instant;

public record MedicationProductData(String externalCode, String name, String kind, String manufacturer,
        String efficacy, String usage, String precautions, String interactions, String sideEffects,
        String storage, String imageUrl, String sourceName, Instant sourceUpdatedAt) {}
