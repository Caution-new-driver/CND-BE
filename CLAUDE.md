# next:R.U.N — CND-BE (백엔드)

next:R.U.N은 멋쟁이사자처럼 해커톤(SJF Track — Fashion & Luxury with AI, Challenge 01) 프로젝트. MCM의 잉여 소재(원단·가죽·부자재)를 AI가 분석해 디자이너의 설계 조건과 매칭시키고, 실제 남은 소재량만큼만 제작 가능한 수량을 계산해주는 **내부 상품기획 의사결정 도구**. (v5부터 고객 프리오더/판매 기능은 범위에서 완전히 빠짐 — 계산까지가 이 서비스의 끝)

전체 기획 원문: `../next_RUN_기능명세서_v5.md`(최신, 이 파일이 기준), `../next_RUN_erd_2.html`(ERD)을 참고. 이 파일은 그중 백엔드 작업에 필요한 핵심만 요약함.

## 팀 구성 & 역할 분담 (총 4인)

- **백엔드A 김재현 (이 저장소 담당 범위)**: Stage 0 공통기반(`b0~b3`), Stage 2 Drop 기획(`b7,b8,f2,f3`), Stage 5 확정(`b13,b14`)
- 백엔드B 이수현: Stage 1 소재등록·AI 태깅(`b4~b6`)
- 박서준: Stage 3 추천(`b9~b11`), Stage 4 제작가능성 계산(`b12`)
- 가연우 (FE 전체): `f1`, `f4~f7`

## 핵심 설계 원칙 (구현 시 반드시 지킬 것)

- **AI는 추천·설명·태깅만 담당하고, 숫자 계산(제작 가능 수량/활용률/러기지 태그 수량)은 항상 별도의 결정론적 알고리즘이 담당한다.** OpenAI 호출로 수량을 계산하지 않는다.
- 최종 결정은 항상 사람: 소재 최종 선택, 제작안 최종 선택, 소개문 최종 승인 — AI가 자동 확정하지 않는다. 적합 소재 0건이어도 AI가 자동으로 대체 소재를 고르지 않는다(재검색/종료만).
- 소재 **등급(grade)은 담당자 직접 입력** (AI 태깅 대상 아님 — AI는 색상·패턴·질감·신뢰도만 태깅). v3에서는 AI 제안이 있었으나 v4에서 제외됨.
- **부자재(지퍼·링) 수량 기반 병목 계산은 v4에서 제외** — 지퍼·링은 항상 충분하다고 가정하고, 소재(면적) 기준 계산만 수행.
- **2D 재단 좌표 시각화·제품 미리보기 v4에서 완전 제외**.
- **Drop 이름은 AI 자동 생성이 아니라 담당자가 직접 입력**한다 (문서 간 모순을 이 방향으로 확정함, 착각하기 쉬운 지점).
- 회원가입/로그인/역할별 권한관리 완전 제외.
- **v5부터 고객 프리오더/구매/공개(발행) 기능 자체가 없음.** Drop의 마지막 상태는 `CONFIRMED`(확정)까지이고, `PUBLISHED` 상태·"발행" 개념이 없음. 프리오더 신청 수량 정책(1건당 1개 등)은 더 이상 해당 없음.

## ERD 핵심 (10개 엔티티, 전체는 `../next_RUN_erd_2.html`)

재고: `MATERIAL`, `ACCESSORY`, `TEMPLATE`(미니백/러기지 태그 고정 패턴, 시드 데이터) →
기획: `DROP`, `DESIGN_REQUIREMENT` →
매칭: `MATERIAL_CANDIDATE`(AI 추천 후보, 탈락분도 보존) vs `DROP_MATERIAL_SELECTION`/`DROP_ACCESSORY_SELECTION`(확정본만) →
계산: `PRODUCTION_SCENARIO`(Drop당 2행: 미니백단독/러기지추가안, `is_selected`) → `PRODUCTION_SCENARIO_ITEM`(제품별 수량·넘버링)

반복되는 설계 패턴: "여러 개 중 하나를 고르는" 지점마다 후보 전체 보존 테이블과 확정본 테이블을 분리.

(v4까지 있던 `PREORDER` 테이블과 `DROP.preorder_start_date`/`preorder_end_date` 필드는 v5에서 삭제됨 — 고객 판매 기능 자체가 빠졌기 때문)

## 기술 스택

- 백엔드: **Spring Boot 4.1.0**, **Java 21 필수**(Jakarta EE 11 기반). 로컬 기본 `java`가 17일 수 있으니 프로젝트별 `JAVA_HOME` 21 고정 필요. Jackson 3.x 사용(`ObjectMapper` 빈만으로는 부족, `JsonMapper`/`XmlMapper` 빈 필요). Undertow 미지원(Tomcat 11/Jetty 12.1만).
- DB: PostgreSQL, Neon 호스팅 (배열/조건 검색 + `TEMPLATE.pattern_pieces` 같은 유연한 스키마 때문에 MySQL 대신 선택)
- 프론트: React + Vite + TypeScript (Next.js 아님), TanStack Query, Tailwind + shadcn/ui
- 이미지 저장: Cloudinary
- AI: OpenAI GPT-5.6 Luna(태깅) / Terra(추천이유, 소개문), Structured Outputs, API 키는 백엔드 환경변수로만 관리, 호출 결과는 DB 캐싱해 중복 호출 방지
- 배포: 백엔드 Railway(git push 기반, 실제 배포 완료 — `https://cnd-be-production.up.railway.app`), 프론트 Vercel
- 인증/접근제어: 완전 제외 (v5에서 관리자 게이트 검토 예정이나 보류 중)

## 일정 (7/31~8/18, 19일 스프린트)

Stage 0(~8/1: 셋업·DB·API명세) → Stage1·2 병렬(~8/4) → Stage3 합류(~8/6) → Stage4 계산(~8/8) → Stage5 확정(~8/11, **기능개발 종료**) → 통합/QA/데모(~8/18, 7일)

## 구현 현황 (v5 기준 23개 중)

- ✅ **완료·`dev` 병합·배포됨**: `b0,b1,b2`(초기세팅/DB스키마/외부클라이언트), `b7,b8`(Drop 생성·디자인조건저장 API), `f0,f2,f3`(FE)
- 🔲 **미착수**: `b4~b6,f1`(이수현), `b9~b12,f4,f5`(박서준/가연우), `b13,b14,f6,f7`(김재현·가연우 잔여분 — `b13`이 `b12` 결과에 의존해서 완전한 구현은 `b12` 끝나야 가능. 뼈대만 먼저 만드는 건 가능)
- `b3`(API 명세 합의)는 팀 회의로 못 박지 않고, `docs/api-draft.md` 초안 그대로 각자 개발 착수 → 진행하면서 수정하기로 **합의된 방침**. 열린 질문(`Material.status` 값 목록, 소재 후보 0건 응답 형식)은 각 담당자가 구현하면서 확정.
- **역할 분담 개수가 불균형함**: 김재현 10 / 가연우 5 / 박서준 4 / 이수현 3 (v5에서 이수현 담당이던 Stage 6 전체가 사라진 부작용). 팀이 인지한 상태로 일단 이대로 진행하기로 결정함 — 나중에 이수현 쪽 여유 생기면 `b3` 주도나 박서준 페어 제안 고려.

## 배포 정보

- 백엔드(Railway): `https://cnd-be-production.up.railway.app` — `dev` 브랜치 push 시 자동 재배포 (Custom Start Command 직접 지정돼있음, 아래 참고)
- 프론트(Vercel): `https://cnd-fe-git-dev-jae-hyun-kim.vercel.app` — Production Branch = `dev`
- Railway 환경변수: `SPRING_PROFILES_ACTIVE=prod`, `DATASOURCE_URL/USERNAME/PASSWORD`(Neon), `OPENAI_API_KEY`, `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET`, `CORS_ALLOWED_ORIGINS`(콤마 구분 목록, 현재 로컬+Vercel preview 주소 등록됨 — 나중에 Vercel 정식 도메인 나오면 추가 필요)
- 로컬 시크릿은 `src/main/resources/application-local.yaml`(gitignored) — 팀 전달 완료, 새 팀원은 기존 팀원에게 비공개 채널로 요청
- **미해결**: Vercel Deployment Protection이 켜져있어서 프로젝트에 초대 안 된 사람은 배포된 프론트에 접속 불가 — 팀원 전부 Vercel 프로젝트 멤버로 초대됐는지 확인 필요

## 알아두면 시간 아끼는 것들

- **Jackson 3.x**: `com.fasterxml.jackson.databind.ObjectMapper`가 아니라 `tools.jackson.databind.json.JsonMapper` 사용 (패키지 자체가 바뀜; annotation만 예외로 `com.fasterxml.jackson.annotation` 그대로 유지)
- **Railway 시작 커맨드**: 기본 자동감지 패턴(`*/build/libs/*jar`)이 하위 폴더 있는 멀티모듈 프로젝트를 가정해서 이 프로젝트(루트에 바로 jar 생성)엔 안 맞음. `build.gradle`에서 plain jar 비활성화(`tasks.named('jar') { enabled = false }`) + Railway Settings→Deploy에 Custom Start Command `sh -c "java -jar build/libs/*.jar"` 직접 지정해둔 상태.
- **Railway GitHub App**: 조직(`Caution-new-driver`) 레포에 앱 설치 자체가 안 돼있으면 "GitHub Repo not found"로 뜸 — GitHub 조직 설정 → Installed GitHub Apps에서 직접 설치해야 해결됨.
- **CORS**: 콤마로 여러 origin 등록할 때 공백 들어가면 매칭 실패함 — `CorsConfig.java`에서 이미 trim 처리해뒀지만, Railway Variables에 값 입력할 때도 공백/trailing slash 없이 정확히 입력할 것.

## 주의

- v4 문서의 ID(`b0~b18`, `f0~f10`)는 v3와 다르게 재채번됨 — 예전 자료(v3)의 ID와 혼용하지 말 것.
- **v5(현재 기준)에서 고객 프리오더 기능(구 Stage 6: `b16~b18`, `f9,f10`)과 Drop 발행 기능(구 `b15`, `f8`)이 통째로 제외됨** — 남은 건 `b0~b14`, `f0~f7` 총 23개, 전부 🔴필수. 예전 대화나 코드에 `PREORDER`, `publish`, `PUBLISHED` 같은 언급이 있으면 v4까지의 흔적이니 착오 없을 것.
