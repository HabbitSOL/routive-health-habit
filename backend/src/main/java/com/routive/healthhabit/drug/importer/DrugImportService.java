package com.routive.healthhabit.drug.importer;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

@Service
public class DrugImportService {
    private final MfdsImportProperties properties;
    private final MfdsDrugClient client;
    private final DrugImportWriter writer;
    private final ReentrantLock lock = new ReentrantLock();

    public DrugImportService(MfdsImportProperties properties, MfdsDrugClient client, DrugImportWriter writer) {
        this.properties = properties; this.client = client; this.writer = writer;
    }

    public DrugImportRun.Summary importCatalog() {
        if (!lock.tryLock()) throw new IllegalStateException("Drug import already running in this process");
        UUID id = null;
        try {
            id = writer.start(properties.startPage());
            for (int index = 0; index < properties.maxPages(); index++) {
                int pageNo = Math.addExact(properties.startPage(), index);
                MfdsDrugClient.Page page = client.fetch(pageNo);
                writer.writePage(id, pageNo, page.records());
                if ((long) pageNo * properties.pageSize() >= page.totalCount()) {
                    return writer.finish(id, "COMPLETED", null);
                }
            }
            return writer.finish(id, "PARTIAL", null);
        } catch (RuntimeException exception) {
            if (id == null) throw new IllegalStateException("Could not create drug import run");
            String code = exception instanceof DrugImportException error ? error.code().name() : "WRITE_FAILED";
            return writer.finish(id, "FAILED", code);
        } finally {
            lock.unlock();
        }
    }
}
