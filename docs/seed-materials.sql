-- MaterialSeeder.java(b5)와 동일한 시드 소재 5개를 수동으로 채워 넣는 스크립트.
-- 공유 Neon 개발 DB에 이미 다른 테스트 데이터가 있어서 앱 자동 시더(count() > 0이면 skip)가
-- 더 이상 동작하지 않을 때, LTH-001 등 원래 시드 데이터를 복구하기 위해 사용.
-- material_code 기준으로 이미 존재하면 다시 넣지 않도록 가드를 걸어 재실행해도 안전함.

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'LTH-001', 'LEATHER', 'BLACK', 'SOLID',
       NULL, NULL, NULL,
       'A', 300, 200, 1.2, '부드러움', '유연함', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'LTH-001');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'LTH-002', 'LEATHER', 'BROWN', 'MONOGRAM',
       NULL, NULL, NULL,
       'B', 250, 180, 1.5, '약간 거침', '보통', 1,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'LTH-002');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'CVS-001', 'COATED_CANVAS', 'BEIGE', 'STRIPE',
       NULL, NULL, NULL,
       'A', 400, 300, 0.8, '매끈함', '유연함', 3,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'CVS-001');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'FAB-001', 'FABRIC', 'RED', 'GEOMETRIC',
       NULL, NULL, NULL,
       'C', 200, 150, 0.5, '부드러움', '매우 유연함', 1,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'FAB-001');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'SYN-001', 'SYNTHETIC', 'WHITE', 'SOLID',
       NULL, NULL, NULL,
       'B', 350, 250, 1.0, '매끈함', '보통', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'SYN-001');
