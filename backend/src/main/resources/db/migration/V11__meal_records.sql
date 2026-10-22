CREATE TABLE meals (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    meal_type VARCHAR(16) NOT NULL,
    eaten_at DATETIME(6) NOT NULL,
    meal_date DATE NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    memo VARCHAR(1000) NULL,
    recorded_at DATETIME(6) NOT NULL,
    cancelled_at DATETIME(6) NULL,
    request_key VARCHAR(36) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_meal_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_meal_request UNIQUE (user_id, request_key),
    CONSTRAINT ck_meal_type CHECK (meal_type IN ('BREAKFAST','LUNCH','DINNER','SNACK','LATE_NIGHT')),
    CONSTRAINT ck_meal_state CHECK ((status='COMPLETED' AND cancelled_at IS NULL) OR (status='CANCELLED' AND cancelled_at IS NOT NULL)),
    KEY ix_meal_user_date_status (user_id, meal_date, status, eaten_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE meal_items (
    id BINARY(16) NOT NULL PRIMARY KEY,
    meal_id BINARY(16) NOT NULL,
    item_no INT NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,3) NOT NULL,
    unit_code VARCHAR(16) NOT NULL,
    calories_kcal DECIMAL(12,3) NULL,
    carbohydrate_g DECIMAL(12,3) NULL,
    protein_g DECIMAL(12,3) NULL,
    fat_g DECIMAL(12,3) NULL,
    sugar_g DECIMAL(12,3) NULL,
    caffeine_mg DECIMAL(12,3) NULL,
    CONSTRAINT fk_meal_item_meal FOREIGN KEY (meal_id) REFERENCES meals(id),
    CONSTRAINT uk_meal_item_no UNIQUE (meal_id, item_no),
    CONSTRAINT ck_meal_item_quantity CHECK (amount>0 AND item_no>=0),
    CONSTRAINT ck_meal_item_unit CHECK (unit_code IN ('G','ML','SERVING','PIECE')),
    CONSTRAINT ck_meal_nutrients CHECK (
        (calories_kcal IS NULL OR calories_kcal>=0) AND (carbohydrate_g IS NULL OR carbohydrate_g>=0)
        AND (protein_g IS NULL OR protein_g>=0) AND (fat_g IS NULL OR fat_g>=0)
        AND (sugar_g IS NULL OR sugar_g>=0) AND (caffeine_mg IS NULL OR caffeine_mg>=0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
