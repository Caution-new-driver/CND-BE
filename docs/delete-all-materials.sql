-- 소재 전체 초기화용 삭제 쿼리.
-- material은 아래 3개 테이블에서 FK로 참조되고 있어서 단순히
-- DELETE FROM material만 실행하면 FK 위반으로 실패한다.
-- 그래서 material을 참조하는 자식 행부터 먼저 지우고 마지막에 material을 지운다.
--
--   production_material_result.material_id -> material.id
--   drop_material_selection.main_material_id / point_material_id -> material.id
--   material_candidate.material_id -> material.id
--
-- 주의: DROP, PRODUCTION_SCENARIO 같은 상위 엔티티 자체는 지우지 않는다.
-- 다만 그 Drop이 이미 확정한 소재 선택(drop_material_selection)과
-- 시나리오별 소재 계산 결과(production_material_result)는 이 쿼리로 함께 사라지므로,
-- 실제 진행 중인 Drop 데이터가 있는 DB에서 실행하면 그 결과가 같이 지워진다.
-- 공유 Neon 개발 DB에서 실행하기 전에 정말 전체 초기화가 맞는지 한 번 더 확인할 것.

BEGIN;

DELETE FROM production_material_result;
DELETE FROM drop_material_selection;
DELETE FROM material_candidate;
DELETE FROM material;

COMMIT;
