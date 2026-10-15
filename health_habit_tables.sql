CREATE TABLE `users` (
  `id` BINARY(16) NOT NULL,
  `email` VARCHAR(320) NOT NULL,
  `email_normalized` VARCHAR(320) NOT NULL,
  `password_hash` VARCHAR(255),
  `status` VARCHAR(64) NOT NULL,
  `timezone` VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul',
  `locale` VARCHAR(32) NOT NULL DEFAULT 'ko-KR',
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `deleted_at` DATETIME(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_1` (`email_normalized`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `user_profiles` (
  `user_id` BINARY(16) NOT NULL,
  `nickname` VARCHAR(255),
  `birth_date` DATE,
  `sex` VARCHAR(255),
  `height_cm` DECIMAL(18,6),
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `user_consents` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `consent_type` VARCHAR(64) NOT NULL,
  `version` VARCHAR(128) NOT NULL,
  `consented_at` DATETIME(6) NOT NULL,
  `withdrawn_at` DATETIME(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_consents_1` (`user_id`, `consent_type`, `version`),
  CONSTRAINT `fk_user_consents_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habit_categories` (
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `health_stat_enabled` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `unit_definitions` (
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `symbol` VARCHAR(255) NOT NULL,
  `dimension_code` VARCHAR(128) NOT NULL,
  `canonical_factor` DECIMAL(18,6) NOT NULL,
  `decimal_scale` INT NOT NULL DEFAULT 2,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`),
  CONSTRAINT `ck_unit_definitions_1` CHECK (`canonical_factor` > 0),
  CONSTRAINT `ck_unit_definitions_2` CHECK (`decimal_scale` BETWEEN 0 AND 12)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `nutrient_definitions` (
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `canonical_unit_code` VARCHAR(128) NOT NULL,
  `nutrient_group` VARCHAR(128) NOT NULL,
  `parent_code` VARCHAR(128),
  `included_in_total` TINYINT(1) NOT NULL DEFAULT 1,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`),
  CONSTRAINT `fk_nutrient_definitions_canonical_unit_code` FOREIGN KEY (`canonical_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_nutrient_definitions_parent_code` FOREIGN KEY (`parent_code`) REFERENCES `nutrient_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `allergens` (
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255),
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `exercise_categories` (
  `code` VARCHAR(128) NOT NULL,
  `parent_code` VARCHAR(128),
  `name` VARCHAR(255) NOT NULL,
  `level` INT NOT NULL,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`),
  CONSTRAINT `fk_exercise_categories_parent_code` FOREIGN KEY (`parent_code`) REFERENCES `exercise_categories` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_exercise_categories_1` CHECK (`level` BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `dosage_form_definitions` (
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_ingredients` (
  `id` BINARY(16) NOT NULL,
  `external_code` VARCHAR(128),
  `name` VARCHAR(255) NOT NULL,
  `normalized_name` VARCHAR(255) NOT NULL,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_ingredients_1` (`external_code`),
  UNIQUE KEY `uq_medication_ingredients_2` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `observation_types` (
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `value_kind` VARCHAR(255) NOT NULL,
  `canonical_unit_code` VARCHAR(128),
  `min_value` DECIMAL(18,6),
  `max_value` DECIMAL(18,6),
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`),
  CONSTRAINT `fk_observation_types_canonical_unit_code` FOREIGN KEY (`canonical_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_observation_types_1` CHECK (`min_value` IS NULL OR `max_value` IS NULL OR `min_value` <= `max_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `observation_type_units` (
  `observation_type_code` VARCHAR(128) NOT NULL,
  `unit_code` VARCHAR(128) NOT NULL,
  `is_default` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`observation_type_code`, `unit_code`),
  CONSTRAINT `fk_observation_type_units_observation_type_code` FOREIGN KEY (`observation_type_code`) REFERENCES `observation_types` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_observation_type_units_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `healthcare_institutions` (
  `id` BINARY(16) NOT NULL,
  `external_code` VARCHAR(128),
  `name` VARCHAR(255) NOT NULL,
  `status` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habits` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `category_code` VARCHAR(128) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `priority` SMALLINT,
  `status` VARCHAR(64) NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `deleted_at` DATETIME(6),
  PRIMARY KEY (`id`),
  KEY `ix_habits_1` (`user_id`, `status`),
  CONSTRAINT `fk_habits_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_habits_category_code` FOREIGN KEY (`category_code`) REFERENCES `habit_categories` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habit_schedules` (
  `id` BINARY(16) NOT NULL,
  `habit_id` BINARY(16) NOT NULL,
  `schedule_type` VARCHAR(64) NOT NULL,
  `interval_value` INT,
  `timezone` VARCHAR(64) NOT NULL,
  `advanced_rule_json` JSON,
  `effective_from` DATE NOT NULL,
  `effective_to` DATE,
  `version` INT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_schedules_1` (`habit_id`, `version`),
  CONSTRAINT `fk_habit_schedules_habit_id` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_habit_schedules_1` CHECK (`interval_value` IS NULL OR `interval_value` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habit_schedule_days` (
  `schedule_id` BINARY(16) NOT NULL,
  `weekday` INT NOT NULL,
  PRIMARY KEY (`schedule_id`, `weekday`),
  CONSTRAINT `fk_habit_schedule_days_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_habit_schedule_days_1` CHECK (`weekday` BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habit_schedule_times` (
  `id` BINARY(16) NOT NULL,
  `schedule_id` BINARY(16) NOT NULL,
  `sequence_no` INT NOT NULL,
  `local_time` TIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_schedule_times_1` (`schedule_id`, `sequence_no`),
  CONSTRAINT `fk_habit_schedule_times_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_habit_schedule_times_1` CHECK (`sequence_no` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habit_schedule_exceptions` (
  `id` BINARY(16) NOT NULL,
  `schedule_id` BINARY(16) NOT NULL,
  `exception_date` DATE NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `replacement_at` DATETIME(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_schedule_exceptions_1` (`schedule_id`, `exception_date`),
  CONSTRAINT `fk_habit_schedule_exceptions_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habit_occurrences` (
  `id` BINARY(16) NOT NULL,
  `habit_id` BINARY(16) NOT NULL,
  `schedule_id` BINARY(16) NOT NULL,
  `scheduled_at` DATETIME(6) NOT NULL,
  `due_at` DATETIME(6),
  `current_status` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_occurrences_1` (`schedule_id`, `scheduled_at`),
  KEY `ix_habit_occurrences_1` (`habit_id`, `scheduled_at`),
  CONSTRAINT `fk_habit_occurrences_habit_id` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_habit_occurrences_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `habit_action_events` (
  `id` BINARY(16) NOT NULL,
  `occurrence_id` BINARY(16) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `actual_at` DATETIME(6),
  `recorded_at` DATETIME(6) NOT NULL,
  `source_type` VARCHAR(64) NOT NULL,
  `note` TEXT,
  `idempotency_key` VARCHAR(255),
  `previous_event_id` BINARY(16),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_action_events_1` (`idempotency_key`),
  KEY `ix_habit_action_events_1` (`occurrence_id`, `recorded_at`),
  CONSTRAINT `fk_habit_action_events_occurrence_id` FOREIGN KEY (`occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_habit_action_events_previous_event_id` FOREIGN KEY (`previous_event_id`) REFERENCES `habit_action_events` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `exercise_definitions` (
  `id` BINARY(16) NOT NULL,
  `category_code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `measurement_type` VARCHAR(64) NOT NULL,
  `met_value` DECIMAL(18,6),
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_exercise_definitions_category_code` FOREIGN KEY (`category_code`) REFERENCES `exercise_categories` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `exercise_sessions` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `habit_occurrence_id` BINARY(16),
  `started_at` DATETIME(6) NOT NULL,
  `ended_at` DATETIME(6),
  `total_kcal` DECIMAL(18,6),
  `calculation_method` VARCHAR(255),
  `weight_snapshot_kg` DECIMAL(18,6),
  `source_type` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_exercise_sessions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_exercise_sessions_habit_occurrence_id` FOREIGN KEY (`habit_occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `exercise_session_items` (
  `id` BINARY(16) NOT NULL,
  `session_id` BINARY(16) NOT NULL,
  `exercise_id` BINARY(16) NOT NULL,
  `order_no` INT NOT NULL,
  `duration_sec` INT,
  `distance_m` DECIMAL(18,6),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_exercise_session_items_session_id` FOREIGN KEY (`session_id`) REFERENCES `exercise_sessions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_exercise_session_items_exercise_id` FOREIGN KEY (`exercise_id`) REFERENCES `exercise_definitions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `exercise_sets` (
  `id` BINARY(16) NOT NULL,
  `session_item_id` BINARY(16) NOT NULL,
  `set_no` INT NOT NULL,
  `weight_kg` DECIMAL(18,6),
  `reps` INT,
  `duration_sec` INT,
  `rest_sec` INT,
  `distance_m` DECIMAL(18,6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_exercise_sets_1` (`session_item_id`, `set_no`),
  CONSTRAINT `fk_exercise_sets_session_item_id` FOREIGN KEY (`session_item_id`) REFERENCES `exercise_session_items` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `foods` (
  `id` BINARY(16) NOT NULL,
  `owner_user_id` BINARY(16),
  `source_type` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `brand` VARCHAR(255),
  `barcode` VARCHAR(128),
  `verification_status` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_foods_owner_user_id` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `food_nutrients` (
  `food_id` BINARY(16) NOT NULL,
  `nutrient_code` VARCHAR(128) NOT NULL,
  `amount` DECIMAL(18,6) NOT NULL,
  `basis_quantity` DECIMAL(18,6) NOT NULL,
  `basis_unit_code` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`food_id`, `nutrient_code`),
  CONSTRAINT `fk_food_nutrients_food_id` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_food_nutrients_nutrient_code` FOREIGN KEY (`nutrient_code`) REFERENCES `nutrient_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_food_nutrients_basis_unit_code` FOREIGN KEY (`basis_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `food_allergens` (
  `food_id` BINARY(16) NOT NULL,
  `allergen_code` VARCHAR(128) NOT NULL,
  `presence_type` VARCHAR(64) NOT NULL,
  `source_type` VARCHAR(64) NOT NULL,
  `verified_at` DATETIME(6),
  PRIMARY KEY (`food_id`, `allergen_code`),
  CONSTRAINT `fk_food_allergens_food_id` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_food_allergens_allergen_code` FOREIGN KEY (`allergen_code`) REFERENCES `allergens` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `meals` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `habit_occurrence_id` BINARY(16),
  `meal_type` VARCHAR(64) NOT NULL,
  `eaten_at` DATETIME(6),
  `status` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_meals_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meals_habit_occurrence_id` FOREIGN KEY (`habit_occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `meal_items` (
  `id` BINARY(16) NOT NULL,
  `meal_id` BINARY(16) NOT NULL,
  `food_id` BINARY(16) NOT NULL,
  `quantity` DECIMAL(18,6) NOT NULL,
  `unit_code` VARCHAR(128) NOT NULL,
  `estimated_weight_g` DECIMAL(18,6),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_meal_items_meal_id` FOREIGN KEY (`meal_id`) REFERENCES `meals` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meal_items_food_id` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meal_items_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `meal_item_nutrients` (
  `meal_item_id` BINARY(16) NOT NULL,
  `nutrient_code` VARCHAR(128) NOT NULL,
  `amount` DECIMAL(18,6) NOT NULL,
  `source_type` VARCHAR(64) NOT NULL,
  `calculated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`meal_item_id`, `nutrient_code`),
  CONSTRAINT `fk_meal_item_nutrients_meal_item_id` FOREIGN KEY (`meal_item_id`) REFERENCES `meal_items` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meal_item_nutrients_nutrient_code` FOREIGN KEY (`nutrient_code`) REFERENCES `nutrient_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `user_allergies` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `allergen_code` VARCHAR(128) NOT NULL,
  `severity` VARCHAR(64),
  `reaction_note` TEXT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_allergies_1` (`user_id`, `allergen_code`),
  CONSTRAINT `fk_user_allergies_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_user_allergies_allergen_code` FOREIGN KEY (`allergen_code`) REFERENCES `allergens` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_products` (
  `id` BINARY(16) NOT NULL,
  `external_code` VARCHAR(128),
  `kind` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `ingredient_text` TEXT,
  `manufacturer` VARCHAR(255),
  `dosage_form_code` VARCHAR(128),
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_medication_products_dosage_form_code` FOREIGN KEY (`dosage_form_code`) REFERENCES `dosage_form_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_product_ingredients` (
  `product_id` BINARY(16) NOT NULL,
  `ingredient_id` BINARY(16) NOT NULL,
  `amount` DECIMAL(18,6),
  `unit_code` VARCHAR(128),
  `basis_quantity` DECIMAL(18,6),
  `is_active_ingredient` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`product_id`, `ingredient_id`),
  CONSTRAINT `fk_medication_product_ingredients_product_id` FOREIGN KEY (`product_id`) REFERENCES `medication_products` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_product_ingredients_ingredient_id` FOREIGN KEY (`ingredient_id`) REFERENCES `medication_ingredients` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_product_ingredients_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_ingredient_interactions` (
  `ingredient_id` BINARY(16) NOT NULL,
  `interacting_ingredient_id` BINARY(16) NOT NULL,
  `severity` VARCHAR(64) NOT NULL,
  `description` TEXT NOT NULL,
  `evidence_source` VARCHAR(255),
  PRIMARY KEY (`ingredient_id`, `interacting_ingredient_id`),
  CONSTRAINT `fk_medication_ingredient_interactions_ingredient_id` FOREIGN KEY (`ingredient_id`) REFERENCES `medication_ingredients` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_ingredient_interactions_interacting_ingredient_id` FOREIGN KEY (`interacting_ingredient_id`) REFERENCES `medication_ingredients` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_ingredient_interactions_1` CHECK (`ingredient_id` <> `interacting_ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `prescriptions` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `institution_id` BINARY(16),
  `prescribed_at` DATE,
  `duration_days` INT,
  `source_type` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_prescriptions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_prescriptions_institution_id` FOREIGN KEY (`institution_id`) REFERENCES `healthcare_institutions` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `user_medications` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `product_id` BINARY(16),
  `prescription_id` BINARY(16),
  `display_name` VARCHAR(255) NOT NULL,
  `purpose` VARCHAR(255),
  `started_on` DATE,
  `ended_on` DATE,
  `status` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_medications_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_user_medications_product_id` FOREIGN KEY (`product_id`) REFERENCES `medication_products` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_user_medications_prescription_id` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_schedules` (
  `id` BINARY(16) NOT NULL,
  `user_medication_id` BINARY(16) NOT NULL,
  `schedule_type` VARCHAR(64) NOT NULL,
  `interval_days` INT,
  `is_prn` TINYINT(1) NOT NULL DEFAULT 0,
  `timezone` VARCHAR(64) NOT NULL,
  `effective_from` DATE NOT NULL,
  `effective_to` DATE,
  `version` INT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_schedules_1` (`user_medication_id`, `version`),
  CONSTRAINT `fk_medication_schedules_user_medication_id` FOREIGN KEY (`user_medication_id`) REFERENCES `user_medications` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_schedules_1` CHECK (`interval_days` IS NULL OR `interval_days` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_schedule_days` (
  `schedule_id` BINARY(16) NOT NULL,
  `weekday` INT NOT NULL,
  PRIMARY KEY (`schedule_id`, `weekday`),
  CONSTRAINT `fk_medication_schedule_days_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_schedule_days_1` CHECK (`weekday` BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_schedule_times` (
  `id` BINARY(16) NOT NULL,
  `schedule_id` BINARY(16) NOT NULL,
  `sequence_no` INT NOT NULL,
  `local_time` TIME,
  `dose_amount` DECIMAL(18,6) NOT NULL,
  `dose_unit_code` VARCHAR(128) NOT NULL,
  `meal_relation` VARCHAR(64),
  `offset_minutes` INT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_schedule_times_1` (`schedule_id`, `sequence_no`),
  CONSTRAINT `fk_medication_schedule_times_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_schedule_times_dose_unit_code` FOREIGN KEY (`dose_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_schedule_times_1` CHECK (`sequence_no` > 0),
  CONSTRAINT `ck_medication_schedule_times_2` CHECK (`dose_amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_dose_occurrences` (
  `id` BINARY(16) NOT NULL,
  `schedule_id` BINARY(16) NOT NULL,
  `scheduled_at` DATETIME(6) NOT NULL,
  `current_status` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_dose_occurrences_1` (`schedule_id`, `scheduled_at`),
  CONSTRAINT `fk_medication_dose_occurrences_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_intake_events` (
  `id` BINARY(16) NOT NULL,
  `occurrence_id` BINARY(16) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `actual_at` DATETIME(6),
  `recorded_at` DATETIME(6) NOT NULL,
  `actual_amount` DECIMAL(18,6),
  `unit_code` VARCHAR(128),
  `memo` TEXT,
  `idempotency_key` VARCHAR(255),
  `previous_event_id` BINARY(16),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_intake_events_1` (`idempotency_key`),
  KEY `ix_medication_intake_events_1` (`occurrence_id`, `recorded_at`),
  CONSTRAINT `fk_medication_intake_events_occurrence_id` FOREIGN KEY (`occurrence_id`) REFERENCES `medication_dose_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_intake_events_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_medication_intake_events_previous_event_id` FOREIGN KEY (`previous_event_id`) REFERENCES `medication_intake_events` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `medication_inventory_transactions` (
  `id` BINARY(16) NOT NULL,
  `user_medication_id` BINARY(16) NOT NULL,
  `transaction_type` VARCHAR(64) NOT NULL,
  `quantity_delta` DECIMAL(18,6) NOT NULL,
  `unit_code` VARCHAR(128) NOT NULL,
  `source_event_id` BINARY(16),
  `occurred_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_medication_inventory_transactions_user_medication_id` FOREIGN KEY (`user_medication_id`) REFERENCES `user_medications` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_inventory_transactions_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `observation_sessions` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `measured_at` DATETIME(6) NOT NULL,
  `source_type` VARCHAR(64) NOT NULL,
  `external_record_id` VARCHAR(255),
  `context_json` JSON,
  PRIMARY KEY (`id`),
  KEY `ix_observation_sessions_1` (`user_id`, `measured_at`),
  CONSTRAINT `fk_observation_sessions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `health_observations` (
  `id` BINARY(16) NOT NULL,
  `session_id` BINARY(16) NOT NULL,
  `type_code` VARCHAR(128) NOT NULL,
  `numeric_value` DECIMAL(18,6),
  `text_value` VARCHAR(255),
  `unit_code` VARCHAR(128),
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_health_observations_1` (`session_id`, `type_code`),
  CONSTRAINT `fk_health_observations_session_id` FOREIGN KEY (`session_id`) REFERENCES `observation_sessions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_health_observations_type_code` FOREIGN KEY (`type_code`) REFERENCES `observation_types` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_health_observations_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_health_observations_type_unit` FOREIGN KEY (`type_code`, `unit_code`) REFERENCES `observation_type_units` (`observation_type_code`, `unit_code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_health_observations_1` CHECK (((`numeric_value` IS NOT NULL) + (`text_value` IS NOT NULL)) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `notification_rules` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `offset_minutes` INT NOT NULL,
  `max_retries` INT NOT NULL DEFAULT 0,
  `retry_interval_minutes` INT,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_notification_rules_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `notification_rule_habit_targets` (
  `rule_id` BINARY(16) NOT NULL,
  `habit_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`rule_id`, `habit_id`),
  CONSTRAINT `fk_notification_rule_habit_targets_rule_id` FOREIGN KEY (`rule_id`) REFERENCES `notification_rules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_notification_rule_habit_targets_habit_id` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `notification_rule_medication_targets` (
  `rule_id` BINARY(16) NOT NULL,
  `user_medication_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`rule_id`, `user_medication_id`),
  CONSTRAINT `fk_notification_rule_medication_targets_rule_id` FOREIGN KEY (`rule_id`) REFERENCES `notification_rules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_notification_rule_medication_targets_user_medication_id` FOREIGN KEY (`user_medication_id`) REFERENCES `user_medications` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `notification_jobs` (
  `id` BINARY(16) NOT NULL,
  `rule_id` BINARY(16),
  `habit_occurrence_id` BINARY(16),
  `medication_dose_occurrence_id` BINARY(16),
  `job_type` VARCHAR(64) NOT NULL,
  `scheduled_at` DATETIME(6) NOT NULL,
  `status` VARCHAR(64) NOT NULL,
  `retry_no` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_notification_jobs_1` (`status`, `scheduled_at`),
  UNIQUE KEY `uq_notification_jobs_1` (`habit_occurrence_id`, `job_type`, `retry_no`),
  UNIQUE KEY `uq_notification_jobs_2` (`medication_dose_occurrence_id`, `job_type`, `retry_no`),
  CONSTRAINT `fk_notification_jobs_rule_id` FOREIGN KEY (`rule_id`) REFERENCES `notification_rules` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_notification_jobs_habit_occurrence_id` FOREIGN KEY (`habit_occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_notification_jobs_medication_dose_occurrence_id` FOREIGN KEY (`medication_dose_occurrence_id`) REFERENCES `medication_dose_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_notification_jobs_1` CHECK (((`habit_occurrence_id` IS NOT NULL) + (`medication_dose_occurrence_id` IS NOT NULL)) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `notification_deliveries` (
  `id` BINARY(16) NOT NULL,
  `job_id` BINARY(16) NOT NULL,
  `channel` VARCHAR(64) NOT NULL,
  `provider_message_id` VARCHAR(255),
  `status` VARCHAR(64) NOT NULL,
  `sent_at` DATETIME(6),
  `delivered_at` DATETIME(6),
  `opened_at` DATETIME(6),
  `error_code` VARCHAR(128),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_notification_deliveries_1` (`provider_message_id`),
  CONSTRAINT `fk_notification_deliveries_job_id` FOREIGN KEY (`job_id`) REFERENCES `notification_jobs` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `goals` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `category_code` VARCHAR(128) NOT NULL,
  `goal_type` VARCHAR(64) NOT NULL,
  `target_value` DECIMAL(18,6),
  `unit_code` VARCHAR(128),
  `started_on` DATE NOT NULL,
  `ended_on` DATE,
  `status` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_goals_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_goals_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `reward_ledger` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `category_code` VARCHAR(128),
  `reward_type` VARCHAR(64) NOT NULL,
  `amount` DECIMAL(18,6) NOT NULL,
  `reason_type` VARCHAR(64) NOT NULL,
  `reason_id` BINARY(16),
  `reversal_of_id` BINARY(16),
  `occurred_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_reward_ledger_1` (`user_id`, `occurred_at`),
  CONSTRAINT `fk_reward_ledger_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_reward_ledger_reversal_of_id` FOREIGN KEY (`reversal_of_id`) REFERENCES `reward_ledger` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `analytics_events` (
  `event_id` BINARY(16) NOT NULL,
  `occurred_at` DATETIME(6) NOT NULL,
  `received_at` DATETIME(6) NOT NULL,
  `user_id` BINARY(16),
  `anonymous_id` VARCHAR(255),
  `session_id` BINARY(16),
  `event_name` VARCHAR(128) NOT NULL,
  `screen_name` VARCHAR(128),
  `properties_json` JSON NOT NULL,
  `schema_version` INT NOT NULL,
  PRIMARY KEY (`event_id`, `occurred_at`),
  KEY `ix_analytics_events_1` (`user_id`, `occurred_at`),
  KEY `ix_analytics_events_2` (`event_name`, `occurred_at`),
  CONSTRAINT `fk_analytics_events_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `ai_jobs` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `job_type` VARCHAR(64) NOT NULL,
  `status` VARCHAR(64) NOT NULL,
  `model_provider` VARCHAR(255),
  `model_name` VARCHAR(255),
  `prompt_version` VARCHAR(255),
  `requested_at` DATETIME(6) NOT NULL,
  `completed_at` DATETIME(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ai_jobs_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `ai_job_outputs` (
  `id` BINARY(16) NOT NULL,
  `job_id` BINARY(16) NOT NULL,
  `output_json` JSON NOT NULL,
  `confidence` DECIMAL(18,6),
  `safety_flags` JSON,
  `accepted_at` DATETIME(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ai_job_outputs_job_id` FOREIGN KEY (`job_id`) REFERENCES `ai_jobs` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `integration_connections` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `provider` VARCHAR(64) NOT NULL,
  `status` VARCHAR(64) NOT NULL,
  `scopes` JSON NOT NULL,
  `token_reference` VARCHAR(255),
  `connected_at` DATETIME(6) NOT NULL,
  `disconnected_at` DATETIME(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_integration_connections_1` (`user_id`, `provider`),
  CONSTRAINT `fk_integration_connections_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `external_record_mappings` (
  `id` BINARY(16) NOT NULL,
  `connection_id` BINARY(16) NOT NULL,
  `external_id` VARCHAR(255) NOT NULL,
  `internal_type` VARCHAR(64) NOT NULL,
  `internal_id` BINARY(16) NOT NULL,
  `source_updated_at` DATETIME(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_external_record_mappings_1` (`connection_id`, `external_id`),
  CONSTRAINT `fk_external_record_mappings_connection_id` FOREIGN KEY (`connection_id`) REFERENCES `integration_connections` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `healthcare_connections` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `institution_id` BINARY(16) NOT NULL,
  `status` VARCHAR(64) NOT NULL,
  `connected_at` DATETIME(6),
  `disconnected_at` DATETIME(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_healthcare_connections_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_healthcare_connections_institution_id` FOREIGN KEY (`institution_id`) REFERENCES `healthcare_institutions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `healthcare_data_consents` (
  `id` BINARY(16) NOT NULL,
  `connection_id` BINARY(16) NOT NULL,
  `purpose` VARCHAR(255) NOT NULL,
  `data_categories` JSON NOT NULL,
  `valid_from` DATETIME(6) NOT NULL,
  `valid_to` DATETIME(6),
  `withdrawn_at` DATETIME(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_healthcare_data_consents_connection_id` FOREIGN KEY (`connection_id`) REFERENCES `healthcare_connections` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `audit_logs` (
  `id` BINARY(16) NOT NULL,
  `actor_id` BINARY(16),
  `actor_type` VARCHAR(64) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `resource_type` VARCHAR(64) NOT NULL,
  `resource_id` BINARY(16),
  `occurred_at` DATETIME(6) NOT NULL,
  `result` VARCHAR(64) NOT NULL,
  `metadata_json` JSON,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
