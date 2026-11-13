import { describe, expect, it } from "vitest";

import { fallbackPage, parseUiPage, toApiPage, totalPages } from "./pagination";

describe("pagination", () => {
  it.each([
    [null, 1],
    ["", 1],
    ["abc", 1],
    ["0", 1],
    ["-2", 1],
    ["1.5", 1],
    ["3", 3],
  ])("parseUiPage(%s) = %i", (input, expected) => {
    expect(parseUiPage(input)).toBe(expected);
  });

  it("화면 페이지 1은 API 페이지 0", () => {
    expect(toApiPage(1)).toBe(0);
    expect(toApiPage(3)).toBe(2);
  });

  it("전체 페이지 수는 올림이며 0건이어도 1", () => {
    expect(totalPages(0, 20)).toBe(1);
    expect(totalPages(20, 20)).toBe(1);
    expect(totalPages(21, 20)).toBe(2);
  });

  it("빈 페이지인데 자료가 있으면 마지막 유효 페이지로 복귀", () => {
    expect(fallbackPage({ content: [], totalElements: 21, size: 20 })).toBe(2);
    expect(fallbackPage({ content: [], totalElements: 20, size: 20 })).toBe(1);
  });

  it("자료가 없거나 내용이 있으면 복귀하지 않음", () => {
    expect(fallbackPage({ content: [], totalElements: 0, size: 20 })).toBeNull();
    expect(fallbackPage({ content: [1], totalElements: 21, size: 20 })).toBeNull();
  });
});
