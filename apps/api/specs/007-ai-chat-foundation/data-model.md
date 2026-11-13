# Phase 07 Data Model

**Status**: 설계 모델, 영속 entity·신규 테이블·migration 없음

## Request / application values

| 값 | 필드 | 검증/책임 |
| --- | --- | --- |
| AiChatRequest | message, history | presentation record, history 생략만 []로 정규화, null 거부 |
| HistoryMessageRequest | role, content | 문자열 자료형, 미허용 필드 거부 |
| AiChatCommand | message, immutable ordered history | HTTP와 독립된 application 입력, 원문 순서 보존 |
| HistoryMessage | USER/ASSISTANT, content | 외부 user/assistant 정확 일치, 모두 client-provided 신뢰 수준 |
| RequestId | 임시 무작위 문자열 | 요청 단위 추적, 장기 신원/재실행 방지 키 금지 |

본문은 전송 byte로, 텍스트는 String.codePointCount로 측정한다. 공백 판정은 Unicode whitespace 및 space character를 포함하며 허용된 텍스트를 trim/절단하지 않는다. escaped JSON의 byte와 decoded code point를 따로 제한한다. role 교대 순서는 강제하지 않는다.

## Application contracts

- **LlmGateway**: 로컬 가용성 확인 및 validated command로 LlmStream 준비. 기본 adapter는 AI_UNAVAILABLE. prepare에서 외부 호출·worker·텍스트 생성 없음
- **LlmStream**: 단일 실행, 동기 순차 Delta callback, best-effort non-blocking cancel 신호, idempotent close. cancel은 thread-safe이며 scheduler에서 네트워크 I/O/종료 대기를 하지 않는다. 실제 source close는 worker 종료 경로에서 수행한다. provider adapter도 병렬 callback 대신 이 순서를 준수
- **StreamSink**: application의 시작/Delta/정상 완료/실패 신호 경계. wire event 이름·JSON·Servlet 타입은 presentation 책임. started 전송 실패면 application source를 실행하지 않음
- **ChatStreamExecutor / StreamActivation**: application의 유한 실행 제출/준비 대기 계약. infrastructure는 실행 풀을, presentation은 HTTP 준비 신호를 연결하며 서로 구현체를 직접 참조하지 않음
- **CancellationToken**: 사유/flag. handle 등록 전 취소를 기억하고 등록 즉시 전달. 즉시 중단 보장 없음
- **AdmissionLease**: 실행 점유의 한 번 반환 권한. 정상 실행에서는 실제 worker 종료와 transport 종료 모두 확인 후 반환. 시작 전 실패는 별도 미실행 분기가 반환
- **ChatRequestGuard**: process-wide 빈도 시도/실행 점유 계약. HTTP/IP/세션 데이터 없음

구체 타입 수는 경계에 필요한 만큼만 유지하고 각 개념마다 wrapper class를 만들지 않는다. application은 HTTP DTO나 infrastructure 구현을 import하지 않는다.

## Event values

| Event | 필수 data | 개수 |
| --- | --- | --- |
| started | requestId:string | 1, 전송 불가능하면 0 |
| message_delta | text:string | 0..N |
| completed | status:"success" | 정상 성공 시 1 |
| error | code:string, message:string, retryable:boolean | 쓰기 가능한 실패 시 1 |

JSON record serialization으로 escaping을 보장한다. text를 JSON에 수동 보간하지 않는다. Delta text의 누적 UTF-8 byte와 이벤트 수로 출력 예산을 검사하고 초과 Delta는 보내지 않는다. started/completed/error에 원문·provider 예외를 넣지 않는다.

## State and ownership

| 상태/사유 | 조건 | 전송/정리 |
| --- | --- | --- |
| VALIDATING | bounded input | 거부 시 일반 HTTP 오류, source 미실행 |
| PREPARED | 검증·가용성 확인·점유 획득 | 구성/제출 실패 시 source close·permit 반환 |
| WAITING_FOR_TRANSPORT | worker 제출 성공, emitter 초기화 대기 | 취소 가능한 activation gate 대기, send/complete 금지 |
| RUNNING | afterConcurrentHandlingStarted 활성화 후 started flush 성공 | application/source 실행, 이후 Delta |
| SUCCEEDED | source 정상 반환, 다른 종료 없음 | completed 1회 시도, complete |
| FAILED | source 오류/출력 한도 | error 1회 시도, complete |
| TIMED_OUT | application deadline 또는 container timeout | 취소, worker가 반응하고 전송 가능할 때만 error, timeout callback은 정리만 |
| CANCELLED / DISCONNECTED | 취소·쓰기 실패·선택된 성공 없는 completion | 이후 전송 차단, 취소·정리, terminal 보장 없음 |
| TRANSPORT_CLOSED | 최종 onCompletion 확인 | timer/연결 정리, 성공 여부 변경 금지, worker 종료도 확인된 경우만 permit 반환 |
| WORKER_EXITED | 실제 worker finally 또는 시작 전 취소 확정 | source close, transport 종료도 확인된 경우만 permit 반환 |

첫 terminal 사유는 atomic하게 보존한다. 전송 결과를 따로 기록하여 completed 쓰기 실패를 성공으로 남기지 않는다. deadline을 선점한 실행의 정상 반환은 completed로 바뀌지 않는다. 사유 선택 이후 새 Delta는 금지하고 선택된 terminal만 한 번 시도한다. 하나의 writer 경계에서 terminal/Delta를 직렬화하고 전송 실패로 terminal이 0개일 수 있음을 허용한다.

Future.cancel 성공은 실제 worker 종료 증거가 아니다. cancel-before-start, 등록 전후 취소, finally vs callback, completed-send IOException 경합을 검증한다. 취소 무시 worker의 실행 점유는 실제 종료 전 재사용하지 않는다. HTTP 연결/timer 자원은 별도로 정리한다. 초기화 전 send/complete를 금지한다. afterConcurrentHandlingStarted의 준비 신호 전에는 source를 실행하지 않으며 complete 호출만으로 물리적 연결 종료를 가정하지 않는다. 이중 종료 조건으로 빠른 source와 느린 클라이언트의 동시 연결 누적도 제한한다.

초기화 gate 대기 중 deadline은 pending timeout으로 기억한다. 초기화 후 첫 write 이전 단일 writer가 기한을 재확인하고 미커밋 상태에서는 MVC 예외 dispatch를 통해 504 JSON으로 끝낸다. source는 시작하지 않는다. 초기화 실패로 폐기된 gate의 후속 activation은 no-op이며 실제 transport가 생성되지 않은 미실행 경로의 점유도 정리한다.

## Configuration

prefix는 app.ai-chat이며 값은 [API 계약](contracts/ai-chat-api.md) 표를 따른다. positive 및 emitter-timeout > execution-timeout 검증, max-context >= max-message, max-request-bytes+1 overflow 검증을 시작 시 적용한다. 비정상 크기의 executor를 만들지 않도록 worker 설정 상한 64를 검증한다. 정책은 시작 시 immutable snapshot이고 hot reload는 범위 밖이다. 0/음수/잘못된 duration을 무제한으로 해석하지 않는다.

## Persistence / privacy

V1 baseline 잔존 conversation/message/client 테이블을 삭제·변경·조회·기록하지 않는다. 채팅 entity/repository를 만들지 않는다. 새 migration 부재와 채팅 전후 해당 row count 불변을 검증한다. 행 수 비교만으로 비저장을 증명하지 않고 테스트 전용 SQL 관찰로 대상 테이블 INSERT/UPDATE/DELETE가 0회임을 확인한다. fixture 준비는 측정 구간 전에 끝내고 rollback된 쓰기도 관찰하며 값/파라미터를 로그에 남기지 않는다. 이는 기존 행 덮어쓰기나 삽입/삭제 후 행 수가 같아지는 경우도 탐지하기 위한 검증이다. 대화 원문은 실행 중 메모리에만 두고 종료 후 참조를 정리한다. 전역 rate counter는 숫자/시간뿐이며 방문자 데이터가 없다. 로그는 requestId·안전한 코드·소요 시간·종료 사유만 남긴다.
