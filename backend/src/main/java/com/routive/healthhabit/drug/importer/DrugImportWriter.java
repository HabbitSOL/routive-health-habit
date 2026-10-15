package com.routive.healthhabit.drug.importer;

import com.routive.healthhabit.drug.domain.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class DrugImportWriter {
    private final MedicationProductRepository products;
    private final DrugImportRunRepository runs;

    public DrugImportWriter(MedicationProductRepository products, DrugImportRunRepository runs) {
        this.products = products; this.runs = runs;
    }
    public UUID start(int page) { return runs.save(new DrugImportRun(page)).summary().runId(); }

    public void writePage(UUID id, int page, List<MfdsDrugRecord> rows) {
        DrugImportRun run = runs.findLockedById(id).orElseThrow();
        long created = 0, updated = 0, unchanged = 0, rejected = 0;
        for (MfdsDrugRecord row : rows) {
            MfdsDrugData data;
            try { data = MfdsDrugData.from(row); }
            catch (IllegalArgumentException exception) { rejected++; continue; }
            var existing = products.findByExternalCode(data.externalCode());
            if (existing.isEmpty()) {
                products.save(MedicationProduct.fromPublicData(data.toProductData())); created++;
            } else if (existing.get().applyPublicData(data.toProductData())) updated++;
            else unchanged++;
        }
        run.completePage(page, created, updated, unchanged, rejected);
    }

    public DrugImportRun.Summary finish(UUID id, String status, String errorCode) {
        DrugImportRun run = runs.findLockedById(id).orElseThrow();
        run.finish(status, errorCode);
        return run.summary();
    }
}
