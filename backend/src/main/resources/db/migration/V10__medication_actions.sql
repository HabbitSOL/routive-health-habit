CREATE TABLE medication_actions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    occurrence_id BINARY(16) NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    actual_at DATETIME(6) NULL,
    actual_dose_amount DECIMAL(12,3) NULL,
    dose_unit_code VARCHAR(16) NULL,
    memo VARCHAR(1000) NULL,
    recorded_at DATETIME(6) NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    CONSTRAINT fk_action_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_action_occurrence FOREIGN KEY (occurrence_id) REFERENCES medication_occurrences(id),
    CONSTRAINT uk_action_occurrence UNIQUE (occurrence_id),
    CONSTRAINT uk_action_request UNIQUE (user_id, request_key),
    CONSTRAINT ck_action_fields CHECK (
        (action_type = 'TAKE' AND actual_at IS NOT NULL AND actual_dose_amount IS NOT NULL
            AND actual_dose_amount > 0 AND dose_unit_code IS NOT NULL)
        OR (action_type IN ('SKIP', 'NO_STOCK') AND actual_at IS NULL
            AND actual_dose_amount IS NULL AND dose_unit_code IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
