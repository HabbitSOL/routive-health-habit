ALTER TABLE medication_products
    ADD COLUMN interactions TEXT NULL,
    ADD COLUMN record_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE drug_import_runs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    source_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    last_completed_page INT NOT NULL,
    created_count BIGINT NOT NULL,
    updated_count BIGINT NOT NULL,
    unchanged_count BIGINT NOT NULL,
    rejected_count BIGINT NOT NULL,
    error_code VARCHAR(64) NULL,
    KEY ix_drug_import_runs_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
