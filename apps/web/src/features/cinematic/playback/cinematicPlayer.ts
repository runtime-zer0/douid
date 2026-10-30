import type { CinematicTable, FrameTarget } from "../core/types";
import type { QaEventInput } from "../diagnostics/qaLog";
import {
  createGroupSeeker,
  type GroupSeeker,
  isPresented,
  type SeekerError,
  type SeekerSnapshot,
  type SeekerStatus,
} from "./groupSeeker";

export type PlayerPhase = "initializing" | "media_loading" | "seeking" | "presented" | "presented_unverified" | "error";

export type PlayerView = {
  visibleGroupIndex: number | null;
  phase: PlayerPhase;
  error: (SeekerError & { groupIndex: number }) | null;
};

export type PlayerSnapshot = {
  target: FrameTarget | null;
  view: PlayerView;
  seekers: SeekerSnapshot[];
  targetMedia: { readyState: number; networkState: number } | null;
};

export type CinematicPlayer = {
  setTarget: (target: FrameTarget) => void;
  /** 탭 복귀·bfcache 복원 시 끊긴 seek 복구 */
  resume: () => void;
  retry: () => void;
  getSnapshot: () => PlayerSnapshot;
  dispose: () => void;
};

type PlayerOptions = {
  onView: (view: PlayerView) => void;
  log?: (event: QaEventInput) => void;
  /** 멈춤 복구로 해당 그룹 media를 다시 불러오기 직전 호출 */
  onBeforeReload?: (groupIndex: number) => void;
};

const PHASE_BY_STATUS: Record<SeekerStatus, PlayerPhase> = {
  idle: "initializing",
  waiting_metadata: "media_loading",
  seeking: "seeking",
  presented: "presented",
  presented_unverified: "presented_unverified",
  error: "error",
};

/**
 * G1~G4 video 4개의 seek 분배와 표시 그룹 결정
 *
 * 목표 그룹의 표시가 확인되기 전까지 이전 그룹 화면 유지, 목표 ±1 그룹만 preload="auto"
 */
export function createCinematicPlayer(
  videos: HTMLVideoElement[],
  groups: CinematicTable["groups"],
  { onView, log, onBeforeReload }: PlayerOptions,
): CinematicPlayer {
  let target: FrameTarget | null = null;
  let view: PlayerView = { visibleGroupIndex: null, phase: "initializing", error: null };

  const seekers: GroupSeeker[] = groups.map((group, index) =>
    createGroupSeeker(videos[index], group.src, {
      groupId: group.id,
      log,
      onChange: sync,
      onBeforeReload: () => onBeforeReload?.(index),
    }),
  );

  function sync() {
    if (!target) return;
    const groupIndex = target.groupIndex;
    const seeker = seekers[groupIndex].getSnapshot();
    let visibleGroupIndex = view.visibleGroupIndex;

    if (isPresented(seeker.status) && visibleGroupIndex !== groupIndex) {
      log?.({
        type: "visible_group",
        from: visibleGroupIndex === null ? null : groups[visibleGroupIndex].id,
        to: groups[groupIndex].id,
      });
      visibleGroupIndex = groupIndex;
    }
    // 화면에 없고 목표도 아닌 그룹은 남은 seek를 이어가지 않음
    seekers.forEach((other, index) => {
      if (index !== groupIndex && index !== visibleGroupIndex) other.release();
    });

    const next: PlayerView = {
      visibleGroupIndex,
      phase: PHASE_BY_STATUS[seeker.status],
      error: seeker.error ? { ...seeker.error, groupIndex } : null,
    };
    if (
      next.visibleGroupIndex !== view.visibleGroupIndex ||
      next.phase !== view.phase ||
      next.error?.kind !== view.error?.kind ||
      next.error?.groupIndex !== view.error?.groupIndex
    ) {
      view = next;
      onView(view);
    }
  }

  function updatePreload(groupIndex: number) {
    videos.forEach((video, index) => {
      video.preload = Math.abs(index - groupIndex) <= 1 ? "auto" : "metadata";
    });
  }

  return {
    setTarget(next) {
      if (target?.groupIndex !== next.groupIndex) updatePreload(next.groupIndex);
      target = next;
      seekers[next.groupIndex].setTarget(next.webFrame, next.ptsTicks);
      sync();
    },
    resume() {
      seekers.forEach((seeker) => seeker.resume());
    },
    retry() {
      seekers.forEach((seeker) => {
        if (seeker.getSnapshot().status === "error") seeker.retry();
      });
    },
    getSnapshot() {
      const video = target ? videos[target.groupIndex] : null;
      return {
        target,
        view,
        seekers: seekers.map((seeker) => seeker.getSnapshot()),
        targetMedia: video && { readyState: video.readyState, networkState: video.networkState },
      };
    },
    dispose() {
      seekers.forEach((seeker) => seeker.dispose());
    },
  };
}
