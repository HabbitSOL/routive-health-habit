INSERT INTO dosage_form_definitions (code, name) VALUES
    ('TABLET', '정제'),
    ('CAPSULE', '캡슐'),
    ('LIQUID', '액제');

INSERT INTO medication_products (
    id, external_code, kind, name, normalized_name, ingredient_text,
    manufacturer, dosage_form_code, efficacy, usage_instructions,
    precautions, side_effects, storage_method, color, shape, score_line,
    source_name, source_updated_at
) VALUES
(
    UNHEX(REPLACE('0193c0d1-9b11-7d42-9b3d-b7f9ccac82b5', '-', '')),
    'DEMO-DRUG-001', 'OTC', '예시 해열진통제 500mg', '예시 해열진통제 500mg',
    '아세트아미노펜 500mg', '예시제약', 'TABLET',
    '감기로 인한 발열 및 통증 완화에 사용하는 예시 데이터입니다.',
    '제품 설명서 또는 전문가의 안내에 따라 복용합니다.',
    '다른 아세트아미노펜 함유 제품과 중복 복용하지 마세요.',
    '과민반응 등 이상 증상이 나타나면 복용을 중단하고 전문가와 상담하세요.',
    '실온의 건조한 곳에 보관합니다.', '흰색', '원형', '없음',
    '개발용 시연 데이터', '2026-10-01 00:00:00.000000'
),
(
    UNHEX(REPLACE('0193c0d1-9b11-7d42-9b3d-b7f9ccac82c6', '-', '')),
    'DEMO-SUPPLEMENT-001', 'SUPPLEMENT', '예시 비타민C 1000mg', '예시 비타민c 1000mg',
    '비타민C 1000mg', '예시건강', 'TABLET',
    '비타민C 보충을 위한 개발용 예시 데이터입니다.',
    '제품 표시사항에 따라 섭취합니다.',
    '질환이 있거나 다른 제품을 복용 중이면 전문가와 상담하세요.',
    '개인에 따라 위장 불편 등이 나타날 수 있습니다.',
    '직사광선을 피해 서늘한 곳에 보관합니다.', '노란색', '타원형', '없음',
    '개발용 시연 데이터', '2026-10-01 00:00:00.000000'
);

INSERT INTO medication_product_ingredients (product_id, name, amount, unit_code, display_order) VALUES
    (UNHEX(REPLACE('0193c0d1-9b11-7d42-9b3d-b7f9ccac82b5', '-', '')), '아세트아미노펜', 500, 'MG', 0),
    (UNHEX(REPLACE('0193c0d1-9b11-7d42-9b3d-b7f9ccac82c6', '-', '')), '비타민C', 1000, 'MG', 0);
