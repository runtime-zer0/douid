import { describe, expect, it } from "vitest";

import { toChatStreamEvent } from "./events";

const raw = (type: string, data: unknown) => ({ type, data: typeof data === "string" ? data : JSON.stringify(data) });

describe("toChatStreamEvent", () => {
  it.each([
    [raw("started", { requestId: "r" }), { type: "started" }],
    [raw("message_delta", { text: "안녕" }), { type: "delta", text: "안녕" }],
    [raw("completed", { status: "success" }), { type: "completed" }],
    [
      raw("error", { code: "STREAM_TIMEOUT", message: "x", retryable: true }),
      { type: "error", code: "STREAM_TIMEOUT", retryable: true },
    ],
    [raw("recommendations", { portfolioIds: ["a", 1] }), { type: "recommendations", ids: ["a", "1"] }],
  ])("정상 변환 %#", (input, expected) => {
    expect(toChatStreamEvent(input)).toEqual(expected);
  });

  it.each([
    raw("message_delta", "not json"),
    raw("message_delta", "[]"),
    raw("message_delta", "null"),
    raw("message_delta", { text: 1 }),
    raw("completed", { status: "done" }),
    raw("error", { message: "x", retryable: false }),
    raw("started", "oops"),
  ])("잘못된 알려진 이벤트는 invalid %#", (input) => {
    expect(toChatStreamEvent(input)).toEqual({ type: "invalid" });
  });

  it.each([
    raw("recommendations", { portfolioIds: "a" }),
    raw("recommendations", { portfolioIds: [{}] }),
    raw("recommendations", "broken"),
    raw("ping", {}),
    raw("message", "x"),
  ])("잘못된 추천·모르는 이벤트는 ignored %#", (input) => {
    expect(toChatStreamEvent(input)).toEqual({ type: "ignored" });
  });
});
