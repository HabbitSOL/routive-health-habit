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
