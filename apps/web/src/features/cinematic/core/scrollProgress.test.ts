import { describe, expect, it } from "vitest";

import { computeProgress } from "./scrollProgress";

const section = { sectionTop: 1000, sectionHeight: 11000, viewportHeight: 1000 };

describe("computeProgress", () => {
  it("섹션 시작 전이면 0", () => {
    expect(computeProgress({ ...section, scrollY: 0 })).toBe(0);
  });

  it("섹션 끝 이후면 1", () => {
    expect(computeProgress({ ...section, scrollY: 50_000 })).toBe(1);
  });

  it("섹션 안에서는 스크롤 거리에 비례", () => {
    expect(computeProgress({ ...section, scrollY: 1000 })).toBe(0);
    expect(computeProgress({ ...section, scrollY: 6000 })).toBe(0.5);
    expect(computeProgress({ ...section, scrollY: 11000 })).toBe(1);
  });

  it("스크롤 가능한 거리가 없으면 0", () => {
    expect(computeProgress({ scrollY: 500, sectionTop: 0, sectionHeight: 800, viewportHeight: 1000 })).toBe(0);
  });

  it("비유한 입력이면 null", () => {
    expect(computeProgress({ ...section, scrollY: Number.NaN })).toBeNull();
    expect(computeProgress({ ...section, scrollY: 0, sectionHeight: Number.POSITIVE_INFINITY })).toBeNull();
  });
});
