import { describe, expect, it } from "vitest";

import mappingJson from "../config/DOUID_SCROLL_MAPPING.json";
import manifestJson from "../config/DOUID_WEB_GROUP_MANIFEST.json";
import { buildCinematicTable, type ManifestSource, type MappingSource, validateMappingSource } from "./mapping.server";

const mapping = mappingJson as MappingSource;
const manifest = manifestJson as ManifestSource;
const BASE_URL = "/media/cinematic";

describe("buildCinematicTable", () => {
  const table = buildCinematicTable(mapping, manifest, BASE_URL);

  it("3,089개 지점을 열 배열로 추출", () => {
    for (const column of [table.progress, table.groupIndex, table.webFrame, table.ptsTicks]) {
      expect(column).toHaveLength(3089);
    }
  });

  it("진행률은 0에서 1까지 엄격 증가", () => {
    expect(table.progress[0]).toBe(0);
    expect(table.progress[3088]).toBe(1);
    for (let i = 1; i < 3089; i++) expect(table.progress[i]).toBeGreaterThan(table.progress[i - 1]);
  });

  it("그룹 범위가 G1 0~449 / G2 450~1469 / G3 1470~2409 / G4 2410~3088", () => {
    const ranges = [
      [0, 449],
      [450, 1469],
      [1470, 2409],
      [2410, 3088],
    ];
    ranges.forEach(([first, last], groupIndex) => {
      expect(table.groupIndex[first]).toBe(groupIndex);
      expect(table.groupIndex[last]).toBe(groupIndex);
      expect(table.webFrame[first]).toBe(0);
      expect(table.webFrame[last]).toBe(last - first);
    });
  });

  it("모든 지점의 PTS가 webFrame × 512", () => {
    table.ptsTicks.forEach((ticks, i) => expect(ticks).toBe(table.webFrame[i] * 512));
  });

  it("그룹 src는 base URL과 승인 파일명의 조합", () => {
    expect(table.groups.map((group) => group.src)).toEqual(
      [1, 2, 3, 4].map((n) => `${BASE_URL}/4k/DOUID_G${n}_4K_30FPS_WEB_H264.mp4`),
    );
  });

  it("L01 독립 루프를 포함하지 않음", () => {
    expect(table.groups.map((group) => group.id)).toEqual(["G1", "G2", "G3", "G4"]);
    expect(JSON.stringify(table)).not.toContain("L01");
  });
});

describe("validateMappingSource", () => {
  const broken = (mutate: (source: MappingSource) => void) => {
    const source = structuredClone(mapping);
    mutate(source);
    return () => validateMappingSource(source, manifest);
  };

  it("원본은 통과", () => {
    expect(() => validateMappingSource(mapping, manifest)).not.toThrow();
  });

  it.each([
    ["schema 변경", (s: MappingSource) => (s.schema = "DOUID_SCROLL_MAPPING_V1"), /schema/],
    ["lookup_points 하나 삭제", (s: MappingSource) => s.lookup_points.splice(1000, 1), /lookup_points 개수/],
    [
      "progress 역전",
      (s: MappingSource) => (s.lookup_points[10].scroll_progress = s.lookup_points[9].scroll_progress - 0.0001),
      /lookup_points\[10\]\.scroll_progress/,
    ],
    ["pts 불일치", (s: MappingSource) => (s.lookup_points[500].web_pts_ticks += 1), /lookup_points\[500\]\.web_pts_ticks/],
    ["그룹 범위 불일치", (s: MappingSource) => (s.groups[1].master_start_frame = 451), /groups\[1\] 범위/],
    ["seam 17개", (s: MappingSource) => s.source_seams.pop(), /source_seams 개수/],
    [
      "마지막 지점 classification 변경",
      (s: MappingSource) => (s.lookup_points[3088].outgoing_edge_classification = "AUTHORIAL"),
      /outgoing_edge_classification/,
    ],
  ])("%s → 해당 항목을 담은 Error", (_, mutate, message) => {
    expect(broken(mutate)).toThrow(message);
  });
});
