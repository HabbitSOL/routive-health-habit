package com.routive.healthhabit.medication.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="personal_medications", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_key"}))
public class PersonalMedication {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="user_id",nullable=false,columnDefinition="BINARY(16)") private UUID userId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="drug_id",columnDefinition="BINARY(16)") private UUID drugId;
    @Column(name="display_name",nullable=false,length=255) private String displayName;
    @Column(name="custom_name",length=255) private String customName;
    @Column(name="medication_type",nullable=false,length=32) private String medicationType;
    @Column(length=1000) private String purpose;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date") private LocalDate endDate;
    @Column(length=2000) private String memo;
    @Column(nullable=false,length=32) private String status;
    @Column(name="request_key",nullable=false,length=36) private String requestKey;
    @Column(name="request_hash",nullable=false,length=64) private String requestHash;
    @Version @Column(nullable=false) private Long version;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Column(name="closed_at") private Instant closedAt;

    protected PersonalMedication() {}
    public PersonalMedication(UUID user, UUID drug, String name, String custom, String type,
            String purpose, LocalDate start, LocalDate end, String memo, String key, String hash) {
        id=UUID.randomUUID(); userId=user; drugId=drug; displayName=name; customName=custom;
        medicationType=type; this.purpose=purpose; startDate=start; endDate=end; this.memo=memo;
        requestKey=key; requestHash=hash; status="ACTIVE"; createdAt=Instant.now(); updatedAt=createdAt;
    }
    public void update(String custom, String purpose, LocalDate start, LocalDate end, String memo) {
        if (drugId==null) {customName=custom; displayName=custom;}
        this.purpose=purpose; startDate=start; endDate=end; this.memo=memo; updatedAt=Instant.now();
    }
    public void close(String target) {status=target; closedAt=Instant.now(); updatedAt=closedAt;}
    public void touch() {updatedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getDrugId(){return drugId;}
    public String getDisplayName(){return displayName;} public String getCustomName(){return customName;}
    public String getMedicationType(){return medicationType;} public String getPurpose(){return purpose;}
    public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;}
    public String getMemo(){return memo;} public String getStatus(){return status;}
    public String getRequestHash(){return requestHash;} public Long getVersion(){return version;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public Instant getClosedAt(){return closedAt;}
}
