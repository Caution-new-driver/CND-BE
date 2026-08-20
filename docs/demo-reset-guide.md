# 데모 리셋 가이드 (담당자 아닌 팀원용)

시연 한 번 끝날 때마다(리허설 포함) 소재 상태가 소진되므로, **다음 시연 전에 아래 SQL을
한 번 돌려서 초기화**해야 합니다. 터미널/JDBC 없이 브라우저만으로 가능합니다.

## 준비물

- Neon 계정 로그인 정보 (공유 계정) — 데모 당일 전에 미리 로그인해보고 프로젝트가
  보이는지 확인해두세요. 안 보이면 김재현에게 초대 요청.
- `docs/reset-demo-state.sql` 파일 내용 (아래 그대로 복붙해도 됩니다)

## 실행 방법 (Neon 콘솔 SQL Editor, 설치 불필요)

1. https://console.neon.tech 접속 → 로그인
2. next:R.U.N 프로젝트 선택
3. 왼쪽 메뉴에서 **SQL Editor** 클릭
4. 아래 SQL을 전체 복사해서 편집창에 붙여넣기

```sql
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
```

5. **Run** 버튼 클릭
6. 에러 없이 끝나면 완료. (`ERROR` 같은 빨간 글씨 없으면 성공)
7. 확인하고 싶으면 프론트에서 소재 목록 화면을 새로고침해서 소재가 전부 다시
   "선택 가능" 상태로 보이는지, Drop 목록이 비어있는지 체크

## 주의사항

- 이 쿼리는 **소재 사진/치수/등급 같은 소재 정보 자체는 안 지웁니다.** 시연 중 만든
  Drop과 소재 선택 상태만 초기화합니다.
- 여러 번 실행해도 안전합니다 (이미 깨끗한 상태면 그냥 아무것도 안 지워짐).
- 실행 후 화면이 이상하면 김재현한테 바로 연락.

## (백업) 터미널이 편한 팀원이면

`psql` 설치돼있으면:
```bash
psql "<Neon 연결 문자열>" -f docs/reset-demo-state.sql
```
연결 문자열은 `src/main/resources/application-local.yaml`의 `datasource.url` 참고
(gitignored 파일이라 팀 채널로 공유받아야 함).
