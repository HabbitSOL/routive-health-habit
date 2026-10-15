package com.routive.healthhabit.drug.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "drug-import", name = "run-on-startup", havingValue = "true")
public class DrugImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DrugImportRunner.class);
    private final DrugImportService service;
    public DrugImportRunner(DrugImportService service) { this.service = service; }
    @Override
    public void run(ApplicationArguments arguments) {
        DrugImportRun.Summary result = service.importCatalog();
        if (result.rejected() > 0) log.warn("MFDS drug import requires data review: {}", result);
        else log.info("MFDS drug import: {}", result);
        if ("FAILED".equals(result.status())) {
            throw new IllegalStateException("MFDS drug import failed: " + result.errorCode() + ", run " + result.runId());
        }
    }
}
