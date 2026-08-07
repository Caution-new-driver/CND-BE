# next:R.U.N API 명세 초안 (b3 팀 논의용)

> **이 문서의 상태: 초안입니다.** 팀 미팅에서 다 같이 맞춰본 뒤 확정하세요.
> "✅ 구현됨"만 실제로 만들어서 테스트까지 끝낸 것이고, 나머지는 ERD·기능명세서 v5(`next_RUN_기능명세서_v5.md`, 고객 프리오더 기능 제외 반영) 기준으로 제가 제안한 형태입니다 — 실제 담당자가 다르게 만들고 싶으면 얼마든지 바뀔 수 있습니다.
>
> **v4 → v5 변경**: 고객 프리오더 기능(Stage 6)을 완전히 제외하기로 결정하면서, Stage 6 섹션 전체와 Stage 5의 "발행" 관련 항목을 이 문서에서 제거했습니다.

공통 규칙: 전부 내부 운영용 API라 `/api/**`로 통일 (고객 공개 엔드포인트가 없어서 `/api/public/**` 구분 자체가 불필요해짐).

---

## Stage 0 — 공통

없음 (인프라 세팅만, API 없음)

---

## Stage 1 — 소재 등록 & AI 태깅 (담당: 이수현)

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b4 | POST | `/api/materials` | 소재 등록 (사진 + 메타데이터) | 제안 |
| b4 | GET | `/api/materials` | 소재 목록 조회 (필터 포함) | 제안 |
| b4 | GET | `/api/materials/{id}` | 소재 단건 조회 | 제안 |
| b4 | PATCH | `/api/materials/{id}` | 소재 수정 (등급·태그 수정 포함) | 제안 |
| b6 | - | - | AI 태깅은 `POST /api/materials` 응답에 함께 포함하는 걸 제안 (등록과 동시에 AI 분석 결과까지 반환 → f1 화면이 한 화면에서 등록+태깅확인 하므로) | 제안 |

**해결됨 (2026-07-31, 김재현)**: `Material.status`는 `AVAILABLE`(등록됨) → `RESERVED`(b11에서 어떤 Drop의 주/포인트 소재로 선택됨) → `DEPLETED`(b13에서 그 Drop이 확정됨)의 3단계로 확정. `RESERVED` 상태에서 소재 선택이 재검색되거나 취소되면 `AVAILABLE`로 자동 복귀시켜야 함. `quantity`(재고 수량)는 지금은 분할 관리 없이 한 소재를 통째로 한 Drop에만 배정하는 것으로 단순화(부분 사용 지원은 나중에 스키마 변경 없이 추가 가능) — 이수현님은 이 흐름대로 b4 구현하시면 됩니다.

---

## Stage 2 — Drop 기획 시작 (담당: 김재현) — ✅ 구현됨

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b7 | POST | `/api/drops` | Drop 생성 (draft 상태), 고정 미니백 템플릿 정보 함께 반환 | ✅ 구현됨 |
| b8 | POST | `/api/drops/{dropId}/design-requirement` | 디자인 조건 저장 | ✅ 구현됨 |

### `POST /api/drops`
요청 바디 없음.

응답 (201):
```json
{
  "id": "uuid",
  "status": "DRAFT",
  "templateId": "uuid",
  "templateName": "미니백",
  "patternPieces": [{ "pieceName": "앞판", "widthCm": 20, "heightCm": 15, "quantity": 1 }],
  "requiredAccessories": [{ "accessoryType": "지퍼", "quantity": 1 }]
}
```

### `POST /api/drops/{dropId}/design-requirement`
`multipart/form-data`:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| materialType | string | 아니오 |
| color | string | 아니오 |
| pattern | string | 아니오 |
| minGrade | string | 아니오 |
| accessoryColor | string | 아니오 |
| usePointMaterial | boolean | 아니오 |

응답 (200):
```json
{
  "id": "uuid",
  "dropId": "uuid",
  "materialType": "가죽",
  "color": "블랙",
  "pattern": "무지",
  "minGrade": "A",
  "accessoryColor": "골드",
  "usePointMaterial": true
}
```
같은 `dropId`로 재호출하면 새로 안 생기고 기존 것을 덮어씀(upsert).

**열린 질문**: 필수 필드가 실제로 뭔지 (지금은 전부 선택). f4 분기 B "조건 수정해 다시 검색" 시 전체 재입력인지 특정 필드만 수정인지도 기획서 자체에 미결정으로 남아있음 — 이 upsert 방식이면 어느 쪽이든 대응은 됨.

**변경 이력 (2026-08-01, 김재현)**: 스케치 이미지 첨부 기능(`sketchImage`/`sketchImageUrl`) 제거. 저장은 됐지만 이후 어떤 화면(f4~f7)에서도 다시 노출하는 계획이 없어 "업로드만 되고 아무도 다시 안 보는" 죽은 기능이었음. v4 문서에 있었던 고객용 Drop 상세 페이지(`f9`, v5에서 삭제)에서 노출하려던 용도였을 것으로 추정 — 고객 접점 자체가 사라지며 목적을 잃은 것으로 판단해 정리함.

---

## Stage 3 — 소재 후보 추천 & 조합 선택 (담당: 박서준)

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b9+b10 | POST | `/api/drops/{dropId}/material-candidates` | 필수조건 필터링 + AI 추천(최대 3개, 추천이유·주의사항 포함) 계산·저장 | 제안 |
| b9+b10 | GET | `/api/drops/{dropId}/material-candidates` | 계산된 후보 조회 | 제안 |
| b11 | GET | `/api/accessories` | 선택 가능한 부자재 목록 조회 | ✅ 구현됨 |
| b11 | POST | `/api/drops/{dropId}/material-selection` | 주 소재·포인트 소재 확정 저장 | ✅ 구현됨 |
| b11 | POST | `/api/drops/{dropId}/accessory-selections` | 부자재 세트 확정 저장 | ✅ 구현됨 |

**열린 질문**: 후보 0건일 때(b19에 해당하던 케이스, v4에서도 로직상 필요) 응답을 어떻게 표현할지 — 빈 배열 + 상태 코드로 구분할지, 별도 필드로 표시할지.

### `GET /api/accessories`

응답 (200):
```json
[
  {"id": "uuid", "accessoryType": "지퍼", "color": "GOLD"},
  {"id": "uuid", "accessoryType": "링", "color": "GOLD"}
]
```

서버 시작 시 디자이너가 선택할 수 있는 `지퍼`/`링` × `GOLD`/`SILVER`/`BLACK`
조합 중 DB에 없는 것만 시드 데이터로 등록한다. AI가 선택하거나 확정하지 않는다.

### `POST /api/drops/{dropId}/material-selection`

요청 바디:
```json
{
  "mainCandidateId": "uuid",
  "pointCandidateId": "uuid 또는 null"
}
```

`mainCandidateId`는 필수이고 `pointCandidateId`는 선택사항이다. 둘 다 b9 응답의
`candidateId`를 사용한다. 선택한 소재는 `RESERVED`로 변경되며 같은 Drop에서
재선택하면 더 이상 사용하지 않는 기존 소재는 `AVAILABLE`로 복구된다.

응답 (200): 선택 ID, Drop ID, 주 소재 상세 정보와 선택적 포인트 소재 상세 정보를 반환한다.

### `POST /api/drops/{dropId}/accessory-selections`

요청 바디:
```json
{
  "accessoryIds": ["지퍼 uuid", "링 uuid"]
}
```

`GET /api/accessories`에서 받은 ID를 사용한다. 미니백 템플릿의 필수 종류인
지퍼와 링이 각각 하나씩 포함돼야 하며, 같은 Drop에서 재호출하면 기존 부자재
선택을 새 세트로 교체한다. 링의 필요 수량 2개는 템플릿 정보로 관리하므로 같은
부자재 ID를 두 번 보내지 않는다.

응답 (200): Drop ID와 저장된 부자재 선택 ID·부자재 ID·종류·색상을 반환한다.

소재 후보를 다시 계산하는 `POST /material-candidates`를 호출하면 기존 소재 선택은
삭제되고 해당 Drop이 예약했던 소재는 `AVAILABLE`로 자동 복구된다.

---

## Stage 4 — 제작 가능성 계산 (담당: 박서준)

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b12 | POST | `/api/drops/{dropId}/production-scenarios` | 소재 기준 수량·활용률·러기지 태그 수량 계산 → 시나리오 2건(단독/추가) 생성 | 제안 |
| b12 | GET | `/api/drops/{dropId}/production-scenarios` | 계산된 시나리오 조회 | 제안 |
| b12 | POST | `/api/drops/{dropId}/production-scenarios/{scenarioId}/select` | 최종 제작안 선택 (`Drop.selectedScenarioId` 갱신) | 제안 |

---

## Stage 5 — Drop 확정 (담당: 김재현)

> v5부터 "발행/공개" 개념 없음 — 확정(CONFIRMED)까지가 이 서비스의 마지막 단계.

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b13 | PATCH | `/api/drops/{dropId}/confirm` | 상태 전환(CONFIRMED) + 부가정보 저장(이름 직접입력, 예상 제작기간, 넘버링은 Stage4 값 기반 확정) | 제안 |
| b14 | POST | `/api/drops/{dropId}/intro-text` | AI 소개문 초안 생성 | 제안 |
| b14 | PATCH | `/api/drops/{dropId}/intro-text` | 담당자 수정본 저장 | 제안 |

---

## 팀 미팅에서 확정하면 좋을 것

1. 위 "열린 질문" 2가지 (Stage 1, Stage 3)
2. 에러 응답 공통 포맷 (지금은 `{"message": "..."}` 하나만 씀 — 필드 에러 등 세분화 필요할지)
3. 인증 없음이 확정인데, 이제 전부 내부용 API라 v4 때보다 리스크는 낮아짐 — 그래도 한 번 팀 확인 필요
