INSERT INTO dosage_form_definitions (code, name, active) VALUES ('QA_TABLET', '테스트 정제', 1);

INSERT INTO medication_products
    (id, kind, name, normalized_name, ingredient_text, manufacturer, dosage_form_code,
     efficacy, usage_instructions, precautions, side_effects, storage_method,
     color, shape, score_line, source_name, source_updated_at, active)
VALUES
    (X'11111111111141118111111111111111', 'OTC', 'QA Alpha50% Formula', 'qa alpha50% formula',
     '테스트성분A 500mg', 'QA Pharma', 'QA_TABLET', '테스트 효능', '테스트 사용법',
     '테스트 주의사항', '테스트 부작용', '테스트 보관법', '흰색', '원형', '없음',
     '자동 테스트 전용', NULL, 1),
    (X'22222222222242228222222222222222', 'OTC', 'QA Beta_One', 'qa beta_one',
     NULL, 'QA Pharma', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     '자동 테스트 전용', NULL, 1),
    (X'33333333333343338333333333333333', 'PRESCRIPTION', 'QA Twin', 'qa twin',
     '테스트성분C', 'QA Pharma', 'QA_TABLET', NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, '자동 테스트 전용', NULL, 1),
    (X'44444444444444448444444444444444', 'PRESCRIPTION', 'QA Twin', 'qa twin',
     '테스트성분D', 'QA Pharma', 'QA_TABLET', NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, '자동 테스트 전용', NULL, 1),
    (X'55555555555545558555555555555555', 'OTC', 'QA Hidden', 'qa hidden',
     '테스트성분A', 'QA Pharma', 'QA_TABLET', NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, '자동 테스트 전용', NULL, 0),
    (X'66666666666646668666666666666666', 'SUPPLEMENT', 'QA Bang!', 'qa bang!',
     NULL, 'QA Pharma', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     '자동 테스트 전용', NULL, 1);

INSERT INTO medication_product_ingredients (product_id, name, amount, unit_code, display_order)
VALUES
    (X'11111111111141118111111111111111', '두번째성분', 5, 'MG', 1),
    (X'11111111111141118111111111111111', '첫번째성분', 500, 'MG', 0);
