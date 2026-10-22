package com.routive.healthhabit.medication.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "medication_actions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "occurrence_id"),
        @UniqueConstraint(columnNames = {"user_id", "request_key"})})
public class MedicationAction {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name = "occurrence_id", nullable = false, columnDefinition = "BINARY(16)") private UUID occurrenceId;
    @Column(name = "action_type", nullable = false, length = 16) private String action;
    @Column(name = "actual_at") private Instant actualAt;
    @Column(name = "actual_dose_amount", precision = 12, scale = 3) private BigDecimal actualDoseAmount;
    @Column(name = "dose_unit_code", length = 16) private String doseUnitCode;
    @Column(length = 1000) private String memo;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;
    @Column(name = "request_key", nullable = false, length = 36) private String requestKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;

    protected MedicationAction() { }
    public MedicationAction(UUID userId, UUID occurrenceId, String action, Instant actualAt,
            BigDecimal actualDoseAmount, String doseUnitCode, String memo, Instant recordedAt,
            UUID requestKey, String requestHash) {
        id = UUID.randomUUID(); this.userId = userId; this.occurrenceId = occurrenceId; this.action = action;
        this.actualAt = actualAt; this.actualDoseAmount = actualDoseAmount; this.doseUnitCode = doseUnitCode;
        this.memo = memo; this.recordedAt = recordedAt; this.requestKey = requestKey.toString(); this.requestHash = requestHash;
    }
    public UUID getId() { return id; }
    public UUID getOccurrenceId() { return occurrenceId; }
    public String getAction() { return action; }
    public Instant getActualAt() { return actualAt; }
    public BigDecimal getActualDoseAmount() { return actualDoseAmount; }
    public String getDoseUnitCode() { return doseUnitCode; }
    public String getMemo() { return memo; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRequestHash() { return requestHash; }
    public String status() {
        return switch (action) { case "TAKE" -> "TAKEN"; case "SKIP" -> "SKIPPED"; case "NO_STOCK" -> "NO_STOCK";
            default -> throw new IllegalStateException("Unknown medication action"); };
    }
}
