import type { SseEvent } from "./sseParser";

/** 프런트가 소비하는 스트림 이벤트, `invalid`는 계약 위반으로 종료, `ignored`는 무시 */
export type ChatStreamEvent =
  | { type: "started" }
  | { type: "delta"; text: string }
  | { type: "completed" }
  | { type: "error"; code: string; retryable: boolean }
  | { type: "recommendations"; ids: string[] }
  | { type: "invalid" }
  | { type: "ignored" };

const INVALID = { type: "invalid" } as const;
const IGNORED = { type: "ignored" } as const;

function parseObject(data: string): Record<string, unknown> | null {
  try {
    const value: unknown = JSON.parse(data);
    return typeof value === "object" && value !== null && !Array.isArray(value) ? (value as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

/**
 * SSE 이벤트의 계약 검증(contracts/ai-chat-stream.md §3)
 * 알려진 이벤트 형식 오류는 `invalid`, 잠정 `recommendations` 형식 오류와 모르는 이름은 `ignored`
 */
export function toChatStreamEvent({ type, data }: SseEvent): ChatStreamEvent {
  if (type === "recommendations") {
    const ids = parseObject(data)?.portfolioIds;
    const valid = Array.isArray(ids) && ids.every((id) => typeof id === "string" || typeof id === "number");
    return valid ? { type: "recommendations", ids: ids.map(String) } : IGNORED;
  }
  if (!["started", "message_delta", "completed", "error"].includes(type)) return IGNORED;

  const body = parseObject(data);
  if (!body) return INVALID;
  switch (type) {
    case "started":
      return typeof body.requestId === "string" ? { type: "started" } : INVALID;
    case "message_delta":
      return typeof body.text === "string" ? { type: "delta", text: body.text } : INVALID;
    case "completed":
      return body.status === "success" ? { type: "completed" } : INVALID;
    default:
      return typeof body.code === "string" && typeof body.retryable === "boolean"
        ? { type: "error", code: body.code, retryable: body.retryable }
        : INVALID;
  }
}
