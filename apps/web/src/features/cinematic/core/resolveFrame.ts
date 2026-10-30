import type { CinematicTable, FrameTarget } from "./types";

/**
 * 진행률에 가장 가까운 승인 조회 지점의 프레임 선택
 *
 * 동률이면 작은 masterFrame, 0~1 밖은 끝값으로 clamp, 비유한 값은 null
 */
export function resolveFrame(table: CinematicTable, progress: number): FrameTarget | null {
  if (!Number.isFinite(progress)) return null;

  const points = table.progress;
  const last = points.length - 1;
  let masterFrame: number;

  if (progress <= points[0]) {
    masterFrame = 0;
  } else if (progress >= points[last]) {
    masterFrame = last;
  } else {
    // points[low] <= progress < points[high]
    let low = 0;
    let high = last;
    while (high - low > 1) {
      const mid = (low + high) >> 1;
      if (points[mid] <= progress) low = mid;
      else high = mid;
    }
    masterFrame = progress - points[low] <= points[high] - progress ? low : high;
  }

  return {
    masterFrame,
    groupIndex: table.groupIndex[masterFrame],
    webFrame: table.webFrame[masterFrame],
    ptsTicks: table.ptsTicks[masterFrame],
  };
}
