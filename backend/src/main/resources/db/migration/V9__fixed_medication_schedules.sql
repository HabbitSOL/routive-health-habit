CREATE TABLE medication_schedules (
    id BINARY(16) NOT NULL PRIMARY KEY,
    medication_id BINARY(16) NOT NULL,
    effective_from DATE NOT NULL,
    effective_until DATE NULL,
    timezone VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_schedule_medication FOREIGN KEY (medication_id) REFERENCES personal_medications(id),
    CONSTRAINT uk_schedule_effective UNIQUE (medication_id, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medication_schedule_days (
    schedule_id BINARY(16) NOT NULL,
    day_of_week INT NOT NULL,
    PRIMARY KEY (schedule_id, day_of_week),
    CONSTRAINT fk_schedule_day FOREIGN KEY (schedule_id) REFERENCES medication_schedules(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medication_schedule_times (
    schedule_id BINARY(16) NOT NULL,
    slot_no INT NOT NULL,
    fixed_time TIME(6) NOT NULL,
    dose_amount DECIMAL(12,3) NOT NULL,
    dose_unit_code VARCHAR(16) NOT NULL,
    PRIMARY KEY (schedule_id, slot_no),
    CONSTRAINT fk_schedule_time FOREIGN KEY (schedule_id) REFERENCES medication_schedules(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medication_occurrences (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    medication_id BINARY(16) NOT NULL,
    schedule_id BINARY(16) NOT NULL,
    scheduled_date DATE NOT NULL,
    slot_no INT NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    dose_amount DECIMAL(12,3) NOT NULL,
    dose_unit_code VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_occurrence_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_occurrence_medication FOREIGN KEY (medication_id) REFERENCES personal_medications(id),
    CONSTRAINT fk_occurrence_schedule FOREIGN KEY (schedule_id) REFERENCES medication_schedules(id),
    CONSTRAINT uk_occurrence_slot UNIQUE (schedule_id, scheduled_date, slot_no),
    KEY ix_occurrence_user_date (user_id, scheduled_date, scheduled_at),
    KEY ix_occurrence_medication_time (medication_id, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
