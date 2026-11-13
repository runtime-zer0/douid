# Phase 07 Research

**Date**: 2026-11-13  
**Scope**: 기존 코드 및 공식 자료에 근거한 결정, 실행·배포 결과 아님

## R-01 — MVC streaming

**Decision**: SseEmitter와 전용 유한 실행 풀. 이벤트 JSON record를 변환하고 provider 원본 이벤트는 노출하지 않는다.

**Rationale**: 기존 MVC를 유지하며 명명 이벤트와 flush를 지원한다. emitter 초기화 전 early-send collection과 early-complete 시 callback 미등록 경로가 있으므로 emitter 초기화 뒤 활성화하는 gate를 사용한다. 출력량·이벤트 수도 별도로 유한하게 한다. 최종 수집 결과만으로 점진 전송을 증명하지 않는다.

**Alternatives considered**: WebFlux/Reactor는 새 실행 모델, StreamingResponseBody는 수동 framing 관리가 늘어나 제외. WebSocket은 확정 제외 범위다.

**Sources**: [Spring MVC async](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html), [7.0.7 emitter handler](https://github.com/spring-projects/spring-framework/blob/v7.0.7/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitterReturnValueHandler.java). 로컬 7.0.7 source와 현행 reference를 구분해 확인했다.

활성화 순서는 v7.0.7 소스로 확인했다. DispatcherServlet의 handler.handle이 emitter.initialize를 마친 뒤 finally의 afterConcurrentHandlingStarted를 실행한다. 실행 수용은 controller 반환 전에 확인하고 worker는 이 준비 신호 이후에만 started를 flush하고 source를 실행한다. 초기화 실패/활성화 전 취소도 점유를 반환한다. [DispatcherServlet 순서](https://github.com/spring-projects/spring-framework/blob/v7.0.7/spring-webmvc/src/main/java/org/springframework/web/servlet/DispatcherServlet.java#L897), [emitter initialization](https://github.com/spring-projects/spring-framework/blob/v7.0.7/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitterReturnValueHandler.java#L220).

## R-02 — Timeout / IOException

**Decision**: application deadline 60초, emitter timeout 65초. timer는 사유 선택·취소만 수행하고 반응 가능한 worker가 error 전송을 시도한다. emitter callback은 정리 전용이다.

**Rationale**: 7.0.7 emitter는 onTimeout/onError callback 전에 완료 상태가 되어 그 안의 send가 실패할 수 있다. IOException 이후 연결 종료는 container 책임이므로 complete/completeWithError 재호출을 피한다. onCompletion은 성공 증거가 아니다. 취소를 무시하는 source나 blocking write에는 terminal 전달을 보장하지 않는다.

**Alternatives considered**: onTimeout의 error send는 emitter 상태와 충돌. timer의 직접 write는 다른 timeout을 막을 수 있다. completeWithError를 SSE error 전송 수단으로 쓰지 않는다.

**Sources**: [7.0.7 emitter](https://github.com/spring-projects/spring-framework/blob/v7.0.7/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitter.java), [Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitter.html), [Disconnects](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html#mvc-ann-async-disconnects).

## R-03 — Cancellation / capacity

**Decision**: 8개 고정 worker, SynchronousQueue/AbortPolicy, 대기열 없음. 취소 token·source cancel·Future interrupt 연결, 실제 worker 종료 전 실행 점유 반환 금지.

**Rationale**: Future가 취소되어도 실행이 즉시 끝나지는 않는다. permit을 먼저 반환하면 취소를 무시하는 실행이 누적될 수 있다. 시작 전 실패·등록 경합·worker finally의 점유 소유권을 구분한다.

**Alternatives considered**: 무제한 executor/virtual threads/queue와 강제 thread stop 제외. 동기 순차 callback을 사용하고 복잡한 publisher/event bus는 추가하지 않는다.

**Sources**: [JDK ThreadPoolExecutor](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html), [JDK Future](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Future.html).

## R-04 — Public POST / Admin CSRF

**Decision**: 단일 기존 SecurityFilterChain/IF_REQUIRED/Cookie CSRF 유지. PathPatternRequestMatcher의 method/path를 사용해 정확한 채팅 POST만 CSRF 예외로 둔다.

**Rationale**: 공개 permitAll과 CSRF 면제는 다르다. 이 기능은 쿠키 기반 Admin 권한을 사용/변경하지 않으며 JSON·Origin·실제 요청 제한을 적용한다. 채팅을 위해 세션/CSRF 쿠키 선발급을 요구하지 않는다. CORS는 bot 방어가 아니므로 한도를 대체하지 않는다.

**Alternatives considered**: 전체 CSRF disable, 공개 prefix 전체 면제, 전역 STATELESS, 별도 인증/식별 모델 제외. 새 filter chain은 필요하지 않다.

**Sources**: [Spring Security CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html), [method/path matcher](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/servlet/util/matcher/PathPatternRequestMatcher.Builder.html), [CORS](https://docs.spring.io/spring-security/reference/7.0/servlet/integrations/cors.html), 기존 SecurityConfig/CsrfCookieFlowTest.

## R-05 — Redispatch

**Decision**: 같은 공개 경로 ASYNC는 기존 matcher 사용. 내부 ERROR는 원래 채팅 POST에 한정하고 전역 dispatcher permitAll 금지.

**Rationale**: async 종료/오류에도 security와 exception resolver가 재진입할 수 있다. 커밋된 스트림의 JSON 오염과 미등록 경로 공개를 막아야 한다. 실제 컨테이너에서 원래 요청 속성과 dispatcher를 검증한다.

**Alternatives considered**: 전체 ASYNC/ERROR 허용과 모든 오류의 catch-all JSON 변환 제외.

**Source**: [Spring Security dispatcher authorization](https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/authorize-http-requests.html).

## R-06 — Bounded strict JSON

**Decision**: presentation reader의 readNBytes(16,385)로 실제 본문 제한 후 endpoint 전용 ObjectReader/tree 검사. root/필드/배열/문자열 자료형 확인, duplicate key/trailing token/unknown field 거부 후 request record 생성.

**Rationale**: Content-Length만으로는 미상 길이/chunked 입력을 제한하지 못한다. multipart 제한은 JSON에 적용되지 않는다. Jackson 3 isString/stringValue로 숫자·boolean의 문자열 coercion을 막고 생략 history와 null을 구분한다. 전역 mapper 설정은 변경하지 않는다.

**Alternatives considered**: ContentCachingRequestWrapper의 cacheLimit은 기본 overflow가 수용을 막지 않아 부적합. replay wrapper/filter는 같은 본문을 두 번 읽을 필요가 없어 제외. 전역 coercion 변경은 CMS 계약에 영향이 있다.

**Sources**: [Jackson 3 JsonNode](https://github.com/FasterXML/jackson-databind/blob/3.0/src/main/java/tools/jackson/databind/JsonNode.java), [ContentCachingRequestWrapper](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/ContentCachingRequestWrapper.html), [JDK InputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/InputStream.html#readNBytes(int)).

## R-07 — Abuse guard / defaults

**Decision**: 프로세스 전역 fixed window 30회/60초 + 동시 실행 8개. byte·code point·출력·시간 예산도 환경별 분리한다. IP/쿠키별 맵이나 Redis 없음.

**Rationale**: 유한한 비용/자원 보호를 구현하면서 요청자 식별·프록시 신뢰·key eviction 복잡도를 피한다. 한 방문자가 공용 예산을 소비할 수 있고 재시작/여러 인스턴스에 걸친 총량 보장은 없다. Phase 08 호출 전에도 보호를 유지한다.

**Alternatives considered**: IP별 bucket은 공정성을 개선하지만 식별/맵 관리와 프록시 신뢰가 필요하다. Redis/새 의존성/무제한 실행 후 제한 연기는 제외한다.

**Evidence**: SPEC SEC-009~SEC-012/LOCAL-003. 수치는 이번 계획의 초기 보호 정책이며 성능 보장이나 공식 문서 권장값이 아니다.

## R-08 — Source / schema / tests

**Decision**: LlmGateway의 prepare는 로컬 가용성·취소 가능한 실행 준비만 담당. 기본 adapter는 unavailable, 정상 source는 src/test에만 둔다. 신규 entity/migration 없음. MockMvc와 실제 랜덤 포트 HTTP 검증을 분리한다.

**Rationale**: OpenAI 및 CMS와 독립적으로 계약을 검증한다. source 완료를 latch로 보류한 동안 첫 Delta 수신을 확인하면 전체 버퍼링을 탐지한다. baseline 잔존 테이블이 없다고 가정하지 않고 신규 사용/행 추가가 없음을 확인한다.

**Alternatives considered**: 운영 가짜 답변·새 대화 저장소·실제 OpenAI 계약 테스트·MockMvc 최종 body만으로 streaming 주장·수동 local stub profile 제외.

**Sources**: [MockMvc streaming limits](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/vs-streaming-response.html), 기존 ai CLAUDE/baseline/PostgresIntegrationTest.

## Resolution

미정 연구 항목은 없다. 실제 Spring Security matcher/Jackson reader 옵션·async 종료는 구현 시 프로젝트 resolved 버전으로 컴파일/통합 검증한다. 이번 조사는 그 실행 결과를 대체하지 않는다.
