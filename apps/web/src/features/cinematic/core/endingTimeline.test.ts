import { describe, expect, it } from "vitest";

import { resolveEndingState } from "./endingTimeline";

const samples = Array.from({ length: 1001 }, (_, index) => index / 1000);

describe("resolveEndingState", () => {
  it.each([
    [0, "G4_END"],
    [0.1, "S43_SETTLE"],
    [0.2, "LOGO_REVEAL"],
    [0.4, "LOGO_HOLD"],
    [0.65, "RESISTANCE"],
    [0.75, "CURTAIN"],
    [0.9, "CURTAIN"],
    [1, "COVERED"],
  ])("e=%s → %s", (e, phase) => {
    expect(resolveEndingState(e)?.phase).toBe(phase);
  });

  it.each([
    [0.1, 0],
    [0.2, 0],
    [0.3, 0.5],
    [0.4, 1],
    [0.7, 1],
    [1, 1],
  ])("e=%s 로고 투명도 %s", (e, opacity) => {
    expect(resolveEndingState(e)?.logoOpacity).toBeCloseTo(opacity);
  });

  it.each([
    [0.5, 0],
    [0.75, 0],
    [0.875, 0.5],
    [1, 1],
  ])("e=%s 커튼 진행률 %s", (e, curtain) => {
    expect(resolveEndingState(e)?.curtainProgress).toBeCloseTo(curtain);
  });

  it("커튼 진행률은 e에 대해 단조 증가", () => {
    const curtains = samples.map((e) => resolveEndingState(e)!.curtainProgress);
    curtains.slice(1).forEach((value, index) => expect(value).toBeGreaterThanOrEqual(curtains[index]));
  });

  it("커튼이 올라오기 시작하면 로고는 완전 표시, 정착 전에는 로고 없음", () => {
    samples.forEach((e) => {
      const state = resolveEndingState(e)!;
      if (state.curtainProgress > 0) expect(state.logoOpacity).toBe(1);
      if (state.phase === "G4_END" || state.phase === "S43_SETTLE") expect(state.logoOpacity).toBe(0);
    });
  });

  it("범위 밖 입력은 0~1로 고정", () => {
    expect(resolveEndingState(-0.5)?.phase).toBe("G4_END");
    expect(resolveEndingState(1.5)?.phase).toBe("COVERED");
  });

  it.each([NaN, Infinity, -Infinity])("비유한 입력 %s은 null", (e) => {
    expect(resolveEndingState(e)).toBeNull();
  });

  it("순방향·역방향 조회 순서와 무관하게 같은 결과", () => {
    const forward = samples.map(resolveEndingState);
    const backward = [...samples].reverse().map(resolveEndingState).reverse();
    expect(backward).toEqual(forward);
  });
});
