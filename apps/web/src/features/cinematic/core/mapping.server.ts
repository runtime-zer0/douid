import "server-only";

import { resolveMediaBaseUrl } from "../config/media";
import mappingJson from "../config/DOUID_SCROLL_MAPPING.json";
import manifestJson from "../config/DOUID_WEB_GROUP_MANIFEST.json";
import type { CinematicTable, GroupId } from "./types";

type SourceGroup = {
  id: string;
  filename: string;
  master_start_frame: number;
  master_end_frame_exclusive: number;
  frame_count: number;
};

type LookupPoint = {
  scroll_progress: number;
  master_frame: number;
  group: string;
  web_frame: number;
  web_pts_ticks: number;
  outgoing_edge_classification: string;
};

export type MappingSource = {
  schema: string;
  main_total_frames: number;
  fps: string;
  web_pts_time_base: string;
  groups: SourceGroup[];
  source_seams: { edge_from_master_frame: number; edge_to_master_frame: number; web_group_from: string; web_group_to: string }[];
  independent_loop: { id: string };
  lookup_points: LookupPoint[];
};

export type ManifestSource = { groups: SourceGroup[] };

const SCHEMA = "DOUID_SCROLL_MAPPING_V2";
const TOTAL_FRAMES = 3089;
const PTS_TICKS_PER_FRAME = 512;
const SEAM_COUNT = 18;
const TERMINAL_CLASSIFICATION = "TERMINAL_FRAME_NO_OUTGOING_EDGE";

// spec 3.2 기준 데이터 계약, 원본과 다르면 보정 없이 빌드 실패
const EXPECTED_GROUPS: { id: GroupId; start: number; endExclusive: number }[] = [
  { id: "G1", start: 0, endExclusive: 450 },
  { id: "G2", start: 450, endExclusive: 1470 },
  { id: "G3", start: 1470, endExclusive: 2410 },
  { id: "G4", start: 2410, endExclusive: 3089 },
];
const EXPECTED_CROSS_GROUP_SEAMS = ["449→450", "1469→1470", "2409→2410"];

/** 원본 매핑·매니페스트 계약 검증, 위반 항목을 모아 하나의 Error로 throw */
export function validateMappingSource(mapping: MappingSource, manifest: ManifestSource): void {
  const errors: string[] = [];
  const check = (ok: boolean, message: string) => {
    if (!ok) errors.push(message);
  };

  check(mapping.schema === SCHEMA, `schema: ${mapping.schema}`);
  check(mapping.main_total_frames === TOTAL_FRAMES, `main_total_frames: ${mapping.main_total_frames}`);
  check(mapping.fps === "30/1", `fps: ${mapping.fps}`);
  check(mapping.web_pts_time_base === "1/15360", `web_pts_time_base: ${mapping.web_pts_time_base}`);
  check(mapping.independent_loop?.id === "L01", `independent_loop: ${mapping.independent_loop?.id}`);

  check(mapping.groups.length === EXPECTED_GROUPS.length, `groups 개수: ${mapping.groups.length}`);
  check(manifest.groups.length === EXPECTED_GROUPS.length, `manifest groups 개수: ${manifest.groups.length}`);
  EXPECTED_GROUPS.forEach((expected, index) => {
    for (const [label, group] of [["groups", mapping.groups[index]], ["manifest groups", manifest.groups[index]]] as const) {
      const ok =
        group?.id === expected.id &&
        group.master_start_frame === expected.start &&
        group.master_end_frame_exclusive === expected.endExclusive &&
        group.frame_count === expected.endExclusive - expected.start;
      check(ok, `${label}[${index}] 범위 불일치: ${group?.id} ${group?.master_start_frame}~${group?.master_end_frame_exclusive}`);
    }
    check(
      mapping.groups[index]?.filename === manifest.groups[index]?.filename,
      `groups[${index}] filename 불일치: ${mapping.groups[index]?.filename} / ${manifest.groups[index]?.filename}`,
    );
  });

  const seams = mapping.source_seams;
  const crossGroup = seams
    .filter((seam) => seam.web_group_from !== seam.web_group_to)
    .map((seam) => `${seam.edge_from_master_frame}→${seam.edge_to_master_frame}`);
  check(seams.length === SEAM_COUNT, `source_seams 개수: ${seams.length}`);
  check(crossGroup.join() === EXPECTED_CROSS_GROUP_SEAMS.join(), `그룹 간 seam: ${crossGroup.join()}`);

  const points = mapping.lookup_points;
  check(points.length === TOTAL_FRAMES, `lookup_points 개수: ${points.length}`);
  check(points[0]?.scroll_progress === 0, `첫 scroll_progress: ${points[0]?.scroll_progress}`);
  check(points.at(-1)?.scroll_progress === 1, `마지막 scroll_progress: ${points.at(-1)?.scroll_progress}`);
  check(
    points.at(-1)?.outgoing_edge_classification === TERMINAL_CLASSIFICATION,
    `마지막 outgoing_edge_classification: ${points.at(-1)?.outgoing_edge_classification}`,
  );
  points.forEach((point, index) => {
    const group = EXPECTED_GROUPS.find((g) => index >= g.start && index < g.endExclusive);
    check(point.master_frame === index, `lookup_points[${index}].master_frame: ${point.master_frame}`);
    check(point.group === group?.id, `lookup_points[${index}].group: ${point.group}`);
    check(point.web_frame === index - (group?.start ?? 0), `lookup_points[${index}].web_frame: ${point.web_frame}`);
    check(point.web_pts_ticks === point.web_frame * PTS_TICKS_PER_FRAME, `lookup_points[${index}].web_pts_ticks: ${point.web_pts_ticks}`);
    if (index > 0) {
      check(point.scroll_progress > points[index - 1].scroll_progress, `lookup_points[${index}].scroll_progress 비증가`);
    }
  });

  if (errors.length > 0) {
    throw new Error(`DOUID_SCROLL_MAPPING 검증 실패 (${errors.length}건)\n${errors.slice(0, 20).join("\n")}`);
  }
}

/** 검증된 원본에서 런타임 열 배열 추출, 값 보정 없음 */
export function buildCinematicTable(mapping: MappingSource, manifest: ManifestSource, baseUrl: string): CinematicTable {
  validateMappingSource(mapping, manifest);
  const points = mapping.lookup_points;
  const groupIds = EXPECTED_GROUPS.map((group) => group.id as string);

  return {
    schema: SCHEMA,
    progress: points.map((point) => point.scroll_progress),
    groupIndex: points.map((point) => groupIds.indexOf(point.group)),
    webFrame: points.map((point) => point.web_frame),
    ptsTicks: points.map((point) => point.web_pts_ticks),
    groups: manifest.groups.map((group, index) => ({
      id: EXPECTED_GROUPS[index].id,
      src: `${baseUrl}/${group.filename}`,
      masterStart: group.master_start_frame,
      masterEndExclusive: group.master_end_frame_exclusive,
    })),
  };
}

/** 저장소의 승인 원본으로 시네마틱 조회 테이블 생성, Server Component 전용 */
export function loadCinematicTable(): CinematicTable {
  return buildCinematicTable(mappingJson as MappingSource, manifestJson as ManifestSource, resolveMediaBaseUrl());
}
