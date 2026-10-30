import { describe, expect, it } from "vitest";

import mappingJson from "../config/DOUID_SCROLL_MAPPING.json";
import manifestJson from "../config/DOUID_WEB_GROUP_MANIFEST.json";
import { buildCinematicTable, type ManifestSource, type MappingSource } from "./mapping.server";
import { resolveFrame } from "./resolveFrame";

const mapping = mappingJson as MappingSource;
const table = buildCinematicTable(mapping, manifestJson as ManifestSource, "/media/cinematic");
const LAST = 3088;
const masterOf = (progress: number) => resolveFrame(table, progress)?.masterFrame;

/** 고정 seed 의사난수(mulberry32) */
function seededRandom(seed: number) {
  return () => {
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

describe("resolveFrame 전진 (US1)", () => {
  it("3,089개 기준 진행률이 각자의 masterFrame으로 해석됨 (SC-001)", () => {
    table.progress.forEach((progress, master) => {
      expect(resolveFrame(table, progress)).toEqual({
        masterFrame: master,
        groupIndex: table.groupIndex[master],
        webFrame: table.webFrame[master],
        ptsTicks: table.ptsTicks[master],
      });
    });
  });

  it("범위 밖 진행률은 끝값으로 제한", () => {
    expect(masterOf(0)).toBe(0);
    expect(masterOf(-0.2)).toBe(0);
    expect(masterOf(1)).toBe(LAST);
    expect(masterOf(1.5)).toBe(LAST);
  });

  it("비유한 진행률은 null", () => {
    expect(resolveFrame(table, Number.NaN)).toBeNull();
    expect(resolveFrame(table, Number.POSITIVE_INFINITY)).toBeNull();
    expect(resolveFrame(table, Number.NEGATIVE_INFINITY)).toBeNull();
  });

  it("진행률이 증가하면 masterFrame이 감소하지 않음", () => {
    let previous = -1;
    for (let step = 0; step <= 10_000; step++) {
      const master = masterOf(step / 10_000)!;
      expect(master).toBeGreaterThanOrEqual(previous);
      previous = master;
    }
    expect(previous).toBe(LAST);
  });
});

describe("resolveFrame 결정성 (US2, SC-002)", () => {
  const midpoints = table.progress.slice(1).map((progress, i) => (table.progress[i] + progress) / 2);
  const random = seededRandom(20261031);
  const randoms = Array.from({ length: 1000 }, () => random());
  const inputs = [...table.progress, ...midpoints, ...randoms];

  it("정순·역순·셔플 조회 결과가 같음", () => {
    const forward = inputs.map(masterOf);
    const reversed = [...inputs].reverse().map(masterOf).reverse();

    const order = inputs.map((_, i) => i);
    const shuffle = seededRandom(7);
    for (let i = order.length - 1; i > 0; i--) {
      const j = Math.floor(shuffle() * (i + 1));
      [order[i], order[j]] = [order[j], order[i]];
    }
    const shuffled: (number | undefined)[] = [];
    order.forEach((i) => (shuffled[i] = masterOf(inputs[i])));

    expect(reversed).toEqual(forward);
    expect(shuffled).toEqual(forward);
  });

  it("두 지점과의 거리가 정확히 같으면 작은 masterFrame", () => {
    const tiny = { ...table, progress: [0, 0.25, 0.5, 1], groupIndex: [0, 0, 0, 0], webFrame: [0, 1, 2, 3], ptsTicks: [0, 512, 1024, 1536] };
    expect(resolveFrame(tiny, 0.125)?.masterFrame).toBe(0);
    expect(resolveFrame(tiny, 0.375)?.masterFrame).toBe(1);
    expect(resolveFrame(tiny, 0.75)?.masterFrame).toBe(2);
  });

  it("인접 두 지점의 중간값은 더 가까운 쪽, 같으면 작은 masterFrame", () => {
    midpoints.forEach((progress, i) => {
      const distanceLow = progress - table.progress[i];
      const distanceHigh = table.progress[i + 1] - progress;
      // 부동소수 반올림으로 중간값이 한쪽에 치우친 경우는 더 가까운 쪽이 정답
      const expected = distanceLow <= distanceHigh ? i : i + 1;
      expect(masterOf(progress)).toBe(expected);
    });
  });
});

describe("resolveFrame 그룹 경계 (US3, SC-003)", () => {
  it.each([
    [449, 0, 449],
    [450, 1, 0],
    [1469, 1, 1019],
    [1470, 2, 0],
    [2409, 2, 939],
    [2410, 3, 0],
  ])("master %i → 그룹 %i, webFrame %i (정·역방향 동일)", (master, groupIndex, webFrame) => {
    const progress = table.progress[master];
    const expected = { masterFrame: master, groupIndex, webFrame, ptsTicks: webFrame * 512 };
    const before = (table.progress[master - 1] + progress) / 2;
    const after = (progress + table.progress[master + 1]) / 2;

    // 앞에서 들어오는 경우와 뒤에서 되돌아오는 경우
    expect([before, progress].map((p) => resolveFrame(table, p)).at(-1)).toEqual(expected);
    expect([after, progress].map((p) => resolveFrame(table, p)).at(-1)).toEqual(expected);
  });

  it("어떤 결과도 그룹 frame 범위를 벗어나지 않음", () => {
    for (let step = 0; step <= 20_000; step++) {
      const target = resolveFrame(table, step / 20_000)!;
      const group = table.groups[target.groupIndex];
      expect(target.webFrame).toBeGreaterThanOrEqual(0);
      expect(target.webFrame).toBeLessThan(group.masterEndExclusive - group.masterStart);
    }
  });

  it("승인 seam 18개의 양끝 진행률이 양끝 masterFrame으로 해석됨", () => {
    const seams = (mappingJson as unknown as { source_seams: Record<string, number>[] }).source_seams;
    expect(seams).toHaveLength(18);
    seams.forEach((seam) => {
      expect(masterOf(seam.scroll_progress_from)).toBe(seam.edge_from_master_frame);
      expect(masterOf(seam.scroll_progress_to)).toBe(seam.edge_to_master_frame);
    });
  });
});
