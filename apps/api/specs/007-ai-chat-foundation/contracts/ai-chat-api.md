# AI Chat HTTP / SSE Contract

**Version**: Phase 07 initial contract  
**Status**: 구현 및 로컬 계약 검증 완료 — 운영 기본 공급자는 AI_UNAVAILABLE 503  
**Source**: [spec.md](../spec.md), [plan.md](../plan.md)

## Endpoint and browser boundary

`POST /api/public/ai-chat/stream`

- Request Content-Type: application/json
- Accept: text/event-stream과 호환되는 범위. 생략 또는 */* 허용, 명시적 비호환은 406
- 성공: HTTP 200, Content-Type text/event-stream, UTF-8 JSON event data
- 성공 헤더: Cache-Control: no-store, no-transform; X-Accel-Buffering: no. 후자는 향후 proxy의 힌트이며 Nginx 존재나 모든 중간 장비의 buffering 해제를 보장하지 않음
- HTTP/1.1 전용 Connection/Transfer-Encoding을 직접 강제하거나 전체 Content-Length를 계산하지 않음. compression/filter가 응답을 일괄 buffering하지 않는지 실제 배포 시 확인
- 인증·Conversation ID·client token·CSRF 토큰 사전 발급 없음. 정확한 채팅 POST만 CSRF 예외
- Cookie가 함께 와도 공개 권한/한도가 같고 Admin 상태를 변경하지 않음. 채팅을 위해 JSESSIONID/방문자 쿠키를 발급하지 않음
- 허용 Origin은 기존 app.security.allowed-origins에서 설정. CORS preflight는 기존 Spring Security 통합을 사용하며 인증 쿠키 없이 처리. 새 wildcard Origin 허용 없음
- 프런트엔드는 fetch + ReadableStream으로 소비하고 credentials 생략 권장. Zustand/sessionStorage 구현은 별도 범위
- 이 계약은 HTTP POST의 단일 응답이며 EventSource 자동 재연결·Last-Event-ID 재생·재실행 방지를 제공하지 않음

## Request JSON

```json
{
  "message": "패키지 디자인 작업물을 보여줘.",
  "history": [
    {"role": "user", "content": "어떤 디자인을 제작해?"},
    {"role": "assistant", "content": "브랜드와 작업물에 관한 안내를 제공할 수 있어요."}
  ]
}
```

| 필드 | 형태/검증 |
| --- | --- |
| root | JSON object, 추가 JSON 값/trailing token/duplicate key 거부 |
| message | 필수 문자열, Unicode 공백만이면 거부 |
| history | 생략 시 [], 빈 배열 허용, null/비배열 거부 |
| history[] | object, null 항목 거부 |
| role | 필수 문자열, 정확히 user 또는 assistant, 대소문자 변환 없음 |
| content | 필수 비공백 문자열 |
| 기타 필드 | root와 history item 모두 거부, 모델/서버 지시 제어 필드 제공 안 함 |

숫자/boolean을 문자열로 강제 변환하지 않는다. 검증 후 원문을 trim/절단하지 않고 이력 순서와 현재 질문의 구분을 유지한다. assistant도 신뢰할 수 없는 사용자 제공 이력이다. 서버가 과거 메시지의 진위·역할 교대·실제 발화 여부를 보증하지 않는다.

## Policy defaults and environment variables

app.ai-chat 아래 kebab-case 설정을 바인딩한다. 기본값은 초기 단일 프로세스 정책이며 환경변수로 시작 시 재정의한다. 새 필수 인프라나 secret은 없다.

| Property | Default | 환경변수 | 단위/범위 |
| --- | --- | --- | --- |
| max-request-bytes | 16384 | AI_CHAT_MAX_REQUEST_BYTES | 수신 JSON byte, 정확 한도 허용, +1 byte로 초과 판정 |
| max-message-code-points | 2000 | AI_CHAT_MAX_MESSAGE_CODE_POINTS | 현재 질문 Unicode code points |
| max-history-messages | 12 | AI_CHAT_MAX_HISTORY_MESSAGES | 이력 항목 수, 빈 이력은 항상 허용 |
| max-history-message-code-points | 2000 | AI_CHAT_MAX_HISTORY_MESSAGE_CODE_POINTS | 개별 content code points |
| max-context-code-points | 12000 | AI_CHAT_MAX_CONTEXT_CODE_POINTS | 질문+모든 content의 합, role/JSON syntax 제외 |
| rate-limit.requests | 30 | AI_CHAT_RATE_LIMIT_REQUESTS | 프로세스 전체 controller 진입 시도 수 |
| rate-limit.window | 60s | AI_CHAT_RATE_LIMIT_WINDOW | monotonic fixed window |
| max-concurrent-streams | 8 | AI_CHAT_MAX_CONCURRENT_STREAMS | 전역 연결/실행 점유, 허용 설정 1..64 |
| execution-timeout | 60s | AI_CHAT_EXECUTION_TIMEOUT | 실행 수락부터 application deadline |
| emitter-timeout | 65s | AI_CHAT_EMITTER_TIMEOUT | async timeout, execution-timeout보다 커야 함 |
| max-output-bytes | 65536 | AI_CHAT_MAX_OUTPUT_BYTES | Delta text의 UTF-8 누적 byte |
| max-delta-events | 4096 | AI_CHAT_MAX_DELTA_EVENTS | 최대 Delta 개수 |

모든 숫자/duration은 positive이며 0을 무제한으로 해석하지 않는다. context 한도는 질문 한도 이상이어야 한다. 입력 byte 읽기의 +1 overflow를 막고 메모리/worker 설정을 시작 시 검증한다. 정확 한도는 허용하고 초과는 거부한다. output 한도는 source의 생성량을 유한하게 보호하기 위한 정책이며 새로운 SSE event를 추가하지 않는다.

고정 창은 해당 프로세스 guard 생성 시 시작하고 경과한 window마다 초기화한다. 성공/잘못된 body/503/동시 거부 모두 시도를 소비한다. 406/415/CORS 등 controller 전에 거부된 요청과 내부 ASYNC dispatch는 집계하지 않는다. 초과 시 추가 source 작업 없음. 동시 점유는 실제 worker와 transport가 모두 종료된 뒤 반환한다. 분산 총량/개인별 공정성 보장 없음, 재시작 시 초기화된다. 창 경계의 burst 가능성은 고정 창 정책의 알려진 특성이다.

## Before streaming errors

오류 경계는 response commit이다. 검증·빈도·동시 점유·local gateway readiness·executor 제출 수용 확인을 emitter 반환 전에 끝낸다. 제출된 worker는 MVC의 emitter 초기화 완료 신호까지 대기하며 started flush 성공 이후 source를 실행한다. 초기화 전 실패는 source 실행 없이 HTTP 오류/취소·정리로 끝낸다. 초기화 대기 중 deadline도 첫 write 전 재확인하여 미커밋 상태에서 504 JSON으로 끝내며 error만 먼저 보내지 않는다. 폐기된 gate의 뒤늦은 activation은 source를 실행하지 않는다. Accept가 SSE여도 시작 전 오류는 기존 JSON envelope로 반환한다. 공통 MVC 예외가 협상 실패 때문에 빈 body나 500으로 바뀌지 않도록 JSON Content-Type을 명시한다.

| 상황 | HTTP | code | envelope status |
| --- | --- | --- | --- |
| malformed JSON/shape/role/text/context | 400 | INVALID_INPUT | FAILURE |
| 전체 본문 byte 초과 | 413 | PAYLOAD_TOO_LARGE | FAILURE |
| 지원하지 않는 request media type | 415 | UNSUPPORTED_MEDIA_TYPE | FAILURE |
| 호환되지 않는 response Accept | 406 | NOT_ACCEPTABLE | FAILURE |
| 빈도 창 초과 | 429 | AI_RATE_LIMITED | FAILURE |
| 동시 실행/executor 수용 초과 | 429 | AI_BUSY | FAILURE |
| 실제 gateway 미준비 | 503 | AI_UNAVAILABLE | ERROR |
| 커밋 전 실행 deadline 감지 | 504 | AI_TIMEOUT | ERROR |
| 예상하지 못한 내부 오류 | 500 | INTERNAL_ERROR | ERROR |

```json
{
  "status": "FAILURE",
  "data": {
    "code": "INVALID_INPUT",
    "message": "요청 값이 올바르지 않습니다.",
    "fields": [{"field": "message", "message": "질문을 입력해 주세요."}]
  }
}
```

fields는 선택적이며 rejected value를 넣지 않는다. parser/내부 예외 원문은 반환하지 않는다. 기존 다른 API의 메시지와 상태를 일괄 변경하지 않는다. rate 429는 다음 창까지 남은 초(올림, 최소 1)의 Retry-After, concurrency 429는 1초의 Retry-After 힌트를 반환한다. 이는 다음 요청 성공이나 자동 재시도 보장이 아니다.

## SSE framing and events

모든 이벤트는 event 이름, JSON object data, 빈 줄로 구분한다. 아래 각 data 뒤의 빈 줄이 프레이밍이다. event 하나와 네트워크 chunk 하나를 동일시하지 않는다. UTF-8 문자·이벤트가 여러 chunk로 나뉘거나 합쳐질 수 있다.

```text
event: started
data: {"requestId":"example-request-id"}

event: message_delta
data: {"text":"패키지 디자인"}

event: message_delta
data: {"text":" 작업물을 소개해 드릴게요."}

event: completed
data: {"status":"success"}

```

| Event | 필수 data | 계약 |
| --- | --- | --- |
| started | requestId:string | 첫 계약 이벤트, 요청별 1개, 장기 식별 아님 |
| message_delta | text:string | 0..N, 순차 결합, 공백/개행을 포함할 수 있음 |
| completed | status:"success" | 유일한 정상 성공 표시 |
| error | code:string, message:string, retryable:boolean | 실패 terminal |

오류 예시:

```text
event: error
data: {"code":"STREAM_FAILED","message":"응답을 처리하지 못했습니다.","retryable":false}

```

| error code | message | retryable |
| --- | --- | --- |
| STREAM_FAILED | 응답을 처리하지 못했습니다. | false |
| STREAM_TIMEOUT | 응답 시간이 초과되었습니다. | true |
| STREAM_LIMIT_EXCEEDED | 응답이 허용된 범위를 초과했습니다. | false |

정상은 started→Delta 0..N→completed→close, 쓰기 가능한 실패는 started→Delta 0..N→error→close다. terminal 둘은 상호 배타적이며 이후 새 데이터 전송은 없다. started 전송 자체가 실패하거나 연결이 단절되면 terminal은 전달되지 않을 수 있다. completed 없이 EOF면 성공으로 보지 않는다. completed 전송 실패도 서버 성공으로 기록하지 않는다.

response commit 이후 일반 JSON으로 교체하거나 HTTP status를 바꾸지 않는다. application deadline이 먼저 작동하고 worker가 반응할 때만 timeout error를 보낼 수 있다. emitter timeout/onError/IOException 시에는 추가 SSE/JSON 쓰기를 시도하지 않고 취소·정리한다. 이미 진행 중인 blocking write의 즉시 중단은 보장하지 않는다.

상기 네 이벤트와 필수 필드는 유지한다. 추가 이벤트를 도입할 때 소비자는 모르는 이름을 건너뛸 수 있어야 한다. recommendations는 이번 생성/확정 이벤트가 아니다. SSE data와 stream 전체를 ApiResponse로 감싸지 않는다. comment heartbeat는 이번 필수 구현에 포함하지 않으며 즉시 단절 감지를 약속하지 않는다.

## Readiness / testing boundary

기본 gateway는 준비되지 않아 유효한 운영 요청에도 503을 반환한다. 프런트엔드가 이 단계에서 실제 AI 답변을 받을 수 있다고 가정하지 않는다. 정상/오류/지연 source는 src/test의 명시적 TestConfiguration에서만 주입하고 운영 source 선택 설정을 만들지 않는다. OpenAI 호출·키·SDK·토큰 사용량은 Phase 08에서 추가한다.
