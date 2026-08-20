-- 데모데이용 리셋 스크립트.
-- 시연 한 번(Drop 생성 -> 소재후보 -> 소재/부자재 선택 -> 제작가능성 계산 -> 확정)을
-- 끝까지 돌리면 선택된 소재가 DEPLETED로 영구 소진되고, 이걸 되돌리는 API가 없다.
-- 그래서 다음 리허설/발표 전에 이 스크립트로 "시연 중 새로 생긴 데이터"만 전부 지우고,
-- material은 상태만 AVAILABLE로 되돌린다.
--
-- 지워지는 것: run_drop과 그 하위 전부(디자인조건/소재후보/소재·부자재 선택/제작 시나리오·결과)
-- 안 지워지는 것: material 본 데이터(사진 URL, AI 태깅값, 치수 등 — status만 리셋),
--                template(미니백/러기지 태그 고정 시드), accessory(지퍼/링 고정 시드)
--
-- 공유 Neon 개발 DB에 대한 삭제 작업이므로 실행 전 반드시 확인할 것.
-- 몇 번을 다시 돌려도 안전 (남아있는 Drop 데이터가 없으면 그냥 0행 삭제로 끝남).

BEGIN;

DELETE FROM production_material_result;
DELETE FROM production_scenario_item;
DELETE FROM production_scenario;
DELETE FROM drop_accessory_selection;
DELETE FROM drop_material_selection;
DELETE FROM material_candidate;
DELETE FROM design_requirement;
DELETE FROM run_drop;

UPDATE material SET status = 'AVAILABLE' WHERE status != 'AVAILABLE';

COMMIT;
