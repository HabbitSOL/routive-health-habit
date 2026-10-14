package com.routive.healthhabit.drug.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "medication_product_ingredients")
public class MedicationProductIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private MedicationProduct product;

    @Column(nullable = false)
    private String name;

    @Column(precision = 18, scale = 6)
    private BigDecimal amount;

    @Column(name = "unit_code", length = 32)
    private String unitCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected MedicationProductIngredient() {
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getUnitCode() {
        return unitCode;
    }
}
