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
      assistant("a1"),
      user("q2"),
      assistant("부분", "error"),
      user("q3"),
      assistant("", "done"),
      user("q4"),
      assistant("중단", "cancelled"),
      user("q5"),
      assistant("", "pending"),
    ];

    expect(buildRequestBody(messages, "q").history).toEqual([
      { role: "user", content: "q1" },
      { role: "assistant", content: "a1" },
      { role: "user", content: "q2" },
      { role: "user", content: "q3" },
      { role: "user", content: "q4" },
      { role: "user", content: "q5" },
    ]);
  });

  it("최근 12개만 오래된 순서로", () => {
    const messages = Array.from({ length: 20 }, (_, index) => (index % 2 === 0 ? user(`q${index}`) : assistant(`a${index}`)));

    const { history } = buildRequestBody(messages, "q");

    expect(history).toHaveLength(12);
    expect(history[0].content).toBe("q8");
    expect(history.at(-1)?.content).toBe("a19");
  });

  it("질문 포함 12000 code point를 넘으면 오래된 것부터 제외", () => {
    const messages = [user("x".repeat(5000)), assistant("y".repeat(4000)), user("z".repeat(3000))];

    expect(buildRequestBody(messages, "q".repeat(1000)).history.map((item) => item.content.length)).toEqual([4000, 3000]);
    expect(buildRequestBody(messages, "q".repeat(5001)).history.map((item) => item.content.length)).toEqual([3000]);
  });

  it("role은 user·assistant만", () => {
    const { history } = buildRequestBody([user("q"), assistant("a")], "q");

    expect(new Set(history.map((item) => item.role))).toEqual(new Set(["user", "assistant"]));
  });
});
