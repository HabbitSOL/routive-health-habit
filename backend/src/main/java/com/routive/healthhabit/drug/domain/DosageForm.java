package com.routive.healthhabit.drug.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dosage_form_definitions")
public class DosageForm {

    @Id
    @Column(length = 128)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    protected DosageForm() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
