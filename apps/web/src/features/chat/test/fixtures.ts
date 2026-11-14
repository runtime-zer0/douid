import { HttpResponse } from "msw";

// contracts/ai-chat-stream.md §4 Fixture, 서버 Phase 07 초안 기준(CONTRACT_PENDING)
const encoder = new TextEncoder();
const bytes = (text: string) => encoder.encode(text);
const event = (name: string, data: unknown) => `event: ${name}\ndata: ${JSON.stringify(data)}\n\n`;

export const NORMAL_TEXT =
  event("started", { requestId: "req-1" }) +
  event("message_delta", { text: "패키지 디자인" }) +
  event("message_delta", { text: " 작업물을 소개해 드릴게요." }) +
  event("completed", { status: "success" });
export const NORMAL_ANSWER = "패키지 디자인 작업물을 소개해 드릴게요.";
export const NORMAL = bytes(NORMAL_TEXT);

export const NORMAL_NO_STARTED = bytes(
  event("message_delta", { text: "미니멀한 패키지 디자인" }) + event("completed", { status: "success" }),
);

export const MULTIBYTE_CRLF_ANSWER = "안녕하세요 👋\n두 줄 답변";
export const MULTIBYTE_CRLF = bytes(
  ": heartbeat\r\n\r\n" +
    'event: message_delta\r\ndata: {"text":\r\ndata: "안녕하세요 👋"}\r\n\r\n' +
    `event: message_delta\r\ndata:${JSON.stringify({ text: "\n두 줄 답변" })}\r\n\r\n` +
    'event: completed\r\ndata: {"status":"success"}\r\n\r\n',
);

export const WITH_RECOMMENDATIONS = bytes(
  event("message_delta", { text: "추천 작업물이에요." }) +
    event("recommendations", { portfolioIds: ["published-a", 404, "published-a", "draft-x"] }) +
    event("completed", { status: "success" }),
);

export const STREAM_ERROR = bytes(
  event("started", { requestId: "req-1" }) +
    event("message_delta", { text: "부분 답변" }) +
    event("error", { code: "STREAM_TIMEOUT", message: "응답 시간이 초과되었습니다.", retryable: true }),
);

export const EOF_NO_TERMINAL = bytes(event("started", { requestId: "req-1" }) + event("message_delta", { text: "부분 답변" }));

export const INVALID_DELTA = bytes(event("message_delta", { text: 1 }));

export const UNKNOWN_EVENT = bytes(
  event("message_delta", { text: "앞" }) +
    "event: ping\ndata: {}\n\n" +
    event("message_delta", { text: "뒤" }) +
    event("completed", { status: "success" }),
);

export const AFTER_COMPLETED = bytes(
  event("message_delta", { text: "완료" }) +
    event("completed", { status: "success" }) +
    event("message_delta", { text: " 이후" }),
);

/** 바이트열을 주어진 위치에서 나눈 청크 목록 */
export function splitAt(source: Uint8Array, ...offsets: number[]): Uint8Array[] {
  const points = [0, ...offsets, source.length];
  return points.slice(1).map((end, index) => source.slice(points[index], end));
}

/** 1바이트씩 나눈 청크 목록 */
export function splitEveryByte(source: Uint8Array): Uint8Array[] {
  return Array.from(source, (_, index) => source.slice(index, index + 1));
}

type StreamOptions = { delayMs?: number; status?: number; headers?: Record<string, string> };

/** 청크마다 지연을 두는 SSE 응답 */
export function sseResponse(chunks: Uint8Array[], { delayMs = 0, status = 200, headers }: StreamOptions = {}) {
  const stream = new ReadableStream<Uint8Array>({
    async start(controller) {
      for (const chunk of chunks) {
        if (delayMs > 0) await new Promise((resolve) => setTimeout(resolve, delayMs));
        controller.enqueue(chunk);
      }
      controller.close();
    },
  });
  return new HttpResponse(stream, { status, headers: { "Content-Type": "text/event-stream", ...headers } });
}

/** 서버 오류 봉투 응답 */
export function jsonFailure(status: number, code: string, headers?: Record<string, string>) {
  return HttpResponse.json(
    { status: status >= 500 ? "ERROR" : "FAILURE", data: { code, message: "서버 오류 메시지" } },
    { status, headers },
  );
}

export const HTTP_429 = () => jsonFailure(429, "AI_RATE_LIMITED", { "Retry-After": "30" });
export const HTTP_503 = () => jsonFailure(503, "AI_UNAVAILABLE");
export const NOT_SSE = () => new HttpResponse("<!doctype html><p>404</p>", { status: 200, headers: { "Content-Type": "text/html" } });
