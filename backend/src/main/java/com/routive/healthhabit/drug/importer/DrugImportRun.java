package com.routive.healthhabit.drug.importer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "drug_import_runs")
public class DrugImportRun {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(nullable = false) private String sourceName;
    @Column(nullable = false, length = 32) private String status;
    @Column(nullable = false) private Instant startedAt;
    private Instant finishedAt;
    @Column(nullable = false) private int lastCompletedPage;
    @Column(nullable = false) private long createdCount;
    @Column(nullable = false) private long updatedCount;
    @Column(nullable = false) private long unchangedCount;
    @Column(nullable = false) private long rejectedCount;
    @Column(length = 64) private String errorCode;

    protected DrugImportRun() {}
    public DrugImportRun(int startPage) {
        id = UUID.randomUUID(); sourceName = MfdsDrugData.SOURCE; status = "RUNNING";
        startedAt = Instant.now(); lastCompletedPage = startPage - 1;
    }

    void completePage(int page, long created, long updated, long unchanged, long rejected) {
        if (!"RUNNING".equals(status) || page != lastCompletedPage + 1) throw new IllegalStateException("Import page order conflict");
        lastCompletedPage = page; createdCount += created; updatedCount += updated;
        unchangedCount += unchanged; rejectedCount += rejected;
    }
    void finish(String status, String errorCode) {
        this.status = "COMPLETED".equals(status) && rejectedCount > 0 ? "COMPLETED_WITH_REJECTIONS" : status;
        this.errorCode = errorCode; finishedAt = Instant.now();
    }
    public Summary summary() {
        return new Summary(id, status, lastCompletedPage, lastCompletedPage + 1,
                createdCount, updatedCount, unchangedCount, rejectedCount, errorCode);
    }
    public record Summary(UUID runId, String status, int lastCompletedPage, int nextPage,
            long created, long updated, long unchanged, long rejected, String errorCode) {}
}
