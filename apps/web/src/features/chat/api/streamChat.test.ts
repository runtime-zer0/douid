import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";

import { CHAT_ENDPOINT } from "../content";
import {
  AFTER_COMPLETED,
  EOF_NO_TERMINAL,
  HTTP_429,
  HTTP_503,
  INVALID_DELTA,
  NORMAL,
  NOT_SSE,
  sseResponse,
  splitEveryByte,
  STREAM_ERROR,
  UNKNOWN_EVENT,
} from "../test/fixtures";

import type { ChatStreamEvent } from "./events";
import { streamChat } from "./streamChat";

const BODY = { message: "질문", history: [{ role: "user" as const, content: "이전" }] };

async function run(response: () => Response, signal = new AbortController().signal) {
  const events: ChatStreamEvent[] = [];
  server.use(http.post(CHAT_ENDPOINT, response));
  const outcome = await streamChat(BODY, { signal, onEvent: (event) => events.push(event) });
  return { outcome, events };
}

describe("streamChat", () => {
  it("POST JSON 본문, 쿠키 미전송", async () => {
    let request: Request | undefined;
    server.use(
      http.post(CHAT_ENDPOINT, ({ request: received }) => {
        request = received.clone();
        return sseResponse([NORMAL]);
      }),
    );
    document.cookie = "SESSION=secret";

    await streamChat(BODY, { signal: new AbortController().signal, onEvent: () => {} });

    expect(request?.method).toBe("POST");
    expect(request?.headers.get("content-type")).toBe("application/json");
    expect(request?.headers.get("accept")).toBe("text/event-stream");
    expect(request?.headers.get("cookie")).toBeNull();
    expect(await request?.json()).toEqual(BODY);
    document.cookie = "SESSION=; max-age=0";
  });

  it("NORMAL은 started·delta·delta·completed 후 성공", async () => {
    const { outcome, events } = await run(() => sseResponse(splitEveryByte(NORMAL)));

    expect(events.map((event) => event.type)).toEqual(["started", "delta", "delta", "completed"]);
    expect(outcome).toEqual({ ok: true });
  });

  it("완료 없는 EOF는 INCOMPLETE_STREAM", async () => {
    const { outcome, events } = await run(() => sseResponse([EOF_NO_TERMINAL]));

    expect(events.map((event) => event.type)).toEqual(["started", "delta"]);
    expect(outcome).toEqual({ ok: false, failure: { kind: "protocol", code: "INCOMPLETE_STREAM" } });
  });

  it("error 이벤트는 stream 실패", async () => {
    const { outcome } = await run(() => sseResponse([STREAM_ERROR]));

    expect(outcome).toEqual({ ok: false, failure: { kind: "stream", code: "STREAM_TIMEOUT" } });
  });

  it("잘못된 delta는 INVALID_EVENT, 이벤트 미전달", async () => {
    const { outcome, events } = await run(() => sseResponse([INVALID_DELTA]));

    expect(events).toEqual([]);
    expect(outcome).toEqual({ ok: false, failure: { kind: "protocol", code: "INVALID_EVENT" } });
  });

  it("completed 이후 delta 미전달", async () => {
    const { outcome, events } = await run(() => sseResponse([AFTER_COMPLETED]));

    expect(events.map((event) => event.type)).toEqual(["delta", "completed"]);
    expect(outcome).toEqual({ ok: true });
  });

  it("모르는 이벤트는 전달하지 않고 성공", async () => {
    const { outcome, events } = await run(() => sseResponse([UNKNOWN_EVENT]));

    expect(events).toEqual([
      { type: "delta", text: "앞" },
      { type: "delta", text: "뒤" },
      { type: "completed" },
    ]);
    expect(outcome).toEqual({ ok: true });
  });

  it("429는 code와 Retry-After 보관", async () => {
    const { outcome } = await run(HTTP_429);

    expect(outcome).toEqual({ ok: false, failure: { kind: "http", code: "AI_RATE_LIMITED", retryAfter: 30 } });
  });

  it("503은 AI_UNAVAILABLE", async () => {
    const { outcome } = await run(HTTP_503);

    expect(outcome).toEqual({ ok: false, failure: { kind: "http", code: "AI_UNAVAILABLE" } });
  });

  it("200 HTML은 INVALID_RESPONSE", async () => {
    const { outcome } = await run(NOT_SSE);

    expect(outcome).toEqual({ ok: false, failure: { kind: "protocol", code: "INVALID_RESPONSE" } });
  });

  it("fetch 실패는 network", async () => {
    const { outcome } = await run(() => HttpResponse.error());

    expect(outcome).toEqual({ ok: false, failure: { kind: "network", code: null } });
  });

  it("읽는 중 abort는 AbortError로 reject", async () => {
    const controller = new AbortController();
    server.use(http.post(CHAT_ENDPOINT, () => sseResponse([EOF_NO_TERMINAL, NORMAL], { delayMs: 50 })));

    const promise = streamChat(BODY, {
      signal: controller.signal,
      onEvent: (event) => {
        if (event.type === "delta") controller.abort();
      },
    });

    await expect(promise).rejects.toMatchObject({ name: "AbortError" });
  });
});
