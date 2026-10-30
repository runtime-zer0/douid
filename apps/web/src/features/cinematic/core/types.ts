export type GroupId = "G1" | "G2" | "G3" | "G4";

// mapping.server 검증(fps "30/1", web_pts_time_base "1/15360")으로 보장되는 값
export const WEB_FPS = 30;
export const PTS_TICKS_PER_SECOND = 15360;

/** 서버에서 추출한 열 단위 조회 테이블, 배열 인덱스가 masterFrame */
export type CinematicTable = {
  schema: "DOUID_SCROLL_MAPPING_V2";
  progress: number[];
  groupIndex: number[];
  webFrame: number[];
  ptsTicks: number[];
  groups: { id: GroupId; src: string; masterStart: number; masterEndExclusive: number }[];
};

export type FrameTarget = {
  masterFrame: number;
  groupIndex: number;
  webFrame: number;
  ptsTicks: number;
};
