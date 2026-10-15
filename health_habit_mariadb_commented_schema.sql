-- AI 건강습관 서비스 MariaDB 정규화 v2 주석 포함 테이블 정의
-- Target: MariaDB 11.4 LTS, InnoDB, utf8mb4_uca1400_ai_ci
-- UUID: BINARY(16), DateTime: UTC DATETIME(6)

CREATE TABLE `users` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `email` VARCHAR(320) NOT NULL COMMENT '사용자가 입력한 로그인 이메일 원문.',
  `email_normalized` VARCHAR(320) NOT NULL COMMENT '중복 검사와 로그인 검색에 사용하는 소문자·공백 제거 이메일.',
  `password_hash` VARCHAR(255) COMMENT '비밀번호 원문이 아닌 Argon2id 등 단방향 해시값.',
  `status` VARCHAR(64) NOT NULL COMMENT '활성·휴면·정지·탈퇴대기 등 회원 계정 상태.',
  `timezone` VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul' COMMENT '일정 생성과 화면 표시 기준인 IANA 시간대.',
  `locale` VARCHAR(32) NOT NULL DEFAULT 'ko-KR' COMMENT '화면 언어와 지역화 형식을 결정하는 로케일 코드.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  `deleted_at` DATETIME(6) COMMENT '논리 삭제된 UTC 일시이며 NULL이면 유효한 데이터.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_1` (`email_normalized`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='서비스 로그인 계정과 계정 상태, 기본 언어 및 시간대를 관리한다.';

CREATE TABLE `user_profiles` (
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `nickname` VARCHAR(255) COMMENT '서비스에 표시되는 사용자 닉네임.',
  `birth_date` DATE COMMENT '연령 계산과 개인화에 사용하는 생년월일.',
  `sex` VARCHAR(255) COMMENT '사용자가 선택한 성별 또는 생물학적 성별 분류값.',
  `height_cm` DECIMAL(18,6) COMMENT 'BMI 등 계산에 사용하는 최근 대표 키 값(cm).',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='회원 화면에 표시되는 닉네임과 건강 계산에 필요한 기본 프로필을 관리한다.';

CREATE TABLE `user_consents` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `consent_type` VARCHAR(64) NOT NULL COMMENT '서비스 약관·개인정보·민감정보 등 동의 문서 유형.',
  `version` VARCHAR(128) NOT NULL COMMENT '사용자가 동의한 약관 또는 정책 문서 버전.',
  `consented_at` DATETIME(6) NOT NULL COMMENT '사용자가 동의한 UTC 일시.',
  `withdrawn_at` DATETIME(6) COMMENT '사용자가 동의를 철회한 UTC 일시.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_consents_1` (`user_id`, `consent_type`, `version`),
  CONSTRAINT `fk_user_consents_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='약관과 개인정보·건강정보 처리 동의의 버전별 동의 및 철회 이력을 보관한다.';

CREATE TABLE `habit_categories` (
  `code` VARCHAR(128) NOT NULL COMMENT '업무에서 사용하는 변경 가능성이 낮은 고유 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `health_stat_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '해당 습관 분류를 건강 통계 집계에 포함할지 여부.',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='운동·식사·복약·수면·수분·기타 등 습관 분류와 통계 포함 여부를 정의한다.';

CREATE TABLE `unit_definitions` (
  `code` VARCHAR(128) NOT NULL COMMENT '업무에서 사용하는 변경 가능성이 낮은 고유 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `symbol` VARCHAR(255) NOT NULL COMMENT '화면과 단위 입력에 표시하는 축약 기호.',
  `dimension_code` VARCHAR(128) NOT NULL COMMENT '질량·부피·에너지·압력·횟수 등 단위 차원 코드.',
  `canonical_factor` DECIMAL(18,6) NOT NULL COMMENT '같은 차원의 기준 단위로 환산할 때 곱하는 변환계수.',
  `decimal_scale` INT NOT NULL DEFAULT 2 COMMENT '화면 표시와 반올림에 사용하는 소수점 자릿수.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`code`),
  CONSTRAINT `ck_unit_definitions_1` CHECK (`canonical_factor` > 0),
  CONSTRAINT `ck_unit_definitions_2` CHECK (`decimal_scale` BETWEEN 0 AND 12)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='서비스 전 영역에서 사용하는 수량·영양·복약·건강측정 단위와 기준 단위 변환정보를 관리한다.';

CREATE TABLE `nutrient_definitions` (
  `code` VARCHAR(128) NOT NULL COMMENT '업무에서 사용하는 변경 가능성이 낮은 고유 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `canonical_unit_code` VARCHAR(128) NOT NULL COMMENT '해당 영양소나 측정항목의 기준 저장 단위 코드.',
  `nutrient_group` VARCHAR(128) NOT NULL COMMENT '다량영양소·당·각성물질·열량 등 영양소 그룹.',
  `parent_code` VARCHAR(128) COMMENT '첨가당·유리당처럼 상위 집계 영양소를 가리키는 자기참조 코드.',
  `included_in_total` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '하위 영양소를 상위 영양소 합계에 포함할지 여부.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`code`),
  CONSTRAINT `fk_nutrient_definitions_canonical_unit_code` FOREIGN KEY (`canonical_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_nutrient_definitions_parent_code` FOREIGN KEY (`parent_code`) REFERENCES `nutrient_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='열량·탄수화물·단백질·지방·당·카페인 등 영양소의 분류와 기본 단위를 정의한다.';

CREATE TABLE `allergens` (
  `code` VARCHAR(128) NOT NULL COMMENT '업무에서 사용하는 변경 가능성이 낮은 고유 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `description` VARCHAR(255) COMMENT '항목의 의미·주의사항 또는 상호작용 상세 설명.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='음식과 사용자 알레르기 기록에서 공통으로 참조하는 알레르기 성분 사전을 관리한다.';

CREATE TABLE `exercise_categories` (
  `code` VARCHAR(128) NOT NULL COMMENT '업무에서 사용하는 변경 가능성이 낮은 고유 코드.',
  `parent_code` VARCHAR(128) COMMENT '운동 세부분류가 속한 상위 exercise_categories.code.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `level` INT NOT NULL COMMENT '대분류·중분류·세부분류 등 계층 깊이.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`code`),
  CONSTRAINT `fk_exercise_categories_parent_code` FOREIGN KEY (`parent_code`) REFERENCES `exercise_categories` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_exercise_categories_1` CHECK (`level` BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='운동 대분류와 세부분류를 자기참조 계층구조로 관리한다.';

CREATE TABLE `dosage_form_definitions` (
  `code` VARCHAR(128) NOT NULL COMMENT '업무에서 사용하는 변경 가능성이 낮은 고유 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='정제·캡슐·액상·분말 등 의약품과 보충제 제형 사전을 관리한다.';

CREATE TABLE `medication_ingredients` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `external_code` VARCHAR(128) COMMENT '공공 API·제휴기관 등 외부 시스템에서 사용하는 식별 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `normalized_name` VARCHAR(255) NOT NULL COMMENT '대소문자·공백·표기 차이를 정리한 중복 검색용 명칭.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_ingredients_1` (`external_code`),
  UNIQUE KEY `uq_medication_ingredients_2` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='의약품·영양제·보충제를 구성하는 정규화된 성분 사전을 관리한다.';

CREATE TABLE `observation_types` (
  `code` VARCHAR(128) NOT NULL COMMENT '업무에서 사용하는 변경 가능성이 낮은 고유 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `value_kind` VARCHAR(255) NOT NULL COMMENT '숫자형 또는 문자형 중 해당 측정값의 저장 형식.',
  `canonical_unit_code` VARCHAR(128) COMMENT '측정값을 내부에서 비교·집계할 때 사용하는 기준 단위.',
  `min_value` DECIMAL(18,6) COMMENT '입력값 검증에 사용하는 허용 최소값.',
  `max_value` DECIMAL(18,6) COMMENT '입력값 검증에 사용하는 허용 최대값.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`code`),
  CONSTRAINT `fk_observation_types_canonical_unit_code` FOREIGN KEY (`canonical_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_observation_types_1` CHECK (`min_value` IS NULL OR `max_value` IS NULL OR `min_value` <= `max_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='체중·체지방·혈압·혈당·심박·체온 등 신체측정 항목과 값 형식을 정의한다.';

CREATE TABLE `observation_type_units` (
  `observation_type_code` VARCHAR(128) NOT NULL COMMENT '허용 단위를 연결할 observation_types.code.',
  `unit_code` VARCHAR(128) NOT NULL COMMENT '수량·복용량·측정값에 사용하는 unit_definitions.code.',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '여러 허용 단위 중 화면에 기본 선택되는 단위인지 여부.',
  PRIMARY KEY (`observation_type_code`, `unit_code`),
  CONSTRAINT `fk_observation_type_units_observation_type_code` FOREIGN KEY (`observation_type_code`) REFERENCES `observation_types` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_observation_type_units_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='각 신체측정 항목에서 입력할 수 있는 단위와 기본 단위를 관리한다.';

CREATE TABLE `healthcare_institutions` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `external_code` VARCHAR(128) COMMENT '공공 API·제휴기관 등 외부 시스템에서 사용하는 식별 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `status` VARCHAR(64) NOT NULL COMMENT '연계검토·활성·중지 등 의료기관 사용 상태.',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='향후 병원 연계를 위한 의료기관 마스터를 관리한다.';

CREATE TABLE `habits` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `category_code` VARCHAR(128) NOT NULL COMMENT '습관·운동·목표·보상 영역의 분류 코드.',
  `title` VARCHAR(255) NOT NULL COMMENT '사용자가 확인하는 습관 또는 콘텐츠 제목.',
  `priority` SMALLINT COMMENT '사용자가 지정한 수행 우선순위.',
  `status` VARCHAR(64) NOT NULL COMMENT '초안·활성·일시중지·종료 등 습관 운영 상태.',
  `start_date` DATE NOT NULL COMMENT '습관 적용이 시작되는 현지 날짜.',
  `end_date` DATE COMMENT '습관 적용이 종료되는 현지 날짜이며 NULL이면 종료일 없음.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  `deleted_at` DATETIME(6) COMMENT '논리 삭제된 UTC 일시이며 NULL이면 유효한 데이터.',
  PRIMARY KEY (`id`),
  KEY `ix_habits_1` (`user_id`, `status`),
  CONSTRAINT `fk_habits_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_habits_category_code` FOREIGN KEY (`category_code`) REFERENCES `habit_categories` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자가 생성하거나 템플릿에서 가져온 개별 습관의 기본 설정과 운영 기간을 관리한다.';

CREATE TABLE `habit_schedules` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `habit_id` BINARY(16) NOT NULL COMMENT '알림 또는 일정의 대상 habits.id.',
  `schedule_type` VARCHAR(64) NOT NULL COMMENT '매일·요일·N일 간격·고급 규칙 등 습관 반복 유형.',
  `interval_value` INT COMMENT 'N일·N주 간격 반복에서 사용하는 양의 정수 간격.',
  `timezone` VARCHAR(64) NOT NULL COMMENT '일정 생성과 화면 표시 기준인 IANA 시간대.',
  `advanced_rule_json` JSON COMMENT '월말·격주 조합 등 일반 컬럼으로 표현하기 어려운 고급 반복 규칙 JSON.',
  `effective_from` DATE NOT NULL COMMENT '해당 일정이나 정책 버전이 적용되기 시작하는 날짜.',
  `effective_to` DATE COMMENT '해당 일정이나 정책 버전의 마지막 적용 날짜.',
  `version` INT NOT NULL COMMENT '같은 습관 일정 변경 이력을 구분하는 증가 버전.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_schedules_1` (`habit_id`, `version`),
  CONSTRAINT `fk_habit_schedules_habit_id` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_habit_schedules_1` CHECK (`interval_value` IS NULL OR `interval_value` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='습관의 반복 규칙과 알림 기준이 되는 현지 시각을 버전별로 관리한다.';

CREATE TABLE `habit_schedule_days` (
  `schedule_id` BINARY(16) NOT NULL COMMENT '회차를 생성한 일정 레코드의 외래키.',
  `weekday` INT NOT NULL COMMENT 'ISO 요일 번호로 1은 월요일, 7은 일요일.',
  PRIMARY KEY (`schedule_id`, `weekday`),
  CONSTRAINT `fk_habit_schedule_days_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_habit_schedule_days_1` CHECK (`weekday` BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='요일 기반 습관 일정에서 반복되는 요일을 관계형 데이터로 관리한다.';

CREATE TABLE `habit_schedule_times` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `schedule_id` BINARY(16) NOT NULL COMMENT '회차를 생성한 일정 레코드의 외래키.',
  `sequence_no` INT NOT NULL COMMENT '같은 일정 안에서 회차의 정렬 및 식별에 사용하는 순번.',
  `local_time` TIME NOT NULL COMMENT '사용자 시간대에서 습관을 수행하기로 한 현지 시각.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_schedule_times_1` (`schedule_id`, `sequence_no`),
  CONSTRAINT `fk_habit_schedule_times_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_habit_schedule_times_1` CHECK (`sequence_no` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='하루에 한 번 이상 실행할 수 있는 습관의 현지 수행시각을 관리한다.';

CREATE TABLE `habit_schedule_exceptions` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `schedule_id` BINARY(16) NOT NULL COMMENT '회차를 생성한 일정 레코드의 외래키.',
  `exception_date` DATE NOT NULL COMMENT '일정 예외가 적용되는 사용자 현지 날짜.',
  `action` VARCHAR(64) NOT NULL COMMENT '해당 날짜를 제외·휴식 처리하거나 대체 시각으로 이동하는 예외 유형.',
  `replacement_at` DATETIME(6) COMMENT '원래 일정 대신 수행하도록 변경한 UTC 예정시각.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_schedule_exceptions_1` (`schedule_id`, `exception_date`),
  CONSTRAINT `fk_habit_schedule_exceptions_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='특정 날짜의 습관 일정을 제외·휴식·대체 시각으로 변경하는 예외를 관리한다.';

CREATE TABLE `habit_occurrences` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `habit_id` BINARY(16) NOT NULL COMMENT '알림 또는 일정의 대상 habits.id.',
  `schedule_id` BINARY(16) NOT NULL COMMENT '회차를 생성한 일정 레코드의 외래키.',
  `scheduled_at` DATETIME(6) NOT NULL COMMENT '해당 회차나 작업이 예정된 UTC 일시.',
  `due_at` DATETIME(6) COMMENT '지각·실패 판정에 사용하는 마감 UTC 일시.',
  `current_status` VARCHAR(64) NOT NULL COMMENT '이벤트 이력을 반영한 해당 회차의 최신 상태.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_occurrences_1` (`schedule_id`, `scheduled_at`),
  KEY `ix_habit_occurrences_1` (`habit_id`, `scheduled_at`),
  CONSTRAINT `fk_habit_occurrences_habit_id` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_habit_occurrences_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `habit_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='반복 일정에서 생성된 개별 예정 회차와 현재 수행 상태를 관리한다.';

CREATE TABLE `habit_action_events` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `occurrence_id` BINARY(16) NOT NULL COMMENT '행동이 발생한 개별 예정 회차의 외래키.',
  `action` VARCHAR(64) NOT NULL COMMENT '완료·미루기·건너뛰기·실패확정·완료취소 등 회차 상태 변경 행동.',
  `actual_at` DATETIME(6) COMMENT '사용자가 실제 행동했다고 입력한 UTC 일시.',
  `recorded_at` DATETIME(6) NOT NULL COMMENT '사용자가 앱에 기록을 저장한 UTC 일시.',
  `source_type` VARCHAR(64) NOT NULL COMMENT '수동 입력·알림·위젯·웨어러블·AI 승인 등 데이터 생성 출처.',
  `note` TEXT COMMENT '사용자가 입력한 부가 메모.',
  `idempotency_key` VARCHAR(255) COMMENT '네트워크 재시도 시 동일 기록의 중복 생성을 방지하는 키.',
  `previous_event_id` BINARY(16) COMMENT '수정 또는 취소 대상이 된 이전 이벤트의 자기참조 외래키.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_habit_action_events_1` (`idempotency_key`),
  KEY `ix_habit_action_events_1` (`occurrence_id`, `recorded_at`),
  CONSTRAINT `fk_habit_action_events_occurrence_id` FOREIGN KEY (`occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_habit_action_events_previous_event_id` FOREIGN KEY (`previous_event_id`) REFERENCES `habit_action_events` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='습관 회차의 완료·미루기·건너뛰기·수정 행동을 변경 이력 방식으로 저장한다.';

CREATE TABLE `exercise_definitions` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `category_code` VARCHAR(128) NOT NULL COMMENT '습관·운동·목표·보상 영역의 분류 코드.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `measurement_type` VARCHAR(64) NOT NULL COMMENT '횟수·시간·거리·중량 등 운동 기록 방식.',
  `met_value` DECIMAL(18,6) COMMENT '칼로리 추정에 사용하는 운동별 MET 기준값.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_exercise_definitions_category_code` FOREIGN KEY (`category_code`) REFERENCES `exercise_categories` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='근력·유산소 등 사용자가 선택할 수 있는 운동 종목 사전을 관리한다.';

CREATE TABLE `exercise_sessions` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `habit_occurrence_id` BINARY(16) COMMENT '알림 작업이 연결된 습관 예정 회차 ID.',
  `started_at` DATETIME(6) NOT NULL COMMENT '실제 수행 또는 연결이 시작된 UTC 일시.',
  `ended_at` DATETIME(6) COMMENT '실제 수행 또는 적용이 종료된 UTC 일시.',
  `total_kcal` DECIMAL(18,6) COMMENT '운동 또는 식사의 계산된 총 열량(kcal).',
  `calculation_method` VARCHAR(255) COMMENT '칼로리를 계산한 공식·기기·사용자 입력 등의 방식.',
  `weight_snapshot_kg` DECIMAL(18,6) COMMENT '칼로리 계산 당시 사용한 사용자 체중 스냅샷(kg).',
  `source_type` VARCHAR(64) NOT NULL COMMENT '사용자 수동기록·웨어러블·외부 연동 등 운동 데이터 출처.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_exercise_sessions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_exercise_sessions_habit_occurrence_id` FOREIGN KEY (`habit_occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자가 수행한 한 번의 운동 세션과 칼로리 계산 정보를 기록한다.';

CREATE TABLE `exercise_session_items` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `session_id` BINARY(16) NOT NULL COMMENT '해당 상세 레코드가 속한 세션의 외래키.',
  `exercise_id` BINARY(16) NOT NULL COMMENT '선택한 운동 종목의 exercise_definitions.id.',
  `order_no` INT NOT NULL COMMENT '세션이나 루틴 안에서 표시·수행되는 순서.',
  `duration_sec` INT COMMENT '운동 또는 세트의 실제 지속시간(초).',
  `distance_m` DECIMAL(18,6) COMMENT '운동 또는 세트의 이동거리(m).',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_exercise_session_items_session_id` FOREIGN KEY (`session_id`) REFERENCES `exercise_sessions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_exercise_session_items_exercise_id` FOREIGN KEY (`exercise_id`) REFERENCES `exercise_definitions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='운동 세션 안에서 수행한 운동 종목과 순서를 기록한다.';

CREATE TABLE `exercise_sets` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `session_item_id` BINARY(16) NOT NULL COMMENT '세트가 속한 exercise_session_items.id.',
  `set_no` INT NOT NULL COMMENT '운동 종목 안에서의 세트 순번.',
  `weight_kg` DECIMAL(18,6) COMMENT '근력운동 세트에 사용한 중량(kg).',
  `reps` INT COMMENT '세트에서 수행한 반복 횟수.',
  `duration_sec` INT COMMENT '운동 또는 세트의 실제 지속시간(초).',
  `rest_sec` INT COMMENT '세트 종료 후 휴식시간(초).',
  `distance_m` DECIMAL(18,6) COMMENT '운동 또는 세트의 이동거리(m).',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_exercise_sets_1` (`session_item_id`, `set_no`),
  CONSTRAINT `fk_exercise_sets_session_item_id` FOREIGN KEY (`session_item_id`) REFERENCES `exercise_session_items` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='운동 종목의 세트별 중량·횟수·시간·휴식·거리를 기록한다.';

CREATE TABLE `foods` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `owner_user_id` BINARY(16) COMMENT '사용자 직접 등록 음식의 소유 회원이며 공용 음식은 NULL.',
  `source_type` VARCHAR(64) NOT NULL COMMENT '공공 API·관리자·사용자 직접등록·AI 승인 등 음식정보 출처.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `brand` VARCHAR(255) COMMENT '음식 또는 제품의 브랜드명.',
  `barcode` VARCHAR(128) COMMENT 'FOOD QR·바코드 검색에 사용하는 상품 코드.',
  `verification_status` VARCHAR(64) NOT NULL COMMENT '미검토·검토중·검증완료·반려 등 음식정보 검토 상태.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_foods_owner_user_id` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='공공 데이터·관리자·사용자가 등록한 음식의 기본정보와 검증 상태를 관리한다.';

CREATE TABLE `food_nutrients` (
  `food_id` BINARY(16) NOT NULL COMMENT '섭취하거나 영양정보를 조회한 foods.id 외래키.',
  `nutrient_code` VARCHAR(128) NOT NULL COMMENT '열량·탄수화물·단백질·지방·당·카페인 등 영양소 코드.',
  `amount` DECIMAL(18,6) NOT NULL COMMENT '기준 수량에 포함된 해당 영양소 양.',
  `basis_quantity` DECIMAL(18,6) NOT NULL COMMENT '성분 또는 영양 함량이 표시되는 제품·음식의 기준 수량.',
  `basis_unit_code` VARCHAR(128) NOT NULL COMMENT '영양정보 기준 수량의 unit_definitions.code.',
  PRIMARY KEY (`food_id`, `nutrient_code`),
  CONSTRAINT `fk_food_nutrients_food_id` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_food_nutrients_nutrient_code` FOREIGN KEY (`nutrient_code`) REFERENCES `nutrient_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_food_nutrients_basis_unit_code` FOREIGN KEY (`basis_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='음식의 기준 섭취량당 열량·탄수화물·단백질·지방·당·카페인 값을 저장한다.';

CREATE TABLE `food_allergens` (
  `food_id` BINARY(16) NOT NULL COMMENT '섭취하거나 영양정보를 조회한 foods.id 외래키.',
  `allergen_code` VARCHAR(128) NOT NULL COMMENT '알레르기 원인 성분을 식별하는 코드.',
  `presence_type` VARCHAR(64) NOT NULL COMMENT '실제 포함·함유 가능·교차오염 가능 등 알레르기 존재 유형.',
  `source_type` VARCHAR(64) NOT NULL COMMENT '수동 입력·알림·위젯·웨어러블·AI 승인 등 데이터 생성 출처.',
  `verified_at` DATETIME(6) COMMENT '해당 음식 알레르기 정보가 검토된 UTC 일시.',
  PRIMARY KEY (`food_id`, `allergen_code`),
  CONSTRAINT `fk_food_allergens_food_id` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_food_allergens_allergen_code` FOREIGN KEY (`allergen_code`) REFERENCES `allergens` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='음식에 포함되거나 포함 가능성이 있는 알레르기 성분과 정보 출처를 관리한다.';

CREATE TABLE `meals` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `habit_occurrence_id` BINARY(16) COMMENT '알림 작업이 연결된 습관 예정 회차 ID.',
  `meal_type` VARCHAR(64) NOT NULL COMMENT '아침·점심·저녁·간식·야식 등 식사 유형.',
  `eaten_at` DATETIME(6) COMMENT '사용자가 실제로 식사한 UTC 일시.',
  `status` VARCHAR(64) NOT NULL COMMENT '예정·완료·건너뜀·미완료 등 식사 기록 상태.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_meals_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meals_habit_occurrence_id` FOREIGN KEY (`habit_occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자의 식사 유형, 섭취시각 및 식사 기록 상태를 관리한다.';

CREATE TABLE `meal_items` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `meal_id` BINARY(16) NOT NULL COMMENT '항목이 포함된 meals.id 외래키.',
  `food_id` BINARY(16) NOT NULL COMMENT '섭취하거나 영양정보를 조회한 foods.id 외래키.',
  `quantity` DECIMAL(18,6) NOT NULL COMMENT '사용자가 실제 섭취한 음식 수량.',
  `unit_code` VARCHAR(128) NOT NULL COMMENT '사용자가 입력한 음식 수량의 unit_definitions.code.',
  `estimated_weight_g` DECIMAL(18,6) COMMENT '수량을 영양 계산용 무게로 환산한 추정치(g).',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_meal_items_meal_id` FOREIGN KEY (`meal_id`) REFERENCES `meals` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meal_items_food_id` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meal_items_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='한 끼 식사에 포함된 음식과 실제 섭취량을 기록한다.';

CREATE TABLE `meal_item_nutrients` (
  `meal_item_id` BINARY(16) NOT NULL COMMENT '영양 스냅샷이 속한 meal_items.id.',
  `nutrient_code` VARCHAR(128) NOT NULL COMMENT '열량·탄수화물·단백질·지방·당·카페인 등 영양소 코드.',
  `amount` DECIMAL(18,6) NOT NULL COMMENT '실제 섭취량으로 환산한 해당 영양소 양.',
  `source_type` VARCHAR(64) NOT NULL COMMENT '공식 영양정보·사용자 입력·AI 추정 등 스냅샷 산출 근거.',
  `calculated_at` DATETIME(6) NOT NULL COMMENT '영양 스냅샷을 계산하여 저장한 UTC 일시.',
  PRIMARY KEY (`meal_item_id`, `nutrient_code`),
  CONSTRAINT `fk_meal_item_nutrients_meal_item_id` FOREIGN KEY (`meal_item_id`) REFERENCES `meal_items` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_meal_item_nutrients_nutrient_code` FOREIGN KEY (`nutrient_code`) REFERENCES `nutrient_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='음식 정보가 나중에 변경되어도 과거 통계가 유지되도록 섭취 당시 영양값을 저장한다.';

CREATE TABLE `user_allergies` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `allergen_code` VARCHAR(128) NOT NULL COMMENT '알레르기 원인 성분을 식별하는 코드.',
  `severity` VARCHAR(64) COMMENT '사용자가 설정한 알레르기 반응 심각도.',
  `reaction_note` TEXT COMMENT '알레르기 노출 시 나타나는 증상이나 주의사항.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_allergies_1` (`user_id`, `allergen_code`),
  CONSTRAINT `fk_user_allergies_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_user_allergies_allergen_code` FOREIGN KEY (`allergen_code`) REFERENCES `allergens` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자의 보유 알레르기와 반응 수준 및 증상을 관리한다.';

CREATE TABLE `medication_products` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `external_code` VARCHAR(128) COMMENT '공공 API·제휴기관 등 외부 시스템에서 사용하는 식별 코드.',
  `kind` VARCHAR(64) NOT NULL COMMENT '처방약·일반의약품·영양제·운동 보충제 분류.',
  `name` VARCHAR(255) NOT NULL COMMENT '사용자 또는 관리 화면에 표시되는 이름.',
  `ingredient_text` TEXT COMMENT '외부 데이터에 있는 성분 표시 원문이며 검색·표시용으로만 사용하고 관계 판단은 성분 테이블을 사용한다.',
  `manufacturer` VARCHAR(255) COMMENT '의약품 또는 보충제 제조·판매 업체명.',
  `dosage_form_code` VARCHAR(128) COMMENT '정제·캡슐·액상·분말 등 dosage_form_definitions.code.',
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사전 항목을 신규 기록에 사용할 수 있는지 여부.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_medication_products_dosage_form_code` FOREIGN KEY (`dosage_form_code`) REFERENCES `dosage_form_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='처방약·일반의약품·영양제·운동 보충제의 공통 제품 사전을 관리한다.';

CREATE TABLE `medication_product_ingredients` (
  `product_id` BINARY(16) NOT NULL COMMENT '사전에 연결된 medication_products.id이며 사용자 직접 입력은 NULL 가능.',
  `ingredient_id` BINARY(16) NOT NULL COMMENT '제품 또는 상호작용의 기준 성분 medication_ingredients.id.',
  `amount` DECIMAL(18,6) COMMENT '해당 행이 표현하는 수량 또는 보상 증감량.',
  `unit_code` VARCHAR(128) COMMENT '수량·복용량·측정값에 사용하는 unit_definitions.code.',
  `basis_quantity` DECIMAL(18,6) COMMENT '성분 또는 영양 함량이 표시되는 제품·음식의 기준 수량.',
  `is_active_ingredient` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '해당 성분이 제품의 주성분인지 여부.',
  PRIMARY KEY (`product_id`, `ingredient_id`),
  CONSTRAINT `fk_medication_product_ingredients_product_id` FOREIGN KEY (`product_id`) REFERENCES `medication_products` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_product_ingredients_ingredient_id` FOREIGN KEY (`ingredient_id`) REFERENCES `medication_ingredients` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_product_ingredients_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='의약품·보충제 제품과 성분의 다대다 관계 및 함량을 관리한다.';

CREATE TABLE `medication_ingredient_interactions` (
  `ingredient_id` BINARY(16) NOT NULL COMMENT '중복 방지를 위해 정렬된 성분 쌍에서 첫 번째 성분 ID.',
  `interacting_ingredient_id` BINARY(16) NOT NULL COMMENT '중복 방지를 위해 정렬된 성분 쌍에서 두 번째 성분 ID.',
  `severity` VARCHAR(64) NOT NULL COMMENT '사용자가 설정한 알레르기 반응 심각도.',
  `description` TEXT NOT NULL COMMENT '항목의 의미·주의사항 또는 상호작용 상세 설명.',
  `evidence_source` VARCHAR(255) COMMENT '상호작용 판단에 사용한 공공데이터·문헌·제조사 정보 출처.',
  PRIMARY KEY (`ingredient_id`, `interacting_ingredient_id`),
  CONSTRAINT `fk_medication_ingredient_interactions_ingredient_id` FOREIGN KEY (`ingredient_id`) REFERENCES `medication_ingredients` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_ingredient_interactions_interacting_ingredient_id` FOREIGN KEY (`interacting_ingredient_id`) REFERENCES `medication_ingredients` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_ingredient_interactions_1` CHECK (`ingredient_id` <> `interacting_ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='두 약물·보충제 성분 사이의 상호작용 수준과 근거를 관리한다.';

CREATE TABLE `prescriptions` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `institution_id` BINARY(16) COMMENT '처방 의료기관 UUID이며 기관 마스터 연계 전에는 NULL 가능.',
  `prescribed_at` DATE COMMENT '처방전이 발행된 날짜.',
  `duration_days` INT COMMENT '처방된 총 복용기간(일).',
  `source_type` VARCHAR(64) NOT NULL COMMENT '사용자 입력·처방전 이미지·QR·기관 연계 등 처방정보 입력 경로.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_prescriptions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_prescriptions_institution_id` FOREIGN KEY (`institution_id`) REFERENCES `healthcare_institutions` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='한 번의 처방에 포함되는 병원·처방일·복용기간 정보를 묶어 관리한다.';

CREATE TABLE `user_medications` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `product_id` BINARY(16) COMMENT '사전에 연결된 medication_products.id이며 사용자 직접 입력은 NULL 가능.',
  `prescription_id` BINARY(16) COMMENT '해당 복약 항목이 포함된 prescriptions.id.',
  `display_name` VARCHAR(255) NOT NULL COMMENT '사용자 화면과 알림에 표시할 약·영양제 이름.',
  `purpose` VARCHAR(255) COMMENT '사용자가 입력한 복용 목적.',
  `started_on` DATE COMMENT '사용자가 복용을 시작한 날짜.',
  `ended_on` DATE COMMENT '복용을 중지하거나 종료한 날짜.',
  `status` VARCHAR(64) NOT NULL COMMENT '복용예정·복용중·일시중지·종료 등 사용자 복약 상태.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_medications_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_user_medications_product_id` FOREIGN KEY (`product_id`) REFERENCES `medication_products` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_user_medications_prescription_id` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자가 실제로 복용 중이거나 종료한 약·영양제·보충제를 관리한다.';

CREATE TABLE `medication_schedules` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_medication_id` BINARY(16) NOT NULL COMMENT '알림·일정·재고가 속한 user_medications.id.',
  `schedule_type` VARCHAR(64) NOT NULL COMMENT '매일·요일·N일 간격·PRN 등 복약 반복 유형.',
  `interval_days` INT COMMENT 'N일마다 복용하는 일정의 일 단위 간격.',
  `is_prn` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '정해진 회차 없이 필요할 때 복용하는 PRN 여부.',
  `timezone` VARCHAR(64) NOT NULL COMMENT '일정 생성과 화면 표시 기준인 IANA 시간대.',
  `effective_from` DATE NOT NULL COMMENT '해당 일정이나 정책 버전이 적용되기 시작하는 날짜.',
  `effective_to` DATE COMMENT '해당 일정이나 정책 버전의 마지막 적용 날짜.',
  `version` INT NOT NULL COMMENT '같은 복약 일정 변경 이력을 구분하는 증가 버전.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_schedules_1` (`user_medication_id`, `version`),
  CONSTRAINT `fk_medication_schedules_user_medication_id` FOREIGN KEY (`user_medication_id`) REFERENCES `user_medications` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_schedules_1` CHECK (`interval_days` IS NULL OR `interval_days` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자 복약 항목의 용량·횟수·식사 관계·유효기간을 버전별로 관리한다.';

CREATE TABLE `medication_schedule_days` (
  `schedule_id` BINARY(16) NOT NULL COMMENT '회차를 생성한 일정 레코드의 외래키.',
  `weekday` INT NOT NULL COMMENT 'ISO 요일 번호로 1은 월요일, 7은 일요일.',
  PRIMARY KEY (`schedule_id`, `weekday`),
  CONSTRAINT `fk_medication_schedule_days_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_schedule_days_1` CHECK (`weekday` BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='요일 기반 복약 일정에서 실제 복용하는 요일을 관리한다.';

CREATE TABLE `medication_schedule_times` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `schedule_id` BINARY(16) NOT NULL COMMENT '회차를 생성한 일정 레코드의 외래키.',
  `sequence_no` INT NOT NULL COMMENT '같은 일정 안에서 회차의 정렬 및 식별에 사용하는 순번.',
  `local_time` TIME COMMENT '사용자 시간대에서 습관을 수행하기로 한 현지 시각.',
  `dose_amount` DECIMAL(18,6) NOT NULL COMMENT '한 회차에 예정된 복용량.',
  `dose_unit_code` VARCHAR(128) NOT NULL COMMENT '복약 회차 용량이 사용하는 unit_definitions.code.',
  `meal_relation` VARCHAR(64) COMMENT '식전·식후·공복·무관 등 식사와의 관계.',
  `offset_minutes` INT COMMENT '기준시각 대비 최초 알림 발송 시각의 분 단위 차이.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_schedule_times_1` (`schedule_id`, `sequence_no`),
  CONSTRAINT `fk_medication_schedule_times_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_schedule_times_dose_unit_code` FOREIGN KEY (`dose_unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_medication_schedule_times_1` CHECK (`sequence_no` > 0),
  CONSTRAINT `ck_medication_schedule_times_2` CHECK (`dose_amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='복약 일정의 회차별 현지시각·복용량·단위·식사 관계를 관리한다.';

CREATE TABLE `medication_dose_occurrences` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `schedule_id` BINARY(16) NOT NULL COMMENT '회차를 생성한 일정 레코드의 외래키.',
  `scheduled_at` DATETIME(6) NOT NULL COMMENT '해당 회차나 작업이 예정된 UTC 일시.',
  `current_status` VARCHAR(64) NOT NULL COMMENT '예정·복용·건너뜀·약없음·미확인·취소 중 최신 상태.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  `updated_at` DATETIME(6) NOT NULL COMMENT '레코드가 마지막으로 변경된 UTC 일시.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_dose_occurrences_1` (`schedule_id`, `scheduled_at`),
  CONSTRAINT `fk_medication_dose_occurrences_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='복약 일정에서 생성된 개별 예정 복용 회차와 현재 상태를 관리한다.';

CREATE TABLE `medication_intake_events` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `occurrence_id` BINARY(16) NOT NULL COMMENT '행동이 발생한 개별 예정 회차의 외래키.',
  `action` VARCHAR(64) NOT NULL COMMENT '복용·건너뜀·약없음·완료취소·용량수정 등 실제 복약 행동.',
  `actual_at` DATETIME(6) COMMENT '사용자가 실제 행동했다고 입력한 UTC 일시.',
  `recorded_at` DATETIME(6) NOT NULL COMMENT '사용자가 앱에 기록을 저장한 UTC 일시.',
  `actual_amount` DECIMAL(18,6) COMMENT '해당 회차에서 실제로 복용한 양.',
  `unit_code` VARCHAR(128) COMMENT '실제 복용량의 unit_definitions.code.',
  `memo` TEXT COMMENT '복용 후 증상·부작용 등 사용자가 입력한 메모.',
  `idempotency_key` VARCHAR(255) COMMENT '네트워크 재시도 시 동일 기록의 중복 생성을 방지하는 키.',
  `previous_event_id` BINARY(16) COMMENT '수정 또는 취소 대상이 된 이전 이벤트의 자기참조 외래키.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medication_intake_events_1` (`idempotency_key`),
  KEY `ix_medication_intake_events_1` (`occurrence_id`, `recorded_at`),
  CONSTRAINT `fk_medication_intake_events_occurrence_id` FOREIGN KEY (`occurrence_id`) REFERENCES `medication_dose_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_intake_events_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_medication_intake_events_previous_event_id` FOREIGN KEY (`previous_event_id`) REFERENCES `medication_intake_events` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='실제 복용·건너뜀·약 없음·수정 등 복약 회차 행동 이력을 저장한다.';

CREATE TABLE `medication_inventory_transactions` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_medication_id` BINARY(16) NOT NULL COMMENT '알림·일정·재고가 속한 user_medications.id.',
  `transaction_type` VARCHAR(64) NOT NULL COMMENT '초기재고·추가입고·복용차감·수정·폐기 등 재고 거래 유형.',
  `quantity_delta` DECIMAL(18,6) NOT NULL COMMENT '재고를 증가시키거나 감소시키는 부호 있는 수량.',
  `unit_code` VARCHAR(128) NOT NULL COMMENT '재고 거래 수량의 unit_definitions.code.',
  `source_event_id` BINARY(16) COMMENT '재고 증감의 원인이 된 복약 이벤트 식별자.',
  `occurred_at` DATETIME(6) NOT NULL COMMENT '이벤트·거래·감사 행위가 실제 발생한 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_medication_inventory_transactions_user_medication_id` FOREIGN KEY (`user_medication_id`) REFERENCES `user_medications` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_medication_inventory_transactions_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='입고·실제 복용·수정·폐기 등 복약 재고 증감 원장을 관리한다.';

CREATE TABLE `observation_sessions` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `measured_at` DATETIME(6) NOT NULL COMMENT '신체측정을 실시한 UTC 일시.',
  `source_type` VARCHAR(64) NOT NULL COMMENT '사용자 수동입력·기기·웨어러블·기관연계 등 측정 출처.',
  `external_record_id` VARCHAR(255) COMMENT '웨어러블 등 외부 제공자의 원본 레코드 식별자.',
  `context_json` JSON COMMENT '공복·식후·측정자세·기기 등 측정 조건 JSON.',
  PRIMARY KEY (`id`),
  KEY `ix_observation_sessions_1` (`user_id`, `measured_at`),
  CONSTRAINT `fk_observation_sessions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='같은 시점과 출처에서 입력된 신체측정 값들을 하나의 측정 묶음으로 관리한다.';

CREATE TABLE `health_observations` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `session_id` BINARY(16) NOT NULL COMMENT '해당 상세 레코드가 속한 세션의 외래키.',
  `type_code` VARCHAR(128) NOT NULL COMMENT '체중·체지방·수축기혈압·혈당 등 관측값 유형 코드.',
  `numeric_value` DECIMAL(18,6) COMMENT '수치로 표현되는 측정값.',
  `text_value` VARCHAR(255) COMMENT '수치로 표현하기 어려운 범주형 또는 원문 측정값.',
  `unit_code` VARCHAR(128) COMMENT '측정항목에서 허용된 unit_definitions.code이며 type_code와의 조합도 검증한다.',
  `created_at` DATETIME(6) NOT NULL COMMENT '측정값 레코드가 DB에 저장된 UTC 일시.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_health_observations_1` (`session_id`, `type_code`),
  CONSTRAINT `fk_health_observations_session_id` FOREIGN KEY (`session_id`) REFERENCES `observation_sessions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_health_observations_type_code` FOREIGN KEY (`type_code`) REFERENCES `observation_types` (`code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_health_observations_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_health_observations_type_unit` FOREIGN KEY (`type_code`, `unit_code`) REFERENCES `observation_type_units` (`observation_type_code`, `unit_code`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_health_observations_1` CHECK (((`numeric_value` IS NOT NULL) + (`text_value` IS NOT NULL)) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='체중·체지방·혈압·혈당·심박·체온 등 개별 신체측정 값을 저장한다.';

CREATE TABLE `notification_rules` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `offset_minutes` INT NOT NULL COMMENT '기준시각 대비 최초 알림 발송 시각의 분 단위 차이.',
  `max_retries` INT NOT NULL DEFAULT 0 COMMENT '최대 재알림 발송 횟수.',
  `retry_interval_minutes` INT COMMENT '재알림 사이의 분 단위 간격.',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '규칙 또는 기능의 사용 여부.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_notification_rules_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='습관·복약 등 대상별 최초 알림과 재알림 정책을 관리한다.';

CREATE TABLE `notification_rule_habit_targets` (
  `rule_id` BINARY(16) NOT NULL COMMENT '대상 연결 또는 발송 작업이 속한 notification_rules.id.',
  `habit_id` BINARY(16) NOT NULL COMMENT '알림 또는 일정의 대상 habits.id.',
  PRIMARY KEY (`rule_id`, `habit_id`),
  CONSTRAINT `fk_notification_rule_habit_targets_rule_id` FOREIGN KEY (`rule_id`) REFERENCES `notification_rules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_notification_rule_habit_targets_habit_id` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='알림 정책과 적용 대상 습관의 다대다 관계를 관리한다.';

CREATE TABLE `notification_rule_medication_targets` (
  `rule_id` BINARY(16) NOT NULL COMMENT '대상 연결 또는 발송 작업이 속한 notification_rules.id.',
  `user_medication_id` BINARY(16) NOT NULL COMMENT '알림·일정·재고가 속한 user_medications.id.',
  PRIMARY KEY (`rule_id`, `user_medication_id`),
  CONSTRAINT `fk_notification_rule_medication_targets_rule_id` FOREIGN KEY (`rule_id`) REFERENCES `notification_rules` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_notification_rule_medication_targets_user_medication_id` FOREIGN KEY (`user_medication_id`) REFERENCES `user_medications` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='알림 정책과 적용 대상 사용자 복약 항목의 다대다 관계를 관리한다.';

CREATE TABLE `notification_jobs` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `rule_id` BINARY(16) COMMENT '대상 연결 또는 발송 작업이 속한 notification_rules.id.',
  `habit_occurrence_id` BINARY(16) COMMENT '습관 알림이면 연결되는 habit_occurrences.id이며 복약 알림이면 NULL.',
  `medication_dose_occurrence_id` BINARY(16) COMMENT '복약 알림이면 연결되는 medication_dose_occurrences.id이며 습관 알림이면 NULL.',
  `job_type` VARCHAR(64) NOT NULL COMMENT 'AI 작업의 종류.',
  `scheduled_at` DATETIME(6) NOT NULL COMMENT '해당 회차나 작업이 예정된 UTC 일시.',
  `status` VARCHAR(64) NOT NULL COMMENT '대기·처리중·발송완료·취소·실패 등 알림 작업 상태.',
  `retry_no` INT NOT NULL DEFAULT 0 COMMENT '최초 발송을 0으로 하는 재알림 차수.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  PRIMARY KEY (`id`),
  KEY `ix_notification_jobs_1` (`status`, `scheduled_at`),
  UNIQUE KEY `uq_notification_jobs_1` (`habit_occurrence_id`, `job_type`, `retry_no`),
  UNIQUE KEY `uq_notification_jobs_2` (`medication_dose_occurrence_id`, `job_type`, `retry_no`),
  CONSTRAINT `fk_notification_jobs_rule_id` FOREIGN KEY (`rule_id`) REFERENCES `notification_rules` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_notification_jobs_habit_occurrence_id` FOREIGN KEY (`habit_occurrence_id`) REFERENCES `habit_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_notification_jobs_medication_dose_occurrence_id` FOREIGN KEY (`medication_dose_occurrence_id`) REFERENCES `medication_dose_occurrences` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_notification_jobs_1` CHECK (((`habit_occurrence_id` IS NOT NULL) + (`medication_dose_occurrence_id` IS NOT NULL)) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='발송 예정인 개별 알림 작업과 재시도 차수를 관리한다.';

CREATE TABLE `notification_deliveries` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `job_id` BINARY(16) NOT NULL COMMENT '발송 결과가 속한 notification_jobs.id.',
  `channel` VARCHAR(64) NOT NULL COMMENT '푸시·이메일·SMS 등 발송 채널.',
  `provider_message_id` VARCHAR(255) COMMENT '외부 알림 제공자가 반환한 메시지 식별자.',
  `status` VARCHAR(64) NOT NULL COMMENT '요청·발송·전달·열람·실패 등 제공자 발송 상태.',
  `sent_at` DATETIME(6) COMMENT '외부 제공자에게 발송을 요청한 UTC 일시.',
  `delivered_at` DATETIME(6) COMMENT '사용자 기기에 전달된 것으로 확인된 UTC 일시.',
  `opened_at` DATETIME(6) COMMENT '사용자가 알림을 열거나 상호작용한 UTC 일시.',
  `error_code` VARCHAR(128) COMMENT '발송 실패 원인을 분류하는 내부 또는 제공자 오류 코드.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_notification_deliveries_1` (`provider_message_id`),
  CONSTRAINT `fk_notification_deliveries_job_id` FOREIGN KEY (`job_id`) REFERENCES `notification_jobs` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='푸시 등 외부 알림 제공자에게 실제로 발송한 결과와 열람 시각을 기록한다.';

CREATE TABLE `goals` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `category_code` VARCHAR(128) NOT NULL COMMENT '습관·운동·목표·보상 영역의 분류 코드.',
  `goal_type` VARCHAR(64) NOT NULL COMMENT '횟수·기간·수치 증감·범위 유지 등 목표 유형.',
  `target_value` DECIMAL(18,6) COMMENT '사용자가 달성하려는 목표 수치.',
  `unit_code` VARCHAR(128) COMMENT '목표값의 unit_definitions.code.',
  `started_on` DATE NOT NULL COMMENT '사용자가 복용을 시작한 날짜.',
  `ended_on` DATE COMMENT '복용을 중지하거나 종료한 날짜.',
  `status` VARCHAR(64) NOT NULL COMMENT '초안·진행중·달성·중단·종료 등 목표 상태.',
  `created_at` DATETIME(6) NOT NULL COMMENT '레코드가 최초 생성된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_goals_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_goals_unit_code` FOREIGN KEY (`unit_code`) REFERENCES `unit_definitions` (`code`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='운동·식사·체중·습관 등 사용자의 장기·단기 건강 목표를 관리한다.';

CREATE TABLE `reward_ledger` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `category_code` VARCHAR(128) COMMENT '습관·운동·목표·보상 영역의 분류 코드.',
  `reward_type` VARCHAR(64) NOT NULL COMMENT '경험치·포인트·아이템 등 보상 자산 유형.',
  `amount` DECIMAL(18,6) NOT NULL COMMENT '보상 지급은 양수, 취소·회수는 음수로 기록하는 거래 수량.',
  `reason_type` VARCHAR(64) NOT NULL COMMENT '습관·미션·업적·챌린지 등 보상 발생 사유 유형.',
  `reason_id` BINARY(16) COMMENT '보상을 발생시킨 업무 레코드 UUID.',
  `reversal_of_id` BINARY(16) COMMENT '취소하는 원거래 reward_ledger.id.',
  `occurred_at` DATETIME(6) NOT NULL COMMENT '이벤트·거래·감사 행위가 실제 발생한 UTC 일시.',
  PRIMARY KEY (`id`),
  KEY `ix_reward_ledger_1` (`user_id`, `occurred_at`),
  CONSTRAINT `fk_reward_ledger_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_reward_ledger_reversal_of_id` FOREIGN KEY (`reversal_of_id`) REFERENCES `reward_ledger` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='경험치·포인트 지급과 취소를 수정 불가능한 원장 거래로 관리한다.';

CREATE TABLE `analytics_events` (
  `event_id` BINARY(16) NOT NULL COMMENT '클라이언트와 서버에서 중복 없이 생성한 분석 이벤트 UUID.',
  `occurred_at` DATETIME(6) NOT NULL COMMENT '이벤트·거래·감사 행위가 실제 발생한 UTC 일시.',
  `received_at` DATETIME(6) NOT NULL COMMENT '서버가 분석 이벤트를 수신한 UTC 일시.',
  `user_id` BINARY(16) COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `anonymous_id` VARCHAR(255) COMMENT '로그인 전 사용자를 동일 기기 범위에서 구분하는 비식별 키.',
  `session_id` BINARY(16) COMMENT '같은 앱 사용 흐름을 묶는 분석 세션 UUID.',
  `event_name` VARCHAR(128) NOT NULL COMMENT '분석 스키마에 등록된 행동 이벤트명.',
  `screen_name` VARCHAR(128) COMMENT '이벤트가 발생한 앱 화면 또는 기능 영역.',
  `properties_json` JSON NOT NULL COMMENT '이벤트별 부가 속성을 저장하는 JSON이며 민감정보 원문은 제외.',
  `schema_version` INT NOT NULL COMMENT '이벤트 속성 구조의 버전.',
  PRIMARY KEY (`event_id`, `occurred_at`),
  KEY `ix_analytics_events_1` (`user_id`, `occurred_at`),
  KEY `ix_analytics_events_2` (`event_name`, `occurred_at`),
  CONSTRAINT `fk_analytics_events_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자 행동·화면·알림 반응 등 제품 분석용 원시 이벤트를 저장한다.';

CREATE TABLE `ai_jobs` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `job_type` VARCHAR(64) NOT NULL COMMENT 'AI 작업의 종류.',
  `status` VARCHAR(64) NOT NULL COMMENT '대기·처리중·완료·실패·취소 등 AI 비동기 작업 상태.',
  `model_provider` VARCHAR(255) COMMENT 'AI 모델 제공자 코드.',
  `model_name` VARCHAR(255) COMMENT '요청에 사용한 모델 식별명.',
  `prompt_version` VARCHAR(255) COMMENT '프롬프트·후처리 규칙의 배포 버전.',
  `requested_at` DATETIME(6) NOT NULL COMMENT 'AI 작업을 요청한 UTC 일시.',
  `completed_at` DATETIME(6) COMMENT 'AI 작업 처리가 종료된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ai_jobs_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='음식·약 사진 분석, OCR, 추천, 요약 등 비동기 AI 작업을 관리한다.';

CREATE TABLE `ai_job_outputs` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `job_id` BINARY(16) NOT NULL COMMENT '결과가 속한 ai_jobs.id 외래키.',
  `output_json` JSON NOT NULL COMMENT 'AI가 반환한 후보·분류·요약 등 구조화 결과 JSON.',
  `confidence` DECIMAL(18,6) COMMENT 'AI 결과의 신뢰도 또는 모델 점수.',
  `safety_flags` JSON COMMENT '의료 안전·민감정보·저신뢰 등 검토 플래그 JSON.',
  `accepted_at` DATETIME(6) COMMENT '사용자가 AI 결과를 확인하고 채택한 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ai_job_outputs_job_id` FOREIGN KEY (`job_id`) REFERENCES `ai_jobs` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='AI 작업의 구조화 결과와 신뢰도, 안전 플래그, 사용자 채택 여부를 저장한다.';

CREATE TABLE `integration_connections` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `provider` VARCHAR(64) NOT NULL COMMENT 'Health Connect·Garmin·Google Calendar 등 외부 제공자 코드.',
  `status` VARCHAR(64) NOT NULL COMMENT '연결중·연결됨·오류·해제 등 외부 연동 상태.',
  `scopes` JSON NOT NULL COMMENT '사용자가 허용한 외부 데이터 접근 범위 JSON.',
  `token_reference` VARCHAR(255) COMMENT '평문 토큰 대신 비밀 저장소의 인증정보를 가리키는 참조키.',
  `connected_at` DATETIME(6) NOT NULL COMMENT '외부 서비스 또는 기관 연결이 성립한 UTC 일시.',
  `disconnected_at` DATETIME(6) COMMENT '외부 서비스 또는 기관 연결이 해제된 UTC 일시.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_integration_connections_1` (`user_id`, `provider`),
  CONSTRAINT `fk_integration_connections_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='Health Connect·웨어러블·캘린더 등 외부 제공자와의 사용자 연결을 관리한다.';

CREATE TABLE `external_record_mappings` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `connection_id` BINARY(16) NOT NULL COMMENT '매핑이 속한 integration_connections.id 외래키.',
  `external_id` VARCHAR(255) NOT NULL COMMENT '외부 제공자가 부여한 원본 레코드 식별자.',
  `internal_type` VARCHAR(64) NOT NULL COMMENT '연결된 내부 레코드의 테이블·리소스 유형.',
  `internal_id` BINARY(16) NOT NULL COMMENT '외부 레코드와 연결된 내부 레코드 UUID.',
  `source_updated_at` DATETIME(6) COMMENT '외부 제공자 기준으로 원본이 마지막 변경된 UTC 일시.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_external_record_mappings_1` (`connection_id`, `external_id`),
  CONSTRAINT `fk_external_record_mappings_connection_id` FOREIGN KEY (`connection_id`) REFERENCES `integration_connections` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='외부 제공자의 레코드와 서비스 내부 레코드를 연결하여 중복 수집을 방지한다.';

CREATE TABLE `healthcare_connections` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `user_id` BINARY(16) NOT NULL COMMENT '해당 데이터의 소유 회원을 식별하는 users.id 외래키.',
  `institution_id` BINARY(16) NOT NULL COMMENT '연결 대상 healthcare_institutions.id 외래키.',
  `status` VARCHAR(64) NOT NULL COMMENT '요청·연결됨·일시중지·해제 등 사용자 기관 연계 상태.',
  `connected_at` DATETIME(6) COMMENT '외부 서비스 또는 기관 연결이 성립한 UTC 일시.',
  `disconnected_at` DATETIME(6) COMMENT '외부 서비스 또는 기관 연결이 해제된 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_healthcare_connections_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_healthcare_connections_institution_id` FOREIGN KEY (`institution_id`) REFERENCES `healthcare_institutions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='사용자와 의료기관 사이의 연계 상태와 연결·해제 시각을 관리한다.';

CREATE TABLE `healthcare_data_consents` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `connection_id` BINARY(16) NOT NULL COMMENT '동의가 적용되는 healthcare_connections.id 외래키.',
  `purpose` VARCHAR(255) NOT NULL COMMENT '의료기관에 데이터를 제공하거나 수신하는 구체적 이용 목적.',
  `data_categories` JSON NOT NULL COMMENT '제공·수신에 동의한 건강데이터 범위 JSON.',
  `valid_from` DATETIME(6) NOT NULL COMMENT '동의 또는 연결 권한이 유효해지는 UTC 일시.',
  `valid_to` DATETIME(6) COMMENT '동의 또는 연결 권한의 만료 UTC 일시.',
  `withdrawn_at` DATETIME(6) COMMENT '사용자가 동의를 철회한 UTC 일시.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_healthcare_data_consents_connection_id` FOREIGN KEY (`connection_id`) REFERENCES `healthcare_connections` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='의료기관 연계 시 목적·데이터 범위·유효기간별 사용자 동의를 관리한다.';

CREATE TABLE `audit_logs` (
  `id` BINARY(16) NOT NULL COMMENT '레코드를 식별하는 UUID 기반 BINARY(16) 기본키.',
  `actor_id` BINARY(16) COMMENT '행위를 수행한 사용자·관리자·시스템 주체 UUID.',
  `actor_type` VARCHAR(64) NOT NULL COMMENT '사용자·관리자·백엔드 작업 등 행위 주체 유형.',
  `action` VARCHAR(64) NOT NULL COMMENT '조회·등록·수정·삭제·다운로드·외부전송 등 감사 대상 행위.',
  `resource_type` VARCHAR(64) NOT NULL COMMENT '연결 대상 리소스의 종류를 구분하는 코드.',
  `resource_id` BINARY(16) COMMENT '감사 대상 업무 레코드 UUID이며 특정 레코드가 없으면 NULL.',
  `occurred_at` DATETIME(6) NOT NULL COMMENT '이벤트·거래·감사 행위가 실제 발생한 UTC 일시.',
  `result` VARCHAR(64) NOT NULL COMMENT '성공·거부·실패 등 감사 대상 행위의 처리 결과.',
  `metadata_json` JSON COMMENT '요청 경로·사유·오류 등 감사에 필요한 비민감 부가정보 JSON.',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='민감정보 조회·변경·전송 등 보안 감사 대상 행위를 변경 불가능한 로그로 보관한다.';
