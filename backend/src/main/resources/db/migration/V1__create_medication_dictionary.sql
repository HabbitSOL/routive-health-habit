CREATE TABLE dosage_form_definitions (
    code VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medication_products (
    id BINARY(16) NOT NULL,
    external_code VARCHAR(128) NULL,
    kind VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    ingredient_text TEXT NULL,
    manufacturer VARCHAR(255) NULL,
    dosage_form_code VARCHAR(128) NULL,
    image_url VARCHAR(1024) NULL,
    efficacy TEXT NULL,
    usage_instructions TEXT NULL,
    precautions TEXT NULL,
    side_effects TEXT NULL,
    storage_method TEXT NULL,
    color VARCHAR(128) NULL,
    shape VARCHAR(128) NULL,
    score_line VARCHAR(128) NULL,
    source_name VARCHAR(255) NOT NULL,
    source_updated_at DATETIME(6) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_medication_products_external_code (external_code),
    KEY ix_medication_products_name (normalized_name),
    KEY ix_medication_products_manufacturer (manufacturer),
    CONSTRAINT fk_medication_products_dosage_form
        FOREIGN KEY (dosage_form_code) REFERENCES dosage_form_definitions (code)
        ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medication_product_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(18,6) NULL,
    unit_code VARCHAR(32) NULL,
    display_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_medication_product_ingredient (product_id, name),
    CONSTRAINT fk_medication_product_ingredients_product
        FOREIGN KEY (product_id) REFERENCES medication_products (id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
