-- QA/통합 테스트용 소재 데이터셋 (docs/seed-materials.sql의 기본 5개와는 별개).
-- 기본 시드(LTH-001 등)는 "화면에 뭔가 보이게" 하는 최소 세트고,
-- 이 파일은 b9(후보 필터·매칭점수)·b12(제작 가능성 계산)의 판단 분기를
-- 하나씩 의도적으로 건드리도록 설계한 경계값/예외 케이스 세트다.
-- material_code 기준으로 이미 존재하면 다시 넣지 않으므로 재실행해도 안전함.
--
-- 정리하고 싶으면: DELETE FROM material WHERE material_code LIKE 'QA-%';

-- ============================================================
-- A. 매칭 점수 테스트 (MaterialMatchScorer) — DesignRequirement에
--    color=BLACK, pattern=SOLID를 넣고 비교하면 아래 4건이 각각
--    100점 / 50점(색상만) / 50점(패턴만) / 0점을 만든다.
-- ============================================================

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-MATCH-FULL', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.92, '균일한 표면, 흠집 없음',
       'A', 350, 250, 1.2, '부드러움', '유연함', 3,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-MATCH-FULL');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-MATCH-COLOR-ONLY', 'LEATHER', 'BLACK', 'STRIPE',
       '매끈함', 0.88, NULL,
       'A', 350, 250, 1.2, '부드러움', '유연함', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-MATCH-COLOR-ONLY');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-MATCH-PATTERN-ONLY', 'COATED_CANVAS', 'RED', 'SOLID',
       '매끈함', 0.85, NULL,
       'B', 350, 250, 1.0, '매끈함', '유연함', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-MATCH-PATTERN-ONLY');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-MATCH-NONE', 'FABRIC', 'BLUE', 'GEOMETRIC',
       '부드러움', 0.80, NULL,
       'B', 300, 220, 0.6, '부드러움', '매우 유연함', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-MATCH-NONE');

-- ============================================================
-- B. 등급(grade) 경계값 테스트 (MaterialCandidateFilter.meetsMinimumGrade)
--    minGrade=B로 검색하면 A/B는 통과, C는 제외되어야 한다.
-- ============================================================

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-GRADE-A', 'SYNTHETIC', 'WHITE', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', 320, 260, 1.0, '매끈함', '보통', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-GRADE-A');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-GRADE-B', 'SYNTHETIC', 'WHITE', 'SOLID',
       '매끈함', 0.9, NULL,
       'B', 320, 260, 1.0, '매끈함', '보통', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-GRADE-B');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-GRADE-C', 'SYNTHETIC', 'WHITE', 'SOLID',
       '매끈함', 0.9, NULL,
       'C', 320, 260, 1.0, '매끈함', '보통', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-GRADE-C');

-- ============================================================
-- C. 상태(status) 필터 테스트 — RESERVED/DEPLETED는 원칙적으로
--    후보 추천에서 빠져야 한다 (RESERVED는 reusableMaterialIds에
--    포함된 경우에만 예외적으로 통과).
-- ============================================================

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-STATUS-RESERVED', 'LEATHER', 'BROWN', 'MONOGRAM',
       '약간 거침', 0.9, NULL,
       'A', 300, 220, 1.3, '약간 거침', '보통', 1,
       NULL, NULL, 'RESERVED',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-STATUS-RESERVED');

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-STATUS-DEPLETED', 'LEATHER', 'BROWN', 'MONOGRAM',
       '약간 거침', 0.9, NULL,
       'A', 300, 220, 1.3, '약간 거침', '보통', 1,
       NULL, NULL, 'DEPLETED',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-STATUS-DEPLETED');

-- ============================================================
-- D. Enum 커버리지 — MaterialType/Color/Pattern의 OTHER·MULTI 값
-- ============================================================

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-TYPE-OTHER', 'OTHER', 'MULTI', 'OTHER',
       '분류 어려움', 0.5, '기존 카테고리와 맞지 않아 OTHER로 태깅됨',
       'B', 300, 300, 0.9, '보통', '보통', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-TYPE-OTHER');

-- ============================================================
-- E. 필수 데이터 누락 케이스 (MaterialCandidateFilter.hasRequiredData)
--    셋 다 후보 추천에서 제외되어야 한다.
-- ============================================================

-- 등록은 됐지만 아직 AI 태깅 전(color/pattern이 비어있는 실제 상태)
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-UNTAGGED', 'LEATHER', NULL, NULL,
       NULL, NULL, NULL,
       'B', 300, 200, 1.0, '부드러움', '유연함', 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-UNTAGGED');

-- 수량 0 (재고 소진 오기입 등)
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-ZERO-QTY', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', 300, 200, 1.0, '부드러움', '유연함', 0,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-ZERO-QTY');

-- 치수 실측 전 (width/height/thickness 미입력)
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-NO-SIZE', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', NULL, NULL, NULL, NULL, NULL, 2,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-NO-SIZE');

-- ============================================================
-- F. 제작 가능성 계산 테스트 (MaterialCandidateFilter.hasEnoughArea /
--    canPlacePatternOnOneSheet, PatternPlacementCalculator, b12)
--    미니백 패턴 기준: 앞판 200x150 + 뒷판 200x150 + 옆판/바닥 400x60
--    (총 필요면적 144,000mm², 옆판/바닥 때문에 한 변이 400mm는 돼야 배치 가능)
--    러기지 태그 패턴 기준: 태그 몸체 100x60 (필요면적 6,000mm², 훨씬 작음)
-- ============================================================

-- 면적·배치 모두 여유 → 미니백 여러 개 생산 가능한 대표 케이스
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-PROD-AMPLE', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', 600, 500, 1.4, '부드러움', '유연함', 5,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-PROD-AMPLE');

-- 미니백 패턴 1세트가 한 장에 딱 들어가는 경계값 (여유 거의 없음)
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-PROD-TIGHT', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', 420, 360, 1.2, '부드러움', '유연함', 1,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-PROD-TIGHT');

-- 총 면적(가로x세로x수량=60,000mm²)이 미니백 필요면적(144,000mm²)보다 작아서
-- hasEnoughArea에서 바로 제외되는 케이스
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-PROD-AREA-SHORT', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', 300, 200, 1.0, '부드러움', '유연함', 1,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-PROD-AREA-SHORT');

-- 총 면적(180x140x50=1,260,000mm²)은 넉넉하지만, 한 장(180x140)이
-- 가장 큰 패턴 조각(200x150)보다 작아서 canPlacePatternOnOneSheet에서
-- 제외되는 케이스 — "총 면적만으로 판단하면 안 된다"를 보여주는 핵심 케이스
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-PROD-SHEET-TOO-SMALL', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', 180, 140, 1.0, '부드러움', '유연함', 50,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-PROD-SHEET-TOO-SMALL');

-- 러기지 태그(100x60)에는 넉넉하지만 미니백 패턴(최대 조각 200x150)엔
-- 못 들어가는 크기 — 미니백단독/러기지추가안 두 시나리오가 갈리는 케이스
INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-PROD-LUGGAGE-ONLY', 'LEATHER', 'BLACK', 'SOLID',
       '매끈함', 0.9, NULL,
       'A', 150, 100, 1.0, '부드러움', '유연함', 10,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-PROD-LUGGAGE-ONLY');

-- ============================================================
-- G. AI 신뢰도(confidence) 낮은 태깅 결과 표시 테스트
-- ============================================================

INSERT INTO material (
    id, material_code, material_type, color, pattern,
    texture, ai_confidence, surface_notes,
    grade, width_mm, height_mm, thickness_mm, hand_feel, flexibility, quantity,
    image_url_full, image_url_closeup, status,
    created_at, updated_at
)
SELECT gen_random_uuid(), 'QA-LOW-CONF', 'COATED_CANVAS', 'BEIGE', 'GEOMETRIC',
       '거친 편', 0.35, '얼룩으로 추정되는 부분 있음, 육안 재확인 필요',
       'C', 280, 200, 0.9, '약간 거침', '보통', 1,
       NULL, NULL, 'AVAILABLE',
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'QA-LOW-CONF');
