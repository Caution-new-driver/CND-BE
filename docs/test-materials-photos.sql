-- 실사 사진이 붙은 QA 테스트 소재 20개.
-- docs/test-materials-qa.sql(매칭점수/등급/상태 필터용)과는 별개로,
-- b12 제작가능성 계산(면적·배치)과 소재 종류(materialType) 5개 enum 전부를
-- 다양한 가로/세로/수량 조합으로 커버하도록 설계됨. 대화에서 합의한
-- 최종 표(코드/타입/등급/치수) 그대로 반영.
--
-- 이미지는 Unsplash 무료 라이선스(images.unsplash.com, 상업적 이용 가능·출처 표기 불필요)
-- 사진을 소재 종류/색상에 맞춰 매칭한 것으로, 실제 next:R.U.N 소재 사진이 아님.
-- color/pattern은 실제 AI 태깅(POST /api/materials/{id}/ai-tag) 결과가 아니라
-- 사진 내용을 보고 사람이 직접 지정한 값 — ai_confidence는 그래서 NULL로 둠
-- (진짜 AI 태깅값과 섞여 혼동되지 않도록).
--
-- material_code 기준으로 이미 있으면 건너뛰므로 재실행해도 안전함.

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'LTH-101', 'LEATHER', 'BLACK', 'SOLID',
       '거친 가죽결', NULL, NULL,
       'B', 600, 500, 1.8, '거칠고 단단함', '뻣뻣함', 4,
       'https://images.unsplash.com/photo-1546872003-917e15185482', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'LTH-101');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'SYN-101', 'SYNTHETIC', 'BLACK', 'SOLID',
       '매끈한 코팅면', NULL, NULL,
       'A', 420, 210, 0.6, '매끈하고 촉촉한 느낌', '유연함', 1,
       'https://images.unsplash.com/photo-1584384689201-e0bcbe2c7f1d', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'SYN-101');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'LTH-102', 'LEATHER', 'BROWN', 'SOLID',
       '부드러운 가죽결', NULL, NULL,
       'A', 550, 420, 1.4, '부드럽고 매끈함', '유연함', 3,
       'https://images.unsplash.com/photo-1778883008356-f7b3789a3073', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'LTH-102');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'SYN-102', 'SYNTHETIC', 'BROWN', 'SOLID',
       '펄감 있는 코팅면', NULL, NULL,
       'B', 420, 220, 0.9, '약간 거침, 펄감 있음', '보통', 1,
       'https://images.unsplash.com/photo-1573227897444-860137a0fe74', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'SYN-102');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'LTH-103', 'LEATHER', 'BROWN', 'SOLID',
       '스웨이드 결', NULL, NULL,
       'B', 170, 130, 1.1, '부드러운 스웨이드감', '유연함', 60,
       'https://images.unsplash.com/photo-1576792741377-eb0f4f6d1a47', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'LTH-103');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'FAB-101', 'FABRIC', 'MULTI', 'GEOMETRIC',
       '컬러풀한 패턴', NULL, NULL,
       'A', 500, 400, 0.5, '매끈함', '유연함', 3,
       'https://images.unsplash.com/photo-1723283126778-c16ae4c2b0c9', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'FAB-101');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'CVS-101', 'COATED_CANVAS', 'RED', 'SOLID',
       '평직 우븐', NULL, NULL,
       'B', 420, 210, 0.7, '까끌까끌함', '보통', 1,
       'https://images.unsplash.com/photo-1671530191715-b1019db3944a', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'CVS-101');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'FAB-102', 'FABRIC', 'BLUE', 'SOLID',
       '니트 질감', NULL, NULL,
       'C', 430, 210, 0.8, '도톰하고 거침', '보통', 1,
       'https://images.unsplash.com/photo-1653580373915-4ddcfa0699eb', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'FAB-102');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'OTH-101', 'OTHER', 'BLUE', 'STRIPE',
       '세로 줄무늬', NULL, NULL,
       'A', 600, 500, 1.0, '단단하고 매끈함', '뻣뻣함', 5,
       'https://images.unsplash.com/photo-1627954051861-c0b7d448f148', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'OTH-101');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'FAB-103', 'FABRIC', 'MULTI', 'STRIPE',
       '방사형 결', NULL, NULL,
       'A', 180, 140, 0.4, '매끈하고 광택있음', '매우 유연함', 40,
       'https://images.unsplash.com/photo-1544038788-bd43060ab9eb', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'FAB-103');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'LTH-104', 'LEATHER', 'BEIGE', 'SOLID',
       '무광 매트', NULL, NULL,
       'B', 480, 400, 1.2, '부드러움', '보통', 4,
       'https://images.unsplash.com/photo-1538645731800-4640c639bba7', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'LTH-104');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'CVS-102', 'COATED_CANVAS', 'OTHER', 'SOLID',
       '데님 위빙', NULL, NULL,
       'C', 440, 230, 0.7, '약간 거침', '보통', 1,
       'https://images.unsplash.com/photo-1761766319959-70f832bee4a0', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'CVS-102');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'FAB-104', 'FABRIC', 'WHITE', 'SOLID',
       '실키한 드레이프', NULL, NULL,
       'A', 520, 420, 0.3, '부드럽고 매끄러움', '매우 유연함', 3,
       'https://images.unsplash.com/photo-1732869415090-179de017b6d6', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'FAB-104');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'SYN-103', 'SYNTHETIC', 'WHITE', 'SOLID',
       '새틴 광택', NULL, NULL,
       'A', 420, 210, 0.4, '매끈하고 광택 있음', '유연함', 1,
       'https://images.unsplash.com/photo-1619043519379-99df2736108d', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'SYN-103');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'LTH-105', 'LEATHER', 'BROWN', 'SOLID',
       '고급 가죽결', NULL, NULL,
       'A', 550, 450, 1.6, '고급스럽고 탄탄함', '보통', 3,
       'https://images.unsplash.com/photo-1751267453227-b221dddf9c84', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'LTH-105');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'FAB-105', 'FABRIC', 'MULTI', 'GEOMETRIC',
       '물결 체크', NULL, NULL,
       'B', 190, 150, 0.6, '약간 뻣뻣함', '보통', 50,
       'https://images.unsplash.com/photo-1758546407134-42b017d5f011', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'FAB-105');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'CVS-103', 'COATED_CANVAS', 'MULTI', 'GEOMETRIC',
       '다이아몬드 격자', NULL, NULL,
       'B', 410, 220, 0.9, '도톰함', '뻣뻣함', 1,
       'https://images.unsplash.com/photo-1634225029989-f7933af70425', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'CVS-103');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'SYN-104', 'SYNTHETIC', 'MULTI', 'GEOMETRIC',
       '체커보드 패턴', NULL, NULL,
       'A', 580, 480, 0.5, '매끈함', '유연함', 4,
       'https://images.unsplash.com/photo-1703929755159-a1a8835a0536', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'SYN-104');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'OTH-102', 'OTHER', 'MULTI', 'GEOMETRIC',
       '그래픽 패턴', NULL, NULL,
       'C', 450, 210, 1.0, '거친 벽돌 질감', '뻣뻣함', 1,
       'https://images.unsplash.com/photo-1629184992954-5079ba28e66a', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'OTH-102');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'CVS-104', 'COATED_CANVAS', 'WHITE', 'SOLID',
       '무지 캔버스', NULL, NULL,
       'A', 550, 450, 0.6, '매끈하고 살짝 도톰함', '보통', 3,
       'https://images.unsplash.com/photo-1601662528567-526cd06f6582', NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'CVS-104');
