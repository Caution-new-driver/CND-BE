-- DB에 쌓인 테스트 데이터 전체 초기화용 삭제 쿼리.
-- template(디자인 템플릿: 미니백/러기지 태그 고정 패턴)만 남기고 나머지 전부 지운다.
--
-- FK 참조 관계상 자식 테이블부터 지워야 하므로 아래 순서를 지킬 것:
--
--   production_material_result.scenario_id -> production_scenario.id
--   production_material_result.material_id -> material.id
--   production_scenario_item.scenario_id   -> production_scenario.id
--   drop_material_selection.drop_id        -> run_drop.id
--   drop_material_selection.main_material_id / point_material_id -> material.id
--   drop_accessory_selection.drop_id       -> run_drop.id
--   drop_accessory_selection.accessory_id  -> accessory.id
--   material_candidate.drop_id             -> run_drop.id
--   material_candidate.material_id         -> material.id
--   design_requirement.drop_id             -> run_drop.id
--   production_scenario.drop_id            -> run_drop.id
--   run_drop.template_id                   -> template.id   (template은 삭제 대상 아님)
--
-- accessory(지퍼/링 색상 메뉴판)도 template과 마찬가지로 AccessorySeeder가
-- 앱 기동 시 idempotent하게 재생성하는 고정 시드라서 지워도 다음 기동 때 자동 복구된다.
-- 그래도 재기동 없이 바로 남겨두고 싶다면 맨 아래 DELETE FROM accessory 줄만 지우면 됨.
--
-- 주의: 공유 Neon 개발 DB에서 실행하면 진행 중이던 Drop/소재 선택/시나리오 계산 결과가
-- 전부 사라진다. 정말 전체 초기화가 맞는지 한 번 더 확인할 것.

BEGIN;

DELETE FROM production_material_result;
DELETE FROM production_scenario_item;
DELETE FROM drop_material_selection;
DELETE FROM drop_accessory_selection;
DELETE FROM material_candidate;
DELETE FROM design_requirement;
DELETE FROM production_scenario;
DELETE FROM run_drop;
DELETE FROM material;
DELETE FROM accessory;

COMMIT;
