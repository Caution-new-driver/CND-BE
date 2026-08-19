# next:R.U.N. API 명세서

> **v4 → v5 변경**: 고객 프리오더 기능(Stage 6)을 완전히 제외하기로 결정하면서, Stage 6 섹션 전체와 Stage 5의 "발행" 관련 항목을 이 문서에서 제거했습니다.

공통 규칙: 전부 내부 운영용 API라 `/api/**`로 통일 (고객 공개 엔드포인트가 없어서 `/api/public/**` 구분 자체가 불필요해짐).

---

## Stage 0 — 공통

없음 (인프라 세팅만, API 없음)

---

## 인증 — 공유 비밀번호 로그인 (담당: 김재현) — ✅ 구현됨

MCM Run 담당 기획자 전용 도구라 회원가입/역할별 계정 대신, 팀이 공유하는 비밀번호
하나로만 접근을 제한한다. `POST /api/auth/login`을 제외한 모든 `/api/**` 요청은
`Authorization: Bearer <token>` 헤더가 없거나 유효하지 않으면 401을 반환한다.

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| - | POST | `/api/auth/login` | 공유 비밀번호 검증, 성공 시 세션 토큰 발급 | ✅ 구현됨 |

### `POST /api/auth/login`

요청 바디:

```json
{ "password": "..." }
```

응답 (200):

```json
{ "token": "1788008153.BpuZtHwGhtjaf7oI0OB4nfGwxNIHODkyziKfe1qK7P4" }
```

비밀번호가 틀리면 401 `{"message": "비밀번호가 올바르지 않습니다."}`.

토큰은 만료시각 1일 + HMAC 서명으로 구성되며 서버는 세션을 DB에 저장하지 않고
매 요청마다 서명만 재검증한다(별도 Session 테이블 없음, ERD 변경 없음). 프론트는
로그인 응답의 토큰을 저장해뒀다가 이후 모든 API 요청에 `Authorization: Bearer <token>`
헤더로 실어 보내야 한다. 쿠키를 쓰지 않은 이유: 프론트(Vercel)와 백엔드(Railway)가
서로 다른 도메인이라 쿠키가 third-party 쿠키로 취급돼 일부 브라우저(Safari 등)에서
차단될 수 있기 때문.

배포 시 필요한 환경변수: `ACCESS_PASSWORD`(로그인 비밀번호), `SESSION_SECRET`(토큰
서명용 랜덤 키, `openssl rand -hex 32`로 생성).

**변경 이력 (2026-08-20, 김재현)**: 토큰 만료 기간을 3일 → 1일로 단축(`AuthTokenService.TOKEN_VALID_DAYS`). 이 문서에 예전부터 "14일"로 적혀있던 건 실제 코드(3일)와 어긋난 오기였음 — 이번에 바로잡음.

---

## Stage 1 — 소재 등록 & AI 태깅 (담당: 이수현) — ✅ 구현됨

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b4 | POST | `/api/materials` | 소재 등록 (사진 + 메타데이터, `multipart/form-data`) | ✅ 구현됨 |
| b4 | GET | `/api/materials` | 소재 목록 조회 (`status`, `materialType` 필터, 둘 다 생략 가능) | ✅ 구현됨 |
| b4 | GET | `/api/materials/{id}` | 소재 단건 조회 (없으면 404) | ✅ 구현됨 |
| b4 | PATCH | `/api/materials/{id}` | 소재 부분 수정 (`multipart/form-data`, 보낸 필드만 반영) | ✅ 구현됨 |
| b4 | DELETE | `/api/materials/{id}` | 소재 삭제 (성공 시 204) | ✅ 구현됨 |
| b6 | POST | `/api/materials/{id}/ai-tag` | AI 이미지 태깅 (별도 호출, 등록 응답에는 포함 안 됨) | ✅ 구현됨 |

**변경 이력 (2026-08-06, 이수현)**: b6는 원래 제안대로 `POST /api/materials` 응답에 AI 태깅 결과를 함께 넣지 않고, `POST /api/materials/{id}/ai-tag`라는 별도 엔드포인트로 구현됨. 등록 직후 `color`/`pattern`/`texture`/`aiConfidence`/`surfaceNotes`는 전부 `null`이고, f1이 등록 완료 후 이 엔드포인트를 따로 호출해야 태깅 값이 채워짐. 이미 태깅된 소재(`color`가 있는 경우)를 다시 호출하면 OpenAI를 재호출하지 않고 기존 값을 그대로 반환한다(중복 호출 방지).

**해결됨 (2026-07-31, 김재현)**: `Material.status`는 `AVAILABLE`(등록됨) → `RESERVED`(b11에서 어떤 Drop의 주/포인트 소재로 선택됨) → `DEPLETED`(b13에서 그 Drop이 확정됨)의 3단계로 확정. `RESERVED` 상태에서 소재 선택이 재검색되거나 취소되면 `AVAILABLE`로 자동 복귀시켜야 함. `quantity`(재고 수량)는 지금은 분할 관리 없이 한 소재를 통째로 한 Drop에만 배정하는 것으로 단순화(부분 사용 지원은 나중에 스키마 변경 없이 추가 가능).

### `POST /api/materials`

`multipart/form-data`:

| 필드 | 타입 | 필수 | 비고 |
| --- | --- | --- | --- |
| materialCode | string | 아니오 |  |
| materialType | enum(string) | 아니오 | `LEATHER`, `COATED_CANVAS`, `FABRIC`, `SYNTHETIC`, `OTHER` |
| grade | enum(string) | 아니오 | `A`, `B`, `C` — 담당자 직접 입력 (AI 태깅 대상 아님) |
| widthMm / heightMm / thicknessMm | number | 아니오 |  |
| handFeel / flexibility | string | 아니오 |  |
| quantity | integer | 아니오 |  |
| imageFull | file | 아니오 | 전체 사진 |
| imageCloseup | file | 아니오 | 클로즈업 사진 |

응답 (201): `MaterialResponse` — 등록 직후 `status`는 `AVAILABLE`로 시작하며 AI 태깅 필드(`color`/`pattern`/`texture`/`aiConfidence`/`surfaceNotes`)는 전부 `null`이다.

### `POST /api/materials/{id}/ai-tag`

요청 바디 없음. 저장된 사진 URL로 OpenAI를 호출해 `color`/`pattern`/`texture`/`aiConfidence`/`surfaceNotes`를 채운 `MaterialResponse`를 반환한다.

### `PATCH /api/materials/{id}`

`POST`와 동일한 필드에 더해 AI 태깅 필드(`color`/`pattern`/`texture`/`aiConfidence`/`surfaceNotes`)도 직접 덮어쓸 수 있다(담당자가 AI 결과를 확인 후 수정하는 용도). `multipart/form-data`이며 값을 보낸 필드만 부분 수정되고, 새 사진을 보내면 기존 사진 URL을 교체한다.

### `DELETE /api/materials/{id}`

`status`가 `AVAILABLE`이 아니면(`RESERVED`=Drop에 예약 중, `DEPLETED`=Drop 확정으로 소진됨) 409를 반환하고 삭제하지 않는다. `AVAILABLE`이면 그 소재를 참조하던 탈락 후보 이력(`material_candidate`)까지 함께 정리하고 삭제한다(`drop_material_selection`/`production_material_result`는 `AVAILABLE` 상태에서 이미 참조가 0건임이 보장되므로 별도 처리 불필요).

**변경 이력 (2026-08-19, 김재현)**: 위 상태 제한 추가. 기존에는 상태와 무관하게 무조건 삭제를 시도해서, 참조가 남아있는 소재를 지우려 하면 FK 위반으로 예외 처리 안 된 500이 나던 문제를 고침.

---

## Stage 2 — Drop 기획 시작 (담당: 김재현) — ✅ 구현됨

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b7 | POST | `/api/drops` | Drop 생성 (draft 상태), 고정 미니백 템플릿 정보 함께 반환 | ✅ 구현됨 |
| b8 | POST | `/api/drops/{dropId}/design-requirement` | 디자인 조건 저장 | ✅ 구현됨 |
| f2 | GET | `/api/templates/{name}` | Drop 생성 없이 고정 템플릿 정보만 조회 (없으면 404) | ✅ 구현됨 |

### `POST /api/drops`

요청 바디 없음.

응답 (201):

```json
{
  "id": "uuid",
  "status": "DRAFT",
  "templateId": "uuid",
  "templateName": "미니백",
  "patternPieces": [{ "pieceName": "앞판", "widthMm": 200, "heightMm": 150, "quantity": 1, "role": "MAIN" }],
  "requiredAccessories": [{ "accessoryType": "지퍼", "quantity": 1 }]
}
```

`role`(`MAIN`/`POINT`)은 b12 제작가능성 계산에서 이 조각을 주 소재와 포인트 소재 중 어디에 배치할지를 나타낸다(포인트 소재가 없으면 전부 주 소재에 배치). 미니백 템플릿은 앞판·뒷판이 `MAIN`, 옆판/바닥이 `POINT`다.

### `POST /api/drops/{dropId}/design-requirement`

`multipart/form-data`:

| 필드 | 타입 | 필수 | 허용 값 |
| --- | --- | --- | --- |
| materialType | enum(string) | 아니오 | `LEATHER`, `COATED_CANVAS`, `FABRIC`, `SYNTHETIC`, `OTHER` |
| color | enum(string) | 아니오 | `BLACK`, `BROWN`, `BEIGE`, `WHITE`, `RED`, `BLUE`, `MULTI`, `OTHER` |
| pattern | enum(string) | 아니오 | `MONOGRAM`, `SOLID`, `GEOMETRIC`, `STRIPE`, `OTHER` |
| minGrade | enum(string) | 아니오 | `A`, `B`, `C` |

`materialType`/`color`/`pattern`/`minGrade`는 `Material` 엔티티의 enum(`MaterialType`/`MaterialColor`/`MaterialPattern`/`MaterialGrade`)과 동일한 값을 그대로 씀 — b9에서 문자열 비교 없이 바로 매칭하기 위함. FE(f3)는 자유 입력 대신 Select로 받아 한글 라벨(예: "가죽")을 이 영문 enum 값(`LEATHER`)으로 변환해 전송해야 함. 잘못된 값이 오면 400으로 거부됨.

응답 (200):

```json
{
  "id": "uuid",
  "dropId": "uuid",
  "materialType": "LEATHER",
  "color": "BLACK",
  "pattern": "SOLID",
  "minGrade": "A"
}
```

같은 `dropId`로 재호출하면 새로 안 생기고 기존 것을 덮어씀(upsert).

**열린 질문**: 필수 필드가 실제로 뭔지 (지금은 전부 선택). f4 분기 B "조건 수정해 다시 검색" 시 전체 재입력인지 특정 필드만 수정인지도 기획서 자체에 미결정으로 남아있음 — 이 upsert 방식이면 어느 쪽이든 대응은 됨.

**변경 이력 (2026-08-07, 김재현)**: `materialType`/`color`/`pattern`/`minGrade`/`accessoryColor`를 자유 입력 문자열에서 고정 enum으로 변경. b9가 `Material` enum과 직접 비교해야 해서 오타·표기 흔들림을 막기 위함. `accessoryColor`와 `Accessory.color`는 모두 `AccessoryColor`(`GOLD`/`SILVER`/`BLACK`)를 사용한다.

**변경 이력 (2026-08-18, 김재현)**: `usePointMaterial` 필드 제거. 저장·응답만 될 뿐 b9~b12 어느 로직에서도 참조되지 않는 죽은 입력값이었음 — 실제 포인트 소재 사용 여부는 b11 소재 확정 단계에서 `pointCandidateId` 제출 여부로 결정됨.

**변경 이력 (2026-08-18, 김재현)**: `accessoryColor` 필드 제거 및 부자재 색상 검증 완화. 기존에는 이 필드로 선호 색상을 미리 선언하고 `/accessory-selections` 호출 시 그 값과 일치하는지, 지퍼·링이 서로 같은 색상인지 교차검증했으나 두 검증 모두 제거함. 이제 부자재 색상은 사전 선언 없이 `/accessory-selections` 호출 시점에 바로 정해지고, 지퍼와 링을 서로 다른 색상으로 선택해도 된다.

**변경 이력 (2026-08-01, 김재현)**: 스케치 이미지 첨부 기능(`sketchImage`/`sketchImageUrl`) 제거. 저장은 됐지만 이후 어떤 화면(f4~f7)에서도 다시 노출하는 계획이 없어 "업로드만 되고 아무도 다시 안 보는" 죽은 기능이었음. v4 문서에 있었던 고객용 Drop 상세 페이지(`f9`, v5에서 삭제)에서 노출하려던 용도였을 것으로 추정 — 고객 접점 자체가 사라지며 목적을 잃은 것으로 판단해 정리함.

### `GET /api/templates/{name}`

Drop을 생성하지 않고도 고정 템플릿(패턴 조각/부자재) 정보만 미리 보여주기 위한 조회 전용 API. f2 화면 진입 시점마다 `POST /api/drops`를 호출하면 쓰지 않는 `DRAFT` Drop이 계속 쌓이는 문제를 막기 위해 추가됨. `name`에는 `미니백` 또는 `러기지 태그`를 사용하며, 없으면 404를 반환한다.

응답 (200):

```json
{
  "templateId": "uuid",
  "templateName": "미니백",
  "patternPieces": [{ "pieceName": "앞판", "widthMm": 200, "heightMm": 150, "quantity": 1, "role": "MAIN" }],
  "requiredAccessories": [{ "accessoryType": "지퍼", "quantity": 1 }]
}
```

`POST /api/drops` 응답과 같은 템플릿 정보이지만 `id`(Drop ID)/`status`가 없고 필드명이 `templateId`/`templateName`이다.

---

## Stage 3 — 소재 후보 추천 & 조합 선택 (담당: 박서준)

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b9+b10 | POST | `/api/drops/{dropId}/material-candidates` | 필수조건 필터링 + AI 추천(최대 3개, 추천이유·주의사항 포함) 계산·저장 | ✅ 구현됨 |
| b9+b10 | GET | `/api/drops/{dropId}/material-candidates` | 계산된 후보 조회 | ✅ 구현됨 |
| b11 | GET | `/api/accessories` | 선택 가능한 부자재 목록 조회 | ✅ 구현됨 |
| b11 | POST | `/api/drops/{dropId}/material-selection` | 주 소재·포인트 소재 확정 저장 | ✅ 구현됨 |
| b11 | POST | `/api/drops/{dropId}/accessory-selections` | 부자재 세트 확정 저장 | ✅ 구현됨 |

후보가 0건이면 정상 응답(200)의 `candidates`를 빈 배열로 반환한다. 프론트는 이를
"조건을 수정해 다시 검색" 또는 "기획 종료" 분기로 처리한다.

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
부자재 ID를 두 번 보내지 않는다. 지퍼와 링은 서로 다른 색상으로 선택해도 되며,
디자인 조건 단계에서 미리 지정해야 하는 색상 제약은 없다 — 이 API를 호출하는 시점에
바로 색상까지 확정된다.

응답 (200): Drop ID와 저장된 부자재 선택 ID·부자재 ID·종류·색상을 반환한다.

소재 후보를 다시 계산하는 `POST /material-candidates`가 성공하면 기존 소재 선택은
삭제되고 해당 Drop이 예약했던 소재는 `AVAILABLE`로 자동 복구된다. 필터링이나
OpenAI 추천이 실패하면 기존 선택·후보·제작 시나리오는 그대로 보존된다.

---

## Stage 4 — 제작 가능성 계산 (담당: 박서준)

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b12 | POST | `/api/drops/{dropId}/production-scenarios` | 소재 기준 수량·활용률·러기지 태그 수량 계산 → 시나리오 2건(단독/추가) 생성 | ✅ 구현됨 |
| b12 | GET | `/api/drops/{dropId}/production-scenarios` | 계산된 시나리오 조회 | ✅ 구현됨 |
| b12 | POST | `/api/drops/{dropId}/production-scenarios/{scenarioId}/select` | 최종 제작안 선택 (`Drop.selectedScenarioId` 갱신) | ✅ 구현됨 |

### b12 계산 규칙

- 템플릿과 소재 치수는 모두 `mm` 단위로 계산한다.
- 직사각형 패턴을 큰 조각부터 2차원으로 배치하며 90도 회전을 허용한다.
- MVP에서는 모든 배치 조합을 완전탐색하지 않고, 실제 배치 성공이 확인된 보수적 수량을 반환한다.
따라서 결과는 안전하게 제작 가능한 수량이지만 수학적 최댓값을 항상 보장하지는 않는다.
- 소재 여러 장은 서로 붙이지 않고 한 장씩 계산한 뒤 수량을 합산한다.
- 포인트 소재가 없으면 모든 미니백 패턴을 주 소재에 배치한다.
- 포인트 소재가 있으면 앞판·뒷판은 주 소재, 옆판/바닥은 포인트 소재에 배치한다.
- 미니백 최종 수량은 주 소재와 포인트 소재가 각각 지원하는 수량 중 작은 값이다.
- 러기지 태그 추가안은 미니백 배치 후 주 소재와 포인트 소재 양쪽의 남은 영역을 사용한다.
- 같은 Drop으로 POST를 재호출하면 기존 결과와 선택 상태를 초기화하고 새 결과로 교체한다.
- 계산 결과와 소재별 남은 사각형 영역은 DB에 저장하며 GET에서는 재계산하지 않는다.

응답에는 `selectedScenarioId`와 두 개의 `scenarios`가 포함된다. 각 시나리오는
제품별 수량·넘버링, 전체 활용률·사용/잔여 면적, 소재별 지원 수량과 잔여 사각형 목록을 반환한다.

---

## Stage 5 — Drop 확정 (담당: 김재현) — ✅ 구현됨

> v5부터 "발행/공개" 개념 없음 — 확정(CONFIRMED)까지가 이 서비스의 마지막 단계.
> 

| ID | Method | Path | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| b13 | PATCH | `/api/drops/{dropId}/confirm` | 상태 전환(CONFIRMED) + 부가정보 저장(이름 직접입력, 예상 제작기간, 넘버링은 Stage4 값 기반 확정) + AI 소개문 초안(b14) 생성까지 한 번에 처리 | ✅ 구현됨 |
| b14 | POST | `/api/drops/{dropId}/intro-text` | 담당자가 AI 소개문을 다시 생성 요청 (재생성) | ✅ 구현됨 |
| b14 | PATCH | `/api/drops/{dropId}/intro-text` | 담당자가 초안을 직접 고친 최종본 저장 | ✅ 구현됨 |

**변경 이력 (2026-08-14, 김재현)**: 초안 설계엔 `POST /api/drops/{dropId}/intro-text`(AI 초안 생성)가 `PATCH /confirm`과 별도 API로 있었으나, 와이어프레임(f6·f7)을 다시 보니 두 화면이 "Drop 확정하기" 버튼 하나로 묶인 한 화면이라 별도 생성 API를 없애고 `PATCH /confirm` 안에서 AI 소개문까지 함께 생성하도록 통합했다. 그래서 `PATCH /confirm` 하나가 상태 전환·부가정보 저장·소재 DEPLETED 전환·AI 소개문 최초 생성을 다 처리한다.

**변경 이력 (2026-08-15, 김재현)**: AI 초안이 마음에 안 들 때 다시 시도할 방법이 없다는 문제로 `POST /api/drops/{dropId}/intro-text`(재생성)를 되살렸다. 단, 인증이 없는 내부 도구라 제한 없이 반복 호출하면 OpenAI 크레딧이 과도하게 소모될 수 있어 **Drop당 총 6회**(b13 최초 생성 1회 + b14 재생성 최대 5회)로 상한을 뒀다. 실패한 시도도 이미 API 호출이 나간 뒤라 횟수에 포함된다. 6회를 모두 쓰면 재생성 API는 `409`를 반환하고, 이후엔 `PATCH /intro-text`(수동 입력)만 가능하다.

**병합 완료 (2026-08-15, PR #11)**: `dev`에 머지됨.

### `PATCH /api/drops/{dropId}/confirm`

요청 바디:

```json
{
  "name": "2026 가을 미니백 캡슐",
  "expectedProductionDays": 14
}
```

`name`은 필수(담당자 직접 입력 — AI 자동 생성 아님), `expectedProductionDays`는 선택이며 보내면 1 이상이어야 한다.

아래 조건을 모두 만족해야 성공한다 (하나라도 없으면 `409 Conflict`):

- 제작안(b12)이 선택돼 있어야 함 (`Drop.selectedScenarioId`)
- 주 소재 확정(b11 `/material-selection`)이 저장돼 있어야 함
- 부자재 확정(b11 `/accessory-selections`)이 저장돼 있어야 함

응답 (200):

```json
{
  "id": "uuid",
  "status": "CONFIRMED",
  "name": "2026 가을 미니백 캡슐",
  "expectedProductionDays": 14,
  "selectedScenarioId": "uuid",
  "items": [{ "productType": "MINI_BAG", "quantity": 12, "numberingStart": 1, "numberingEnd": 12 }],
  "introText": "...",
  "regenerationsRemaining": 5
}
```

넘버링은 새로 계산하지 않고 b12에서 선택해둔 시나리오 값을 그대로 확정해 보여준다. 확정된 주/포인트 소재는 `DEPLETED`로 전환된다. `introText`는 AI 생성이 성공하면 채워지고, OpenAI 호출이 실패해도 확정 자체는 그대로 성공하며 이때는 `null`로 내려온다(트랜잭션 밖에서 호출되므로 AI 실패가 DB 확정을 막지 않음).

### `POST /api/drops/{dropId}/intro-text` (AI 재생성)

요청 바디 없음. `PATCH /confirm`에서 받은 AI 초안이 마음에 안 들 때 다시 생성 요청한다.

응답 (200):

```json
{ "dropId": "uuid", "introText": "...", "regenerationsRemaining": 4 }
```

### `PATCH /api/drops/{dropId}/intro-text` (수동 저장)

요청 바디:

```json
{ "introText": "담당자가 직접 고친 최종 소개문" }
```

응답 (200): `POST` 재생성과 동일한 형태.

`POST /intro-text`(재생성)와 `PATCH /intro-text`(수동 저장) 둘 다 Drop이 `CONFIRMED` 상태가 아니면 거부된다(`409`). 응답엔 공통으로 `regenerationsRemaining`(남은 재생성 가능 횟수, 0~5)이 포함되며, `PATCH`(수동 저장)는 이 횟수를 소모하지 않는다.