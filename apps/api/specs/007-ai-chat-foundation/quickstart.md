# Phase 07 Quickstart / Verification Guide

**Status**: 구현 및 로컬 검증 완료 — 최종 전체 277개 통과, 실패/스킵 0개 (2026-11-14)

## Prerequisites

- 작업 기준: `/private/tmp/douid-phase07/apps/api`
- Java 25, 기존 Gradle wrapper
- 기존 전체 회귀 및 실제 app HTTP 통합 테스트: Docker/PostgreSQL Testcontainers. 기존 PostgresIntegrationTest/Flyway 및 ddl-auto=validate 유지
- OpenAI Key·OCI·Redis 서버·Nginx·프런트엔드 배포는 필요 없음
- 기존 dev 설정의 민감한 값을 문서나 테스트 출력에 복사하지 않음

## Test execution

구현 완료 후 실행한 검증 명령이다. 계획 당시 기록과 실제 결과는 아래에서 구분한다.

```bash
cd /private/tmp/douid-phase07/apps/api
./gradlew test --tests 'kr.douid.brand.ai.*'
./gradlew test --tests 'kr.douid.brand.shared.config.SecurityConfigTest' --tests 'kr.douid.brand.shared.config.CsrfCookieFlowTest'
./gradlew test
```

필요 의존성 다운로드는 기존 Gradle 흐름을 따른다. 순수 정책/입력/MockMvc slice와 DB 포함 실제 HTTP 테스트를 구분하며 Docker 없는 환경에서 전체 통합 테스트 통과를 주장하지 않는다.

## Test-only source setup

src/test에만 있는 TestConfiguration으로 LlmGateway를 대체한다. 실제 source selector나 local stub 운영 설정은 추가하지 않는다. test gateway는 다음을 제어할 수 있어야 한다.

- 정상 started 이후 Delta 2개 이상, source 완료 latch 해제 전 첫 Delta 수신
- source 정상 반환/Delta 0개, 부분 Delta 후 오류
- 취소 token/interrupt 반응, 연결 close 후 후속 write, cancellation handle 등록 전후 경합
- application deadline 반응 및 취소를 무시하는 source의 container timeout fallback
- source close·실행 점유 반환 관찰, 테스트 종료 시 열린 response/source/latch 정리

RANDOM_PORT 서버와 JDK HttpClient의 streaming InputStream을 사용한다. 첫 Delta가 읽힐 때 source 완료 latch가 아직 닫혀 있음을 검증한 뒤 완료를 허용하고 completed/EOF를 확인한다. 연결 종료 테스트는 stream close 후 추가 write를 유발하여 감지를 기다리고 즉시 callback을 가정하지 않는다. 모든 대기는 유한한 테스트 timeout을 둔다.

## Validation matrix

| 검증 | 성공 기준 | Spec |
| --- | --- | --- |
| message/history shape | 빈/공백/null/잘못된 자료형/role/duplicate/trailing/추가 제어 필드 거부, 이력 생략과 [] 허용 | AT-01~AT-02 |
| byte/code-point 한도 | L-1/L 허용, L+1 시작 전 거부, Content-Length 없는 body도 413 | AT-03 |
| framing/UTF-8 | 특수문자·개행·이모지 및 chunk 병합/분할에도 JSON과 텍스트 보존 | AT-04~AT-05 |
| 상태/오류 | 400/413/415/406/429/503/504/500의 안전한 JSON과 SSE 시작 후 error 구분 | AT-06~AT-07 |
| lifecycle | 완료/오류/timeout/쓰기 실패/취소 경합에서 terminal 최대 1개, 이후 데이터 0개 | AT-08 |
| request guard | 같은 창 30번째 허용/31번째 429, 다음 창 허용, 9번째 실행 429, 실제 worker 종료 후 점유 반환 | AT-09 |
| security | 익명 수락, 새 JSESSIONID 없음, 쿠키 동반 우대 없음, 다른 API CSRF·폐기 경로 차단 유지 | AT-10~AT-11 |
| redispatch/CORS | 허용·비허용 Origin/preflight, 실제 ASYNC/ERROR 경로, 초기화 전 source 미실행/기한 초과 504/초기화 실패 정리, SSE 뒤 JSON 오염 없음 | AT-11 |
| privacy | canary 질문/이력/답변·fake secret·내부 예외가 로그/오류 body에 없음 | AT-12 |
| schema | 새 chat entity/migration 없음, 기존 잔존 테이블 row count 불변 및 측정 구간 대상 INSERT/UPDATE/DELETE 0회(SQL 값/파라미터 로그 없음) | AT-13 |
| environment | 서로 다른 2개 정책 설정으로 동일 wire 계약 유지, default gateway 503 | AT-14 |
| regression | Category/Media/Work/Admin 기존 테스트 통과 | AT-15 |

오류 재디스패치의 보안 설정은 T036에서 구현하고 T038 전에 검증한다. T043은 이를 회귀 확인하며 전역 ASYNC/ERROR 허용으로 대체하지 않는다. 비저장 검증의 fixture 준비 SQL은 측정 전에 완료하고 rollback된 쓰기도 관찰한다. 행 수 불변만으로 기존 행 원문 덮어쓰기까지 차단했다고 판단하지 않는다.

입력별 개별 한도를 검증할 때 다른 한도가 먼저 실패하지 않도록 테스트 설정을 조정한다. execution timeout과 emitter timeout은 둘 다 짧게 하되 순서를 유지한다. 매 테스트 guard/clock/source 상태를 초기화하고 fixed-window 검증에서 실제 60초 sleep을 사용하지 않는다.

## Manual readiness request

기존 프로젝트 환경으로 애플리케이션을 실행한 뒤 별도 터미널에서 사용한다. 주소는 환경값이며 코드에 고정하지 않는다.

```bash
export API_BASE_URL='http://localhost:8080'
curl -i -N "$API_BASE_URL/api/public/ai-chat/stream" \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  --data '{"message":"브랜드를 소개해 줘.","history":[]}'
```

Phase 07 기본 gateway의 기대 결과는 503/AI_UNAVAILABLE JSON이다. 위 curl로 정상 AI 답변이나 SSE가 나와야 하는 것이 아니다. 정상 SSE는 test-only source가 주입된 자동화 환경에서 검증한다. 실제 AI가 없는 운영에서 가짜 답변을 제공하지 않는다.

## Configuration and future deployment

[API 계약](contracts/ai-chat-api.md)의 AI_CHAT_* 환경변수와 기존 APP_ALLOWED_ORIGINS를 환경별 제공한다. 정책 변경은 재시작 시 적용한다. prod datasource 등 기존 설정은 기존 배포 체계를 따른다. 이번 계획에서 메일/Redis 잔존 설정을 정리하거나 OCI/Nginx 구성을 추가하지 않는다.

향후 실제 배포에서 HTTPS, Origin/preflight, 버퍼링 없는 조기 Delta, proxy/server 연결·write timeout, client disconnect, 동시 한도, 취소 지연을 검증한다. application deadline < emitter timeout < 실제 proxy 종료 제한이 되도록 환경을 맞추고, 느린 write의 강제 중단 보장을 가정하지 않는다. local 결과로 실제 배포 통합 검증 완료를 주장하지 않는다.

## Scope of this planning run

setup-plan은 SPECIFY_FEATURE=007-ai-chat-foundation으로 실행, 실제 브랜치는 main 유지. update-agent-context는 기존 지침 파일을 보존하도록 feature 하위 agent-context.md를 대상으로 실행한다. 이 파일은 자동 로드되는 AGENTS.md가 아니다. tasks.md·구현 코드·migration·라이브러리 변경은 없음. specs는 현재 Git ignore 대상이므로 산출물이 자동 추적되지 않는다.

## 구현 전 작업 폴더 분리 (2026-11-13)

- 프런트엔드 #56: `/Users/kongz/Desktop/douid`, `feature/56-admin-portfolio-cms`
- 백엔드 #59: `/private/tmp/douid-phase07`, `feature/59-ai-chat-foundation`, 기준 커밋 `849fca1`
- 기존 폴더의 미커밋 파일 SHA-256 확인 결과 분리 전후 내용 동일. 해당 변경 및 비밀 환경 설정은 새 worktree에 복사하지 않음
- 명세·SpecKit 도구·개발 지침을 복사하고 문서의 실행 대상 절대 경로를 #59 worktree로 갱신
- #59 worktree의 Git ignore는 해당 feature 명세만 추적 가능하게 조정. 원본 ignore는 변경하지 않음
- Java Temurin 25.0.3, Gradle wrapper 9.5.1, Docker client/server 29.9.3 확인
- 요구사항 체크리스트 16/16 완료. 구현 작업 48개는 미완료 상태 유지
- 기존 테스트·resolved 의존성 검증은 구현 착수 시 T002에서 수행. 기존 테스트 보조 클래스의 미커밋 수정도 가져오지 않았으므로 기준 테스트에서 차이가 드러날 수 있음
- Project 상태 변경은 사용자 담당. 구현 시작 전 In Progress 확인 필요
- `/private/tmp`는 임시 경로이므로 명세·코드의 장기 보존은 별도 커밋 등으로 확보 필요. 커밋·푸시는 수행하지 않음

## 구현 기준 확인

T001: #59 worktree/branch, ai/shared 및 Java·테스트·설정 경로 규칙 확인. 기존 준비 변경은 두 ignore와 specs이며 #56 폴더는 수정하지 않음. 새 의존성/스키마/프런트엔드 변경 없이 ai, shared 오류·보안, application.yaml 및 테스트만 대상으로 고정. Dockerfile/배포 이미지 및 npm 배포 패키지가 없어 새 ignore 파일 불필요.

T002: Java 25.0.3, Gradle 9.5.1. 기존 전체 테스트에서 SecurityConfigTest의 캐시된 Context가 종료된 PostgreSQL 컨테이너 주소를 재사용해 JDBC 연결 실패. 반복 연결 대기 실행은 종료하고 PostgresIntegrationTest에 AFTER_CLASS Context 정리를 적용하여 테스트 컨테이너와 수명주기를 일치시킴. 원본 작업 폴더의 수정본을 복사하지 않고 #59 기준 실패를 확인한 뒤 수정. resolved 버전과 최종 전체 회귀 결과는 아래 후속 검증에 기록.

Resolved 확인: Spring Boot 4.0.6, Spring MVC 7.0.7, Jackson 3.1.2(애플리케이션), Flyway 경유 Jackson 2.21.2도 존재. 의존성 변경 없음. `GlobalExceptionHandlerTest` 7개, `AiChatPropertiesTest` 15개 통과. HTTP 오류 3개 assertion 실패→수정 후 통과, 잘못된 설정 14개 assertion 실패→시작 시 검증 후 통과 확인.

Foundation: 고정 창 30회/60초·8개 점유·중복 반환·100회 경쟁, 실행 중 cancel 이후 worker 점유·queue 없는 거부·종료 후 거부 5개 테스트 실패→통과 확인. Reader/application 22개 실패→통과. MockMvc 정상 2개와 RANDOM_PORT 실제 HTTP 조기 Delta/UTF-8/EOF 1개 통과. MVC 초기화 후 started flush→source 실행 순서 적용.

US3: application 맥락 15개와 HTTP validation 7개 통과(역할/과도한 입력 assertion 실패→한도 적용 후 통과). US4: 실행 수명주기 5개·presenter 4개·실제 컨테이너 수명주기 5개 통과. 시작 전 504 JSON, 부분 응답 뒤 오류, 실행 timeout, 취소 무시 source의 emitter timeout, 후속 write에 의한 client close 감지를 포함.

보안: 기존 + 추가 SecurityConfig/CsrfCookieFlow 26개 및 공통 오류 8개 통과. 내부 ERROR matcher는 채팅 POST의 original URI에 한정하며 전역 dispatcher 허용 없음. 환경 2종/default unavailable, 개인정보 canary, 실제 Origin/preflight 검증 통과. 비저장 3개 통과: 정상·실패 행 수 불변/DML 0회, rollback된 INSERT 관찰기 자체 검증, 새 entity/repository/migration 부재. 운영 origin은 APP_ALLOWED_ORIGINS로 제공하며 코드/기본 설정의 localhost는 제거. prod 전용 secret 파일을 만들지 않고 공통 AI_CHAT_* 바인딩이 prod에도 그대로 적용됨.

동시 SSE 초기화의 간헐적 쓰기 실패를 조사 중이며 단일 재실행 통과만으로 해결됐다고 판단하지 않음. 겹치는 두 연결 6회(12요청) 회귀 테스트 추가, 통과 확인. 최종 전체 회귀 결과에서 재확인.

## 동시 첫 쓰기 경합 해결

전체 회귀 259개 중 동시 연결 테스트 1개가 간헐적으로 실패하여 원인 조사. 민감한 메시지 없이 예외 타입/호출 위치만 임시 관찰한 결과, 첫 SSE flush의 Tomcat MimeHeaders 변경과 Spring Security HeaderWriterFilter의 지연 헤더 쓰기가 경합. 공식/로컬 source에서 확인한 setShouldWriteHeadersEagerly(true)를 기존 체인에 적용하여 보안 헤더를 downstream 전 미리 작성. 임시 위치 진단 코드는 제거.

헤더 준비 시점을 확인하는 HTTP probe는 변경 전 assertion 실패→변경 후 통과. 8개 연결 상한/31번째 요청 거부·겹친 두 연결 6회·조기 Delta 및 Admin/CSRF 회귀 31개 통과. API 계약·접근권한·보안 헤더 내용은 유지되며 쓰기 시점만 변경. 이는 plan 이후 실제 통합 테스트에서 발견한 필수 호환 조정.

## 최종 검증 결과 — 2026-11-14

최종 `./gradlew test` 성공(40초), 277개 통과·실패 0·스킵 0. JavaDoc·import/공백 정리의 컴파일 확인 후 최종 계약 대조에서 deadline/Delta 경합을 보강하여 전체 회귀를 다시 실행했다. 종료 사유로 차단된 Delta를 실제 단절로 오인하지 않고 timeout terminal을 유지하는 테스트는 수정 전 실패→수정 후 통과 확인.

| 영역 | 테스트 | 실패 | 스킵 |
| --- | ---: | ---: | ---: |
| AI | 100 | 0 | 0 |
| Work | 56 | 0 | 0 |
| Media | 32 | 0 | 0 |
| Category | 29 | 0 | 0 |
| Shared 보안·오류 등 | 47 | 0 | 0 |
| Auth | 9 | 0 | 0 |
| Admin | 3 | 0 | 0 |
| App context | 1 | 0 | 0 |
| 합계 | 277 | 0 | 0 |

| 완료 기준 | 실행 근거 |
| --- | --- |
| AC-01~03 / SC-01 | reader 15개·application 16개·HTTP validation 8개: 엄격 JSON/byte·code-point 경계/역할·맥락·방어 복사, 잘못된 요청 source 미시작 |
| AC-04~05 / SC-02 | Controller 3개·실제 HTTP streaming 2개: 정상/0 Delta/오류 JSON 필드·UTF-8·framing, 완료 보류 중 첫 Delta 수신, 겹치는 연결 6회 |
| AC-06~08 / SC-03 | Execution 17개·presenter 5개·lifecycle HTTP 5개: 초기화 실패, 시작 전 504, 부분 오류, 양쪽 timeout, 단절, 출력 제한, 제출 거부, 종료 경합 10회, 실제 worker/transport 이중 종료 후 lease 반환 |
| AC-09·15 / SC-04 | SecurityConfig/CsrfCookieFlow 26개 및 기존 Auth/CMS 회귀, 허용/비허용 Origin·preflight, 정확 POST만 CSRF 면제, 내부 ERROR 한정, early security headers |
| AC-10~11 / SC-05 | stateless 3개·privacy 1개: 신규 저장 코드/스키마 없음, 대상 행 수 불변과 INSERT/UPDATE/DELETE 실행 0회, rollback 관찰기 자체 검증, canary 로그/오류 비노출 |
| AC-12~14 / SC-06~07 | config 2종·기본 unavailable·guard HTTP/단위 테스트: Key/OCI/외부 AI/Redis 없이 실행, 환경 정책·Origin 변경, 8개 상한과 31번째 요청 429, 503도 빈도 소비 |
| AC-16 / SC-08 | 공급자/Servlet 독립 application port 및 test-only source로 모든 통신 검증. 운영 기본은 여전히 503 AI_UNAVAILABLE |

테스트 결과 XML/HTML은 `build/test-results/test`, `build/reports/tests/test`에서 확인 가능. 임시 실행 로그는 `/private/tmp/phase07-final-test.log`이며 커밋 대상으로 추가하지 않음.

최종 작업 폴더는 #59 `/private/tmp/douid-phase07`, branch `feature/59-ai-chat-foundation`, 기준 849fca1. 원본 checkout은 최종 읽기 확인 당시 main/f1b95f1(프런트엔드 PR #60 포함)으로 외부에서 진행되어 있었으며 본 작업에서 원본 branch 전환·merge·프런트엔드 수정은 수행하지 않음. #59는 해당 변경을 가져오거나 섞지 않음.

커밋·스테이징·푸시·이슈 상태 변경은 수행하지 않음. 실제 OpenAI는 Phase 08, 운영 HTTPS/proxy/OCI 검증은 후속 단계다. 프로세스 단위 공용 한도는 개인별 공정성/분산 총량을 보장하지 않으며, 취소 무시 실행과 blocking write의 즉시 종료/terminal 전달을 보장하지 않음. `/private/tmp` worktree의 장기 보존은 사용자 커밋 등으로 별도 확보 필요.
