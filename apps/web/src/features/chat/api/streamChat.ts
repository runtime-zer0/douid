import { toApiError } from "@/shared/api/client";

import { CHAT_ENDPOINT } from "../content";
import type { ChatFailure } from "../model/failure";

import { type ChatStreamEvent, toChatStreamEvent } from "./events";
import { createSseParser } from "./sseParser";

export type ChatRequestBody = {
  message: string;
  history: { role: "user" | "assistant"; content: string }[];
};

export type StreamOutcome = { ok: true } | { ok: false; failure: ChatFailure };

type DeliveredEvent = Exclude<ChatStreamEvent, { type: "invalid" } | { type: "ignored" }>;

function parseRetryAfter(value: string | null): number | undefined {
  const seconds = value === null ? NaN : Number(value);
  return Number.isInteger(seconds) && seconds >= 0 ? seconds : undefined;
}

/**
 * AI 채팅 POST 요청과 SSE 응답 읽기, 판정 순서는 contracts/ai-chat-stream.md §2
 * `completed`·`error` 이후 수신분은 버리고 reader 취소, `completed` 없는 EOF는 실패
 * `signal` abort 시 원래 abort 사유로 reject(호출자가 취소·타임아웃 구분), 자동 재요청 없음
 */
export async function streamChat(
  body: ChatRequestBody,
  { signal, onEvent }: { signal: AbortSignal; onEvent: (event: DeliveredEvent) => void },
): Promise<StreamOutcome> {
  let response: Response;
  try {
    response = await fetch(new URL(CHAT_ENDPOINT, window.location.origin), {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
      body: JSON.stringify(body),
      credentials: "omit",
      cache: "no-store",
      signal,
    });
  } catch (error) {
    if (signal.aborted) throw error;
    return { ok: false, failure: { kind: "network", code: null } };
  }

  if (!response.ok) {
    const { failure } = await toApiError(response);
    const code = failure.kind === "http" ? failure.code : null;
    const retryAfter = response.status === 429 ? parseRetryAfter(response.headers.get("Retry-After")) : undefined;
    return { ok: false, failure: retryAfter === undefined ? { kind: "http", code } : { kind: "http", code, retryAfter } };
  }
  if (!response.headers.get("content-type")?.startsWith("text/event-stream") || response.body === null) {
    await response.body?.cancel().catch(() => {});
    return { ok: false, failure: { kind: "protocol", code: "INVALID_RESPONSE" } };
  }

  let outcome: StreamOutcome | null = null;
  const parser = createSseParser((raw) => {
    if (outcome) return;
    const event = toChatStreamEvent(raw);
    if (event.type === "ignored") return;
    if (event.type === "invalid") {
      outcome = { ok: false, failure: { kind: "protocol", code: "INVALID_EVENT" } };
      return;
    }
    onEvent(event);
    if (event.type === "completed") outcome = { ok: true };
    if (event.type === "error") outcome = { ok: false, failure: { kind: "stream", code: event.code } };
  });

  const reader = response.body.getReader();
  try {
    while (!outcome) {
      const { done, value } = await reader.read();
      if (done) {
        parser.end();
        break;
      }
      parser.push(value);
    }
  } catch (error) {
    if (signal.aborted) throw signal.reason ?? error;
    return { ok: false, failure: { kind: "network", code: null } };
  }

  if (outcome) {
    await reader.cancel().catch(() => {});
    return outcome;
  }
  return { ok: false, failure: { kind: "protocol", code: "INCOMPLETE_STREAM" } };
}
