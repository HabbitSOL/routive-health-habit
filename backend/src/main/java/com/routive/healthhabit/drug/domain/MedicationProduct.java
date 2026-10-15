package com.routive.healthhabit.drug.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "medication_products")
public class MedicationProduct {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @jakarta.persistence.Version
    @Column(name = "record_version", nullable = false)
    private Long recordVersion;

    @Column(name = "external_code", length = 128)
    private String externalCode;

    @Column(nullable = false, length = 64)
    private String kind;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(name = "ingredient_text", columnDefinition = "TEXT")
    private String ingredientText;

    private String manufacturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dosage_form_code")
    private DosageForm dosageForm;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String efficacy;

    @Column(name = "usage_instructions", columnDefinition = "TEXT")
    private String usageInstructions;

    @Column(columnDefinition = "TEXT")
    private String precautions;

    @Column(columnDefinition = "TEXT")
    private String interactions;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Column(name = "storage_method", columnDefinition = "TEXT")
    private String storageMethod;

    @Column(length = 128)
    private String color;

    @Column(length = 128)
    private String shape;

    @Column(name = "score_line", length = 128)
    private String scoreLine;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    private List<MedicationProductIngredient> ingredients = new ArrayList<>();

    protected MedicationProduct() {
    }

    public static MedicationProduct fromPublicData(MedicationProductData data) {
        MedicationProduct product = new MedicationProduct();
        product.id = UUID.randomUUID();
        product.active = true;
        product.applyPublicData(data);
        return product;
    }

    public boolean applyPublicData(MedicationProductData data) {
        if (externalCode != null && (!externalCode.equals(data.externalCode())
                || !sourceName.equals(data.sourceName()))) {
            throw new IllegalStateException("Public drug source conflict");
        }
        if (sourceUpdatedAt != null && (data.sourceUpdatedAt() == null
                || data.sourceUpdatedAt().isBefore(sourceUpdatedAt))) return false;
        if (java.util.Objects.equals(externalCode, data.externalCode())
                && java.util.Objects.equals(name, data.name())
                && java.util.Objects.equals(kind, data.kind())
                && java.util.Objects.equals(manufacturer, data.manufacturer())
                && java.util.Objects.equals(efficacy, data.efficacy())
                && java.util.Objects.equals(usageInstructions, data.usage())
                && java.util.Objects.equals(precautions, data.precautions())
                && java.util.Objects.equals(interactions, data.interactions())
                && java.util.Objects.equals(sideEffects, data.sideEffects())
                && java.util.Objects.equals(storageMethod, data.storage())
                && java.util.Objects.equals(imageUrl, data.imageUrl())
                && java.util.Objects.equals(sourceUpdatedAt, data.sourceUpdatedAt())) return false;
        externalCode = data.externalCode(); name = data.name();
        normalizedName = data.name().toLowerCase(java.util.Locale.ROOT); kind = data.kind();
        manufacturer = data.manufacturer(); efficacy = data.efficacy(); usageInstructions = data.usage();
        precautions = data.precautions(); interactions = data.interactions(); sideEffects = data.sideEffects();
        storageMethod = data.storage(); imageUrl = data.imageUrl(); sourceName = data.sourceName();
        sourceUpdatedAt = data.sourceUpdatedAt();
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getIngredientText() {
        return ingredientText;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public DosageForm getDosageForm() {
        return dosageForm;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getEfficacy() {
        return efficacy;
    }

    public String getUsageInstructions() {
        return usageInstructions;
    }

    public String getPrecautions() {
        return precautions;
    }

    public String getInteractions() { return interactions; }

    public String getSideEffects() {
        return sideEffects;
    }

    public String getStorageMethod() {
        return storageMethod;
    }

    public String getColor() {
        return color;
    }

    public String getShape() {
        return shape;
    }

    public String getScoreLine() {
        return scoreLine;
    }

    public String getSourceName() {
        return sourceName;
    }

    public Instant getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public List<MedicationProductIngredient> getIngredients() {
        return List.copyOf(ingredients);
    }
}
