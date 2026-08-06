# CND-BE 🚗
2종보통멋쟁이 BE Repository

## 팀 규칙

### 브랜치 전략
- `main`: 건드리지 않음 (현재 미사용, 배포는 `dev` 기준) -> 최종 제출 전에 main에 한 번 병합할 예정.
- `dev`: 실제 작업 기준 브랜치. 배포(Railway/Vercel)도 여기서 자동 트리거됨
- 기능 작업은 `dev`에서 브랜치 따서 진행 후 `dev`로 병합
  - 새 기능: `feat/기능-이름` (예: `feat/b9-material-matching`)
  - 설정/잡일: `chore/작업-이름`
  - 버그 수정: `fix/버그-이름`

### 커밋 컨벤션
`타입: 설명 (관련 ID)` 형식, 타입은 `feat`/`fix`/`chore`/`refactor` 사용.


### PR 규칙
- `dev`로 병합하기 전, 최소 자기 브랜치에서 로컬 빌드·기동 확인 후 병합
- 다른 사람 파트와 겹치는 변경(공용 파일, 엔티티 등)은 병합 전 팀 채널에 한 줄 공유
- 급한 배포 관련 수정이 아니면 가능하면 PR로 올리고 한 명 이상 확인 후 병합 추천

### 코드 컨벤션
- 패키지는 도메인 단위로 분리: `domain/<도메인명>/`(엔티티, Repository, Service, Controller), `domain/<도메인명>/dto/`(요청·응답 DTO)
- 공통 로직은 `common/` (BaseEntity, GlobalExceptionHandler, 외부 클라이언트 설정 등)
- 엔티티는 `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` + `BaseEntity` 상속(생성/수정 시각 자동 포함)
- ID는 `@Id @GeneratedValue @UuidGenerator`로 UUID 자동 생성
- 잘못된 요청은 `IllegalArgumentException`(404), 서버/데이터 문제는 `IllegalStateException`(409)로 던지면 `GlobalExceptionHandler`가 알아서 처리함

### Ground Rule
- **절대 `application-local.yaml`을 커밋하지 말 것** (`.gitignore` 등록돼 있지만 재확인 습관화)
- 로컬 비밀값이 필요하면 담당자(김재현)에게 요청 — Slack DM 등 비공개 채널로만 전달
- 확정 안 된 API 스펙(`docs/api-draft.md`의 "제안" 상태)에 의존하는 코드를 짤 땐, 관련 담당자와 먼저 필드명 맞추고 시작

## 개발 환경
- **Java 21 필수** (Spring Boot 4.1.0). 로컬 기본 `java`가 17일 수 있으니 실행 시 `JAVA_HOME=$(/usr/libexec/java_home -v 21)` 지정
- Gradle (wrapper 포함, 별도 설치 불필요)
- DB: PostgreSQL(Neon), 로컬에서도 같은 Neon 인스턴스에 연결
- API 문서: 로컬 실행 후 `http://localhost:8080/swagger-ui/index.html`

## 작업 방법
1. 저장소 clone, `dev` 브랜치로 체크아웃
2. 담당자에게 받은 `application-local.yaml`을 `src/main/resources/`에 그대로 저장
3. 로컬 실행: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew bootRun`
4. `http://localhost:8080/actuator/health`가 `{"status":"UP"}` 뜨면 정상
5. `dev`에서 `feat/...` 브랜치 따서 작업 → 로컬 확인 → 커밋 → push → pr 올리기 -> `dev` 병합
