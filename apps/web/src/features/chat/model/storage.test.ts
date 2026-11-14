import { afterEach, describe, expect, it, vi } from "vitest";

import { restoreMessages, safeSessionStorage } from "./storage";

const user = { id: "u1", role: "user", text: "질문", createdAt: 1 };
const done = { id: "a1", role: "assistant", text: "답", createdAt: 2, status: "done", recommendations: ["p"] };

afterEach(() => {
  vi.restoreAllMocks();
});

describe("restoreMessages", () => {
  it("정상 배열은 그대로", () => {
    const failed = { id: "a2", role: "assistant", text: "", createdAt: 3, status: "error", error: { kind: "http", code: "AI_BUSY" } };

    expect(restoreMessages([user, done, failed])).toEqual([user, done, failed]);
  });

  it.each(["pending", "streaming", "unknown"])("%s는 interrupted 오류", (status) => {
    expect(restoreMessages([user, { ...done, status }])[1]).toMatchObject({
      status: "error",
      error: { kind: "interrupted", code: null },
    });
  });

  it.each([
    ["배열 아님", { messages: [] }],
    ["id 타입 오류", [{ ...user, id: 1 }]],
    ["text 누락", [{ id: "u", role: "user", createdAt: 1 }]],
    ["알 수 없는 role", [{ ...user, role: "system" }]],
    ["null 항목", [user, null]],
  ])("%s는 빈 대화", (_, persisted) => {
    expect(restoreMessages(persisted)).toEqual([]);
  });
});

describe("safeSessionStorage", () => {
  it("setItem 용량 초과 예외를 전파하지 않음", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("full", "QuotaExceededError");
    });

    expect(() => safeSessionStorage.setItem("k", "v")).not.toThrow();
  });

  it("sessionStorage 접근이 막혀도 getItem은 null", () => {
    vi.spyOn(window, "sessionStorage", "get").mockImplementation(() => {
      throw new DOMException("denied", "SecurityError");
    });

    expect(safeSessionStorage.getItem("k")).toBeNull();
    expect(() => safeSessionStorage.removeItem("k")).not.toThrow();
  });
});
