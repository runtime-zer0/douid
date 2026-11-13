# Implementation Plan: AI Chat Foundation & SSE Contract

**Branch**: `feature/59-ai-chat-foundation` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)  
**Feature**: `007-ai-chat-foundation`  
**Input**: `/private/tmp/douid-phase07/apps/api/specs/007-ai-chat-foundation/spec.md`  
**Status**: 구현 및 로컬 검증 완료 (2026-11-14)

## Summary

기존 Spring MVC에 `POST /api/public/ai-chat/stream`을 추가한다. 제한된 JSON 입력을 검증하고 application use case와 공급자 중립 `LlmGateway`를 통해 실행한 뒤 presentation의 `SseEmitter`로 네 이벤트를 전송한다. 실제 OpenAI·CMS 검색·방문자 인증·대화 저장소는 추가하지 않는다. 기본 운영 gateway는 시작 전 503을 반환하며 정상 스트리밍은 test-only source로 검증한다.

기존 단일 SecurityFilterChain·Admin Session·쿠키 CSRF를 유지하고 정확한 채팅 POST만 CSRF 예외로 둔다. 프로세스 전역 빈도 제한과 고정 실행 풀을 사용한다. 구현 순서는 tasks.md의 48개 작업으로 관리하며 최종 로컬 검증을 완료했다.

## Technical Context

**Language/Version**: Java 25  
**Primary Dependencies**: Spring Boot 4.0.6, Spring MVC, Spring Security, Jackson 3, Jakarta Validation; 기존 Gradle 의존성 재사용, 신규 의존성 없음  
**Storage**: 채팅은 일시 메모리만 사용. 기존 PostgreSQL/Flyway는 CMS·인증 회귀 검증에 유지, 신규 migration 없음  
**Testing**: JUnit 5, AssertJ, Mockito, MockMvc, Spring Boot Test, PostgreSQL Testcontainers, JDK HttpClient  
**Target Platform**: 로컬 JVM/내장 Servlet 컨테이너, 향후 OCI Linux; 현재 배포 서버 없음  
**Project Type**: backend web-service  
**Performance Goals**: 소스 완료 이전 첫 Delta 수신, 기본 최대 8개 worker 및 전역 30회/60초; 운영 처리량 SLA가 아닌 초기 보호 정책  
**Constraints**: 본문 16 KiB, 질문 2,000 code points, 이력 12개/각 2,000 code points, 질문 포함 맥락 12,000 code points, 실행 기한 60초, emitter timeout 65초  
**Scale/Scope**: 단일 프로세스 공개 endpoint 1개, 고정 SSE 이벤트 4종. Redis·WebSocket·OpenAI SDK·프런트엔드 변경 없음

Spring Framework 7.0.7 로컬 cache/source를 조사했고 현행 공식 reference와 구분했다. resolved 의존성 변경이나 버전 업그레이드는 이번 작업이 아니다. 정책·오류·wire 계약은 [API 계약](contracts/ai-chat-api.md), 상태·소유권은 [data-model.md](data-model.md), 근거·대안은 [research.md](research.md)에 기록한다.

## Constitution Check

`.specify/memory/constitution.md`는 미작성 템플릿이다. 완성된 헌법으로 간주하지 않고 CLAUDE 지침과 사용자 결정을 아래 gate로 적용한다.

| Gate | 조사 전 | 설계 후 | 근거 |
| --- | --- | --- | --- |
| 범위·산출물 위치 | PASS | PASS | API specs 하위 문서만 작성, 코드/브랜치/tasks 생성 없음 |
| feature-first / 의존 방향 | PASS | PASS | presentation→application, infrastructure→application port, HTTP 타입의 application 유입 없음 |
| ai 공급자 경계 | PASS | PASS | 기존 ai 지침의 LlmGateway 재사용, provider client 없음 |
| 기존 인증·공개 경로 보존 | PASS | PASS | 정확 POST CSRF 예외, Admin Session·CSRF·기본 거부 유지 |
| 개인정보·stateless | PASS | PASS | 원문 DB/로그 저장 없음, 방문자 세션/장기 식별 생성 없음 |
| 스키마 안전성 | PASS | PASS | 잔존 baseline 유지, 신규 채팅의 테이블 사용 없음 |
| 자원 제한 | PASS | PASS | 본문·맥락·실행·출력·시간 모두 유한, 대기열 없음 |
| 로컬 독립 검증 | PASS | PASS | test-only gateway 및 랜덤 포트 HTTP, 기존 DB 테스트 기반 유지 |
| 일반 응답 래퍼 | 사용자 예외 | 사용자 예외 | 명시적 SSE data만 raw JSON, 시작 전 오류와 다른 API는 기존 래퍼 유지 |
| 프로젝트 지침 구조 | PASS | PASS | AGENTS/CLAUDE 수정 없이 context도 feature 문서로 보관 |

미해결 gate는 없다. PASS는 설계 적합성이며 구현 검증 통과를 뜻하지 않는다. 계획 산출물의 링크·필수 섹션·정책 대응·코드 fence·미해결 연구 항목 부재를 확인했다. 애플리케이션 테스트는 실행하지 않았다.

## Project Structure

### Documentation (this feature)

절대 기준: `/private/tmp/douid-phase07/apps/api/specs/007-ai-chat-foundation/`

```text
spec.md
plan.md
research.md
data-model.md
quickstart.md
agent-context.md
checklists/requirements.md
contracts/ai-chat-api.md
```

`tasks.md`는 구현 순서와 완료 상태를 포함한다. SpecKit 루트는 `/private/tmp/douid-phase07/apps/api`, Git worktree 루트는 `/private/tmp/douid-phase07`이다. setup은 `SPECIFY_FEATURE=007-ai-chat-foundation`으로 실행했다. JSON의 BRANCH는 feature 선택값이고 계획 작성 당시 브랜치는 main이었으며 현재 구현 브랜치는 feature/59-ai-chat-foundation이다. HAS_GIT=false는 API 하위에 `.git`이 없는 스크립트 탐지 한계이며 저장소 부재를 뜻하지 않는다.

### Source Code (repository root)

다음은 이후 구현의 예정 배치이며 이번에 생성한 코드가 아니다. 기준은 `/private/tmp/douid-phase07/apps/api/`다.

```text
src/main/java/kr/douid/brand/ai/
  presentation/                 # controller, bounded strict reader, request records
  presentation/stream/          # SSE presenter, async activation interceptor, lifecycle
  application/                  # AiChatUseCase, command and validated history
  application/port/             # LlmGateway, execution/activation, sink, admission contracts
  infrastructure/stream/        # unavailable gateway, bounded execution/scheduling
  infrastructure/guard/         # process-local request/concurrency guard
  infrastructure/config/        # AiChatProperties and feature wiring
src/main/java/kr/douid/brand/shared/
  config/SecurityConfig.java    # exact POST CSRF exception, scoped redispatch
  exception/                   # neutral TOO_MANY_REQUESTS classification
  presentation/                # standard HTTP exceptions, committed stream guard
src/main/resources/application.yaml
src/main/resources/application-prod.yaml
src/test/java/kr/douid/brand/ai/ # same layers, test-only source, HTTP integration
src/test/java/kr/douid/brand/shared/ # security/error mapping regression
```

**Structure Decision**: ai가 공개 AI 안내 흐름을 소유한다. 영속 상담의 chat/client는 재사용하지 않는다. entity가 필요하지 않으므로 빈 domain 패키지·repository를 만들지 않는다. 기존 지침의 `LlmGateway` 외에 중복 EventSource gateway를 추가하지 않는다. 각 개념마다 별도 클래스가 반드시 필요한 것은 아니다.

### Application and transport boundary

1. Controller가 application의 요청 빈도 확인을 호출한다. MVC의 Content-Type/Accept 매핑을 통과한 최초 요청만 집계하며 ASYNC dispatch에서 중복 집계하지 않는다.
2. Presentation reader는 Servlet input을 최대 `max-request-bytes + 1`만 읽는다. 초과는 파싱 전 413, 정상 크기는 Jackson tree로 구조·문자열 자료형을 검증한 뒤 record로 만든다. 같은 input을 @RequestBody로 다시 읽지 않는다.
3. request→command 변환 후 AiChatUseCase가 텍스트/role/맥락 한도를 검사하고 gateway의 로컬 가용성 확인 및 취소 가능한 실행 준비를 수행한다. prepare는 외부 호출이나 텍스트 생성을 하지 않는다.
4. 전역 실행 점유를 획득한 prepared execution을 SSE presenter가 전달받는다. callback을 먼저 등록한 뒤 use case의 start를 호출한다. use case가 application의 ChatStreamExecutor port를 통해 전용 executor에 제출한다. presentation이 infrastructure executor를 직접 주입/호출하지 않는다. 제출 거부는 emitter 반환 전에 자원을 정리하고 429로 응답한다.
5. 제출된 worker는 취소 가능한 activation gate에서 대기한다. 정확한 endpoint의 AsyncHandlerInterceptor.afterConcurrentHandlingStarted가 MVC emitter 초기화 이후 gate를 열어 준다. gate와 sink의 application 계약은 Servlet 타입 없이 준비/종료 신호만 표현한다. 그 뒤 worker가 started 전송/flush에 성공한 경우에만 application/source 실행을 호출한다. gateway는 동기식 순차 Delta callback을 제공하며 application에는 Servlet/SseEmitter/provider 원본 이벤트 타입을 전달하지 않는다.
6. source 정상 반환도 이미 취소/타임아웃이 결정되었다면 completed로 바꾸지 않는다. presenter가 이벤트 매핑·전송·transport 종료를 담당한다.

Controller는 HTTP 변환과 전달만 수행한다. 핵심 검증·수락·gateway 실행은 application use case 책임이며 이벤트 리스너로 옮기지 않는다. 준비된 실행이 emitter 구성 실패로 시작되지 못하면 close하여 점유를 반환한다. gateway 준비 실패도 동기 HTTP 오류로 매핑한다. 준비된 source가 있는데 실행 점유 확보가 실패한 경우에도 source를 close한다. activation 이전 source 실행은 없고 초기화 실패·요청 처리 예외도 대기 worker를 취소한다. 초기화 전에 deadline이 오면 취소 사유만 기록하고 source 실행을 금지한다. 초기화 후 단일 writer가 첫 write 전에 deadline을 재확인하여 미커밋 상태면 AiTimeoutException으로 completeWithError를 호출, 공통 handler의 504 JSON으로 끝낸다. 이 초기화 후/커밋 전 경로만 MVC 예외 dispatch를 허용하며 커밋 후에는 사용하지 않는다. deadline만으로 gate 대기 worker를 소실시키지 않고 준비 신호 이후 오류 종료를 맡긴다. 초기화 실패/transport 종료로 gate 자체가 폐기되면 뒤늦은 activation은 no-op이다. interceptor는 transport 준비 신호만 주며 비즈니스 흐름을 event listener에 위임하는 구조가 아니다.

### Execution and lifecycle

- 전용 ThreadPoolExecutor: core=max=`max-concurrent-streams`(기본 8), SynchronousQueue, AbortPolicy. 무제한 queue/virtual thread나 MVC 전역 executor 변경 없음
- 전용 단일 scheduled executor: 실행별 deadline 1개, 취소 timer 즉시 제거. timer는 종료 사유 선택·취소 신호만 전달하고 네트워크 write/write lock 대기를 하지 않음
- 실행 기한 60초는 실행 수락 시점부터 측정. worker가 취소에 반응하면 안전한 STREAM_TIMEOUT을 시도. emitter timeout 65초는 취소 지연·전송 불가의 최종 transport 종료 경로
- onTimeout/onError/onCompletion은 정리 fallback. 그 안에서 새 SSE를 보내지 않으며 onCompletion을 성공 증거로 사용하지 않음
- send IOException은 transport 실패로 남기고 작업 취소·정리만 수행. container가 종료를 담당하므로 complete/completeWithError를 다시 호출하지 않음
- 요청별 단일 writer/직렬화 경계에서 Delta/terminal 순서 유지. atomic terminal 사유 선택 후 새 Delta를 시작하지 않고 선택된 terminal만 최대 한 번 전송 시도. 이미 닫힌 transport에는 terminal도 보내지 않음. 이미 진행 중인 socket write의 철회는 보장하지 않음
- 정상 terminal 또는 쓰기 가능한 오류 terminal 이후 complete 호출. 커밋 후 application 실패를 completeWithError로 일반 JSON handler에 넘기지 않음
- emitter 초기화 전 send/complete는 금지. activation gate는 초기화 후 전송을 보장하며 early-complete에 의해 callback 등록이 생략되는 누수도 방지. 출력 예산은 별도로 Delta text 누적 64 KiB/4,096개 적용, 초과 Delta는 보내지 않고 STREAM_LIMIT_EXCEEDED로 실패
- Future.isDone은 실제 실행 종료 증거가 아님. source close는 실제 worker finally에서 수행. 실행 점유는 실제 worker 종료와 transport 종료가 모두 확인된 뒤 한 번 반환. 실행 전 취소/제출 거부는 미실행 분기가 한 번 반환
- transport timer/연결 자원과 실행 점유 정리를 구분. TRANSPORT_CLOSED는 최종 onCompletion에서 확인하고 terminal을 큐에 넣거나 complete를 호출한 것만으로 연결이 닫혔다고 보지 않음. worker가 먼저 끝나도 연결 종료까지 점유를 유지하며, 취소 무시 source도 실제 종료 전 재사용하지 않아 누적 실행을 차단. blocking write의 즉시 강제 종료는 보장하지 않음

### Security and request protection

기존 체인에서 정확 method/path matcher(`POST /api/public/ai-chat/stream`)만 CSRF ignore로 설정한다. CookieCsrfTokenRepository·Admin SecurityContextRepository·IF_REQUIRED 정책은 유지한다. 채팅은 Admin 권한을 사용/변경하지 않으며 세션 생성이나 CSRF 토큰 강제 조회를 하지 않는다. 소비자는 credentials 생략을 권장하되 함께 온 쿠키가 권한이나 요청 한도를 높이지 않는다.

ASYNC 재디스패치는 동일 공개 경로 matcher를 사용한다. 전역 ASYNC/ERROR permitAll은 추가하지 않는다. `/error`로 이동하는 내부 ERROR dispatch는 원래 method/path가 정확한 채팅 POST인 경우에만 허용하고 실제 컨테이너 테스트로 확인한다. 외부 `/error` 및 다른 경로는 기본 거부다. 내부 ERROR dispatch 보안 설정은 US4의 T036에서 구현하고 T038의 실제 HTTP 검증 전에 완료한다. US5의 T043은 해당 설정을 뒤늦게 추가하는 단계가 아니라 환경 연결 및 보안 회귀 확인 단계다.

빈도 제한은 프로세스 전체 고정 창 30회/60초다. monotonic clock 주입, 창 교체와 증가의 atomic 처리가 필요하다. 초과는 429와 다음 창까지 Retry-After를 반환한다. IP·쿠키·Origin별 bucket이나 무제한 key map은 없다. 재시작 초기화·인스턴스별 독립·개인별 공정성 미보장을 명시한다.

잘못된 본문·가용성 503·동시성 429도 빈도 시도를 반환하지 않는다. CORS 및 MVC 406/415처럼 controller 진입 전 거부는 이 카운터 대상이 아니다. 전체 HTTP DDoS 보호가 아니라 최소 AI 실행 보호다. 동시 점유 8개 초과와 executor 제출 거부는 429이며 대기열을 만들지 않는다.

### Error integration and privacy

DomainException/IntegrationException 기반 공통 handler를 재사용하고 ai 고유 코드/예외는 ai 안에 둔다. 429에는 중립 DomainErrorType.TOO_MANY_REQUESTS와 상태 매핑만 추가한다. unavailable은 기존 IntegrationErrorType.SERVICE_UNAVAILABLE을 사용한다. 입력 byte 초과·media type·malformed JSON은 HTTP 입력 오류로 공통 handler에서 413/415/406/400을 매핑한다. shared가 ai 예외를 import하지 않는다.

기존 catch-all이 MVC 406/415를 500으로 바꾸지 않도록 표준 예외별 처리와 회귀 테스트를 포함한다. 커밋된 SSE의 IOException/async timeout/error dispatch에서는 일반 JSON을 쓰지 않도록 feature 중립 transport guard를 둔다. 일반 API의 오류 래퍼는 보존한다.

로그는 임시 requestId, 안전한 코드, 소요 시간, 종료 사유만 기록한다. request/response body나 원인 throwable의 원문 메시지를 새로 기록하지 않는다. JSON parser 오류의 입력 조각도 외부 메시지에 넣지 않는다. 새 public/protected 직접 선언 메서드와 역할 경계 타입의 JavaDoc은 기존 API 지침의 형식/예외를 준수한다. baseline 잔존 테이블은 수정·사용하지 않는다.

### Verification design

| 대상 | 방식 | Spec 추적 |
| --- | --- | --- |
| 입력·전역 빈도·맥락 | 순수 JUnit/Mockito, clock 주입, L-1/L/L+1 | AT-01~AT-03, AT-09 |
| 종료 상태·경합 | latch/barrier, 취소 등록 전후·중복 완료·취소 무시 worker | AT-07~AT-09 |
| HTTP·오류·보안 | MockMvc, 실제 JSON 타입·Accept·쿠키·Origin·CSRF | AT-04, AT-06, AT-10~AT-12 |
| 점진 수신·단절 | RANDOM_PORT + JDK HttpClient InputStream, source 완료 latch 이전 첫 Delta | AT-05, AT-07~AT-08 |
| DB·기존 기능 | PostgresIntegrationTest/Flyway validate, 채팅 전후 행 수 불변·대상 INSERT/UPDATE/DELETE 0회 관찰, 기존 전체 회귀 | AT-11, AT-13, AT-15 |
| 설정·기본 gateway | 서로 다른 2개 설정, test-only source, 기본 운영 503 | AT-14~AT-15 |

실제 HTTP 통합 검증은 test-only gateway와 기존 컨테이너 기반 app context를 사용하므로 Docker가 필요하다. 순수 정책/reader/MockMvc slice에는 DB 의존성을 추가하지 않는다. deadline 단위 검증은 제어 clock/scheduler, HTTP fallback은 짧은 테스트 전용 timeout과 bounded latch 대기를 사용한다. 로컬 결과로 실제 HTTPS·proxy buffering·운영 용량 검증을 주장하지 않는다.

## Complexity Tracking

정당화할 구조적 위반은 없다. 고정 SSE 형식만 사용자 명시 요구에 따른 공통 JSON 래퍼 예외다. 신규 라이브러리·테이블·상시 서비스·중복 gateway·프런트엔드 코드는 추가하지 않는다.

## 구현 검증 및 계획 보완

전체 277개(실패/스킵 0) 및 최종 컴파일 통과, 상세 AC/SC 근거는 quickstart.md 참조. feature-first·의존 방향·기존 인증·비저장·유한 자원·로컬 독립 검증 gate 충족. 새 의존성/테이블/프런트엔드 구현 없음.

실제 테스트에서 확인한 보완:

- MVC emitter 초기화 후 활성화 gate 유지. Spring Security의 지연 헤더 쓰기와 첫 SSE flush의 Tomcat MimeHeaders 경쟁이 재현되어 기존 HeaderWriterFilter에 eager 쓰기 옵션 적용. 보안 헤더 의미와 Admin 접근/CSRF 정책을 보존하며 결정적 초기 헤더 probe와 동시 연결/전체 회귀로 검증
- PostgreSQL 컨테이너가 클래스마다 종료되지만 캐시 Context는 남는 기존 실패를 재현하여 PostgresIntegrationTest에 AFTER_CLASS 정리 적용
- MVC interceptor 등록은 presentation/AiChatWebConfig에 두고 infra config에서 presentation을 import하지 않음
- 취소 사유와 gate/worker 핸들은 AiChatExecution 내부에 모아 별도 CancellationToken·StreamActivation wrapper를 추가하지 않음. 소스는 제출 전 등록되며 취소/늦은 초기화/실제 종료 소유권을 검증
- 운영 전용 secret 설정 파일은 새로 만들지 않음. 공통 application.yaml의 APP_ALLOWED_ORIGINS 및 12개 AI_CHAT_* 바인딩이 profile과 무관하게 적용

운영 기본 503과 후속 Phase 경계 유지. 실제 OCI·HTTPS·proxy buffering/timeout·운영 규모 부하 및 개인정보 법적 적합성은 이번 검증에 포함하지 않음.
