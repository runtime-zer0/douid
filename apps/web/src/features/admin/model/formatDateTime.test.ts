import { describe, expect, it } from "vitest";

import { formatDateTime } from "./formatDateTime";

describe("formatDateTime", () => {
  it("마이크로초가 붙은 값도 시간대 변환 없이 분까지 표시", () => {
    expect(formatDateTime("2026-11-13T23:59:58.123456")).toBe("2026-11-13 23:59");
  });

  it("초까지만 있는 값", () => {
    expect(formatDateTime("2026-01-02T03:04:05")).toBe("2026-01-02 03:04");
  });

  it("형식이 짧으면 원문 그대로", () => {
    expect(formatDateTime("2026-01-02")).toBe("2026-01-02");
  });
});
