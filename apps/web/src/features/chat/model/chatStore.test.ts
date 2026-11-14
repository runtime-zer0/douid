import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";

import { CHAT_ENDPOINT } from "../content";
import {
  AFTER_COMPLETED,
  EOF_NO_TERMINAL,
  HTTP_503,
  INVALID_DELTA,
  MULTIBYTE_CRLF,
  MULTIBYTE_CRLF_ANSWER,
  NORMAL,
  NORMAL_ANSWER,
  NORMAL_NO_STARTED,
  sseResponse,
  splitEveryByte,
  UNKNOWN_EVENT,
  WITH_RECOMMENDATIONS,
} from "../test/fixtures";

import { hasActiveRequest, resetChatForTest, selectPhase, useChatStore } from "./chatStore";
import type { AssistantMessage } from "./types";

const encode = (text: string) => new TextEncoder().encode(text);
const delta = (text: string) => encode(`event: message_delta\ndata: ${JSON.stringify({ text })}\n\n`);
const COMPLETED = encode('event: completed\ndata: {"status":"success"}\n\n');
const STORAGE_KEY = "douid-ai-chat";

let requests: unknown[] = [];

function respond(response: () => Response | Promise<Response>) {
  server.use(
    http.post(CHAT_ENDPOINT, async ({ request }) => {
      requests.push(await request.json());
      return response();
    }),
  );
}

const state = () => useChatStore.getState();
const lastAssistant = () => state().messages.at(-1) as AssistantMessage;
const settled = () => vi.waitFor(() => expect(hasActiveRequest()).toBe(false));
const never = () => new Promise<Response>(() => {});

beforeEach(() => {
  resetChatForTest();
  requests = [];
});

afterEach(() => {
  vi.useRealTimers();
});

describe("send", () => {
  it("직후 user + pending assistant, connecting", () => {
    respond(never);

    state().send("질문");

    expect(state().messages.map((message) => message.role)).toEqual(["user", "assistant"]);
    expect(lastAssistant().status).toBe("pending");
    expect(selectPhase(state())).toBe("connecting");
    expect(state().draft).toBe("");
  });

  it("delta마다 streaming으로 누적, completed 후 done", async () => {
    respond(() => sseResponse([delta("패키지"), delta(" 디자인"), COMPLETED], { delayMs: 20 }));

    state().send("질문");
    await vi.waitFor(() => expect(lastAssistant().text).toBe("패키지"));
    expect(selectPhase(state())).toBe("streaming");
    await settled();

    expect(lastAssistant()).toMatchObject({ text: "패키지 디자인", status: "done" });
    expect(selectPhase(state())).toBe("completed");
  });

  it("진행 중 재호출은 무시", async () => {
    respond(() => sseResponse([NORMAL], { delayMs: 20 }));

    state().send("하나");
    state().send("둘");
    await settled();

    expect(requests).toHaveLength(1);
    expect(state().messages).toHaveLength(2);
  });

  it("공백만이면 메시지·요청 없음", async () => {
    respond(() => sseResponse([NORMAL]));

    state().send("   ");

    expect(state().messages).toEqual([]);
    expect(requests).toHaveLength(0);
  });

  it("started 없는 스트림도 완료", async () => {
    respond(() => sseResponse([NORMAL_NO_STARTED]));

    state().send("질문");
    await settled();

    expect(lastAssistant()).toMatchObject({ text: "미니멀한 패키지 디자인", status: "done" });
  });

  it("빈 답변 completed는 done, 텍스트 빈 문자열", async () => {
    respond(() => sseResponse([COMPLETED]));

    state().send("질문");
    await settled();

    expect(lastAssistant()).toMatchObject({ text: "", status: "done" });
  });

  it("두 번째 질문은 첫 대화를 history로 전송", async () => {
    respond(() => sseResponse([NORMAL]));

    state().send("첫 질문");
    await settled();
    state().send("두 번째");
    await settled();

    expect(requests[1]).toEqual({
      message: "두 번째",
      history: [
        { role: "user", content: "첫 질문" },
        { role: "assistant", content: NORMAL_ANSWER },
      ],
    });
  });
});

describe("스트림 정확성", () => {
  it("1바이트 청크도 한 번에 보낸 결과와 같음", async () => {
    respond(() => sseResponse(splitEveryByte(MULTIBYTE_CRLF)));

    state().send("질문");
    await settled();

    expect(lastAssistant()).toMatchObject({ text: MULTIBYTE_CRLF_ANSWER, status: "done" });
  });

  it("완료 없는 EOF는 부분 텍스트 유지, INCOMPLETE_STREAM", async () => {
    respond(() => sseResponse([EOF_NO_TERMINAL]));

    state().send("질문");
    await settled();

    expect(lastAssistant()).toMatchObject({
      text: "부분 답변",
      status: "error",
      error: { kind: "protocol", code: "INCOMPLETE_STREAM" },
    });
  });

  it("모르는 이벤트는 무시하고 앞뒤 결합", async () => {
    respond(() => sseResponse([UNKNOWN_EVENT]));

    state().send("질문");
    await settled();

    expect(lastAssistant()).toMatchObject({ text: "앞뒤", status: "done" });
  });

  it("completed 이후 delta는 반영하지 않음", async () => {
    respond(() => sseResponse([AFTER_COMPLETED]));

    state().send("질문");
    await settled();

    expect(lastAssistant()).toMatchObject({ text: "완료", status: "done" });
  });

  it("잘못된 delta는 error, 이전 메시지 불변", async () => {
    respond(() => sseResponse([NORMAL]));
    state().send("첫 질문");
    await settled();
    const before = state().messages.slice();

    respond(() => sseResponse([INVALID_DELTA]));
    state().send("두 번째");
    await settled();

    expect(state().messages.slice(0, 2)).toEqual(before);
    expect(lastAssistant()).toMatchObject({ status: "error", error: { kind: "protocol", code: "INVALID_EVENT" } });
  });
});

describe("세션 저장·복원", () => {
  it("완료 후 messages와 version만 저장", async () => {
    respond(() => sseResponse([NORMAL]));
    state().open();
    state().setDraft("입력 중");

    state().send("질문");
    await settled();

    const stored = JSON.parse(window.sessionStorage.getItem(STORAGE_KEY)!);
    expect(Object.keys(stored).sort()).toEqual(["state", "version"]);
    expect(stored.version).toBe(1);
    expect(Object.keys(stored.state)).toEqual(["messages"]);
    expect(stored.state.messages).toHaveLength(2);
  });

  it("저장값을 rehydrate하면 복원, 진행 중이던 답변은 interrupted", async () => {
    const messages = [
      { id: "u1", role: "user", text: "질문", createdAt: 1 },
      { id: "a1", role: "assistant", text: "부분", createdAt: 2, status: "streaming" },
    ];
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ state: { messages }, version: 1 }));

    await useChatStore.persist.rehydrate();

    expect(state().messages[0]).toEqual(messages[0]);
    expect(lastAssistant()).toMatchObject({ text: "부분", status: "error", error: { kind: "interrupted", code: null } });
    expect(requests).toHaveLength(0);
  });

  it("다른 version은 빈 대화", async () => {
    window.sessionStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ state: { messages: [{ id: "u1", role: "user", text: "q", createdAt: 1 }] }, version: 0 }),
    );

    await useChatStore.persist.rehydrate();

    expect(state().messages).toEqual([]);
  });

  it("깨진 JSON은 예외 없이 빈 대화", async () => {
    window.sessionStorage.setItem(STORAGE_KEY, "{broken");

    await expect(Promise.resolve(useChatStore.persist.rehydrate())).resolves.not.toThrow();

    expect(state().messages).toEqual([]);
    expect(requests).toHaveLength(0);
  });
});

describe("중단·새 대화", () => {
  it("streaming 중 cancel은 부분 텍스트 유지, 이후 delta 무시", async () => {
    respond(() => sseResponse([delta("앞"), delta("뒤"), COMPLETED], { delayMs: 30 }));

    state().send("질문");
    await vi.waitFor(() => expect(lastAssistant().text).toBe("앞"));
    state().cancel();
    await new Promise((resolve) => setTimeout(resolve, 120));

    expect(lastAssistant()).toMatchObject({ text: "앞", status: "cancelled" });
    expect(hasActiveRequest()).toBe(false);
  });

  it("connecting 중 cancel은 빈 텍스트로 cancelled", () => {
    respond(never);

    state().send("질문");
    state().cancel();

    expect(lastAssistant()).toMatchObject({ text: "", status: "cancelled" });
  });

  it("streaming 중 reset은 대화·저장값 삭제, 늦은 결과 무시", async () => {
    respond(() => sseResponse([delta("앞"), delta("뒤"), COMPLETED], { delayMs: 30 }));

    state().send("질문");
    await vi.waitFor(() => expect(lastAssistant().text).toBe("앞"));
    state().reset();
    await new Promise((resolve) => setTimeout(resolve, 120));

    expect(state().messages).toEqual([]);
    const stored = window.sessionStorage.getItem(STORAGE_KEY);
    expect(stored === null || JSON.parse(stored).state.messages.length === 0).toBe(true);
  });

  it("reset 직후 send는 새 요청 델타만 반영", async () => {
    let call = 0;
    respond(() => {
      call += 1;
      return call === 1
        ? sseResponse([delta("이전"), delta("이전2"), COMPLETED], { delayMs: 40 })
        : sseResponse([delta("새 답변"), COMPLETED], { delayMs: 5 });
    });

    state().send("첫 질문");
    await vi.waitFor(() => expect(lastAssistant().text).toBe("이전"));
    state().reset();
    state().send("새 질문");
    await settled();
    await new Promise((resolve) => setTimeout(resolve, 120));

    expect(state().messages.map((message) => message.text)).toEqual(["새 질문", "새 답변"]);
  });

  it("완료 후 cancel은 변화 없음", async () => {
    respond(() => sseResponse([NORMAL]));
    state().send("질문");
    await settled();
    const before = state().messages;

    state().cancel();

    expect(state().messages).toBe(before);
  });
});

describe("추천", () => {
  it("진행 중 recommendations를 중복 없이 저장", async () => {
    respond(() => sseResponse([WITH_RECOMMENDATIONS]));

    state().send("질문");
    await settled();

    expect(lastAssistant()).toMatchObject({ status: "done", recommendations: ["published-a", "404", "draft-x"] });
  });

  it("completed 이후 recommendations는 무시", async () => {
    respond(() =>
      sseResponse([delta("답"), COMPLETED, encode('event: recommendations\ndata: {"portfolioIds":["a"]}\n\n')]),
    );

    state().send("질문");
    await settled();

    expect(lastAssistant().recommendations).toBeUndefined();
  });
});

describe("실패·재시도", () => {
  it("90초가 지나면 abort하고 timeout", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    respond(never);

    state().send("질문");
    await vi.waitFor(() => expect(requests).toHaveLength(1));
    vi.advanceTimersByTime(90_000);
    vi.useRealTimers();
    await settled();

    expect(lastAssistant()).toMatchObject({ status: "error", error: { kind: "timeout", code: null } });
  });

  it("retry는 실패 답변을 지우고 같은 질문으로 1회 요청", async () => {
    let call = 0;
    respond(() => (++call === 1 ? HTTP_503() : sseResponse([NORMAL])));

    state().send("질문");
    await settled();
    expect(lastAssistant()).toMatchObject({ status: "error", error: { kind: "http", code: "AI_UNAVAILABLE" } });

    state().retry();
    state().retry();
    await settled();

    expect(requests).toEqual([
      { message: "질문", history: [] },
      { message: "질문", history: [] },
    ]);
    expect(state().messages.map((message) => message.role)).toEqual(["user", "assistant"]);
    expect(lastAssistant()).toMatchObject({ text: NORMAL_ANSWER, status: "done" });
  });

  it("실패 후 아무 동작이 없으면 재요청 없음", async () => {
    respond(HTTP_503);

    state().send("질문");
    await settled();
    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(requests).toHaveLength(1);
  });

  it("history에는 실패한 assistant를 넣지 않음", async () => {
    let call = 0;
    respond(() => (++call === 1 ? sseResponse([EOF_NO_TERMINAL]) : sseResponse([NORMAL])));

    state().send("첫 질문");
    await settled();
    state().send("두 번째");
    await settled();

    expect(requests[1]).toEqual({ message: "두 번째", history: [{ role: "user", content: "첫 질문" }] });
  });

  it("fetch 실패는 network", async () => {
    respond(() => HttpResponse.error());

    state().send("질문");
    await settled();

    expect(lastAssistant().error).toEqual({ kind: "network", code: null });
  });
});
