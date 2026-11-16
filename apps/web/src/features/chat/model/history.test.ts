import { describe, expect, it } from "vitest";

import { buildRequestBody, validateQuestion } from "./history";
import type { AssistantStatus, ChatMessage } from "./types";

let seq = 0;
const user = (text: string): ChatMessage => ({ id: `u${++seq}`, role: "user", text, createdAt: seq });
const assistant = (text: string, status: AssistantStatus = "done"): ChatMessage => ({
  id: `a${++seq}`,
  role: "assistant",
  text,
  createdAt: seq,
  status,
});

describe("validateQuestion", () => {
  it("공백만이면 empty", () => {
    expect(validateQuestion(" \n\t ")).toBe("empty");
  });

  it("2001 code point면 too_long, 이모지는 1자로 계산", () => {
    expect(validateQuestion("👋".repeat(2000))).toBeNull();
    expect(validateQuestion("👋".repeat(2001))).toBe("too_long");
  });

  it("정상이면 null", () => {
    expect(validateQuestion("질문")).toBeNull();
  });
});

describe("buildRequestBody", () => {
  it("질문 원문을 trim 없이 사용", () => {
    expect(buildRequestBody([], "  질문 \n")).toEqual({ message: "  질문 \n", history: [] });
  });

  it("user와 done assistant만, 빈 텍스트 제외", () => {
    const messages = [
      user("q1"),
      assistant("부분", "error"),
      user("q2"),
      assistant("", "done"),
      user("q3"),
      assistant("중단", "cancelled"),
      user("q4"),
      assistant("", "pending"),
      user("q5"),
      assistant("a5"),
    ];

    expect(buildRequestBody(messages, "q").history).toEqual([
      { role: "user", content: "q1" },
      { role: "user", content: "q2" },
      { role: "user", content: "q3" },
      { role: "user", content: "q4" },
      { role: "user", content: "q5" },
      { role: "assistant", content: "a5" },
    ]);
  });

  it("최근 3쌍은 답변까지, 그 이전은 방문자 메시지만 원래 순서로", () => {
    const messages = Array.from({ length: 5 }, (_, index) => [user(`q${index}`), assistant(`a${index}`)]).flat();

    expect(buildRequestBody(messages, "q").history.map((item) => item.content)).toEqual([
      "q0",
      "q1",
      "q2",
      "a2",
      "q3",
      "a3",
      "q4",
      "a4",
    ]);
  });

  it("이력은 최대 64개, 가장 오래된 방문자 메시지부터 제외", () => {
    const messages = Array.from({ length: 100 }, (_, index) => user(`q${index}`));

    const { history } = buildRequestBody(messages, "q");

    expect(history).toHaveLength(64);
    expect(history[0].content).toBe("q36");
    expect(history.at(-1)?.content).toBe("q99");
  });

  it("질문 포함 12000 code point를 넘으면 오래된 방문자 메시지부터 제외", () => {
    const messages = Array.from({ length: 6 }, (_, index) => user(String(index).repeat(2000)));

    expect(buildRequestBody(messages, "q").history.map((item) => item.content[0])).toEqual(["1", "2", "3", "4", "5"]);
  });

  it("요청 본문이 16384 byte를 넘으면 질문과 답변을 쌍 단위로 제외", () => {
    const messages = [user("가".repeat(2000)), assistant("나".repeat(2000)), user("다".repeat(800))];
    const bytes = (body: object) => new TextEncoder().encode(JSON.stringify(body)).length;
    const room = 16384 - bytes({ message: "", history: messages.map((m) => ({ role: m.role, content: m.text })) });

    const atLimit = buildRequestBody(messages, "q".repeat(room));
    const overLimit = buildRequestBody(messages, "q".repeat(room + 1));

    expect(bytes(atLimit)).toBe(16384);
    expect(atLimit.history).toHaveLength(3);
    expect(overLimit.history.map((item) => item.content[0])).toEqual(["다"]);
  });

  it("긴 한국어 답변이 쌓여도 요청 본문은 16384 byte 이내", () => {
    const messages = Array.from({ length: 10 }, (_, index) => [user(`질문${index}`), assistant("답".repeat(1900))]).flat();

    const body = buildRequestBody(messages, "질".repeat(2000));

    expect(new TextEncoder().encode(JSON.stringify(body)).length).toBeLessThanOrEqual(16384);
    expect(body.history.at(-1)?.role).toBe("assistant");
  });

  it("2000 code point를 넘는 답변은 앞 2000 code point만 남김, 이모지는 1자로 계산", () => {
    const messages = [user("q1"), assistant("👋".repeat(2000) + "끝")];

    expect(buildRequestBody(messages, "q").history).toEqual([
      { role: "user", content: "q1" },
      { role: "assistant", content: "👋".repeat(2000) },
    ]);
  });

  it("role은 user·assistant만", () => {
    const { history } = buildRequestBody([user("q"), assistant("a")], "q");

    expect(new Set(history.map((item) => item.role))).toEqual(new Set(["user", "assistant"]));
  });
});
