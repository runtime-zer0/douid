import { type GroupId, PTS_TICKS_PER_SECOND, WEB_FPS } from "../core/types";
import type { QaEventInput } from "../diagnostics/qaLog";

export type SeekerStatus = "idle" | "waiting_metadata" | "seeking" | "presented" | "presented_unverified" | "error";

export type SeekerError = { kind: "media_error" | "no_source" | "seek_timeout"; code?: number };

export type SeekerSnapshot = {
  status: SeekerStatus;
  targetWebFrame: number | null;
  presentedFrame: number | null;
  presentedMediaTime: number | null;
  lastLatencyMs: number | null;
  error: SeekerError | null;
};

export type GroupSeeker = {
  setTarget: (webFrame: number, ptsTicks: number) => void;
  /** 대기 중 목표 폐기, 진행 중 seek는 완료만 하고 후속 seek 없음 */
  release: () => void;
  /** 탭 복귀 등으로 완료 신호가 끊긴 seek를 버리고 현재 목표로 다시 seek */
  resume: () => void;
  retry: () => void;
  getSnapshot: () => SeekerSnapshot;
  dispose: () => void;
};

type SeekerOptions = {
  groupId: GroupId;
  onChange?: (snapshot: SeekerSnapshot) => void;
  log?: (event: QaEventInput) => void;
  /** 멈춤 복구로 media를 다시 불러오기 직전 호출, 화면 프레임이 지워지기 전 보존 기회 */
  onBeforeReload?: () => void;
};

type Target = { requestId: number; webFrame: number; ptsTicks: number };
type InFlight = { requestId: number; webFrame: number; requestedAt: number; seeked: boolean };

const HAVE_METADATA = 1;
const NETWORK_NO_SOURCE = 3;
// 합성되지 않는 숨김 video는 rVFC가 오지 않을 수 있어 seeked 이후 대기 상한
const PRESENT_FALLBACK_MS = 120;
// 프레임 시작 시각으로 seek하면 브라우저 내부 시간 변환에서 직전 프레임이 선택되는 사례 실측(QA 2026-10-31), 프레임 가운데로 seek
const HALF_FRAME_TICKS = PTS_TICKS_PER_SECOND / WEB_FPS / 2;
// 완료 신호 없는 seek를 멈춤으로 보는 기준, 실측 최대 지연 약 230ms
const STALE_SEEK_MS = 1500;
// 탭 복귀 후 화면 표시가 확인될 때까지 Safari seek가 멈추는 경우가 잦아 짧게 판정, 실측 그룹 전환 지연 최대 약 155ms
const RESUME_STALE_SEEK_MS = 400;
// 다시 불러와도 연속으로 멈추면 오류 화면으로 넘겨 무한 재시도 방지
const MAX_SEEK_TIMEOUTS = 3;
const HAVE_NOTHING = 0;
const NETWORK_LOADING = 2;

export const isPresented = (status: SeekerStatus) => status === "presented" || status === "presented_unverified";

/**
 * 영상 1개의 seek 요청과 실제 표시 확인 담당
 *
 * 진행 중 seek는 하나만 두고 최신 목표만 대기(latest-wins), 표시 확인은 requestVideoFrameCallback 기준이며
 * seeked만 확인되면 presented_unverified
 */
export function createGroupSeeker(
  video: HTMLVideoElement,
  src: string,
  { groupId, onChange, log, onBeforeReload }: SeekerOptions,
): GroupSeeker {
  let requestCounter = 0;
  let target: Target | null = null;
  let inFlight: InFlight | null = null;
  let frameCallback: number | null = null;
  let fallbackTimer: ReturnType<typeof setTimeout> | null = null;
  let staleTimer: ReturnType<typeof setTimeout> | null = null;
  let consecutiveTimeouts = 0;
  let resumed = false;
  let disposed = false;
  const snapshot: SeekerSnapshot = {
    status: "idle",
    targetWebFrame: null,
    presentedFrame: null,
    presentedMediaTime: null,
    lastLatencyMs: null,
    error: null,
  };
  const supportsFrameCallback = typeof video.requestVideoFrameCallback === "function";

  const emit = () => onChange?.({ ...snapshot });

  function clearPresentWait() {
    if (frameCallback !== null) video.cancelVideoFrameCallback(frameCallback);
    if (fallbackTimer !== null) clearTimeout(fallbackTimer);
    if (staleTimer !== null) clearTimeout(staleTimer);
    frameCallback = null;
    fallbackTimer = null;
    staleTimer = null;
  }

  // Safari는 탭 전환 후 정지 video가 seek에 응답하지 않는 경우가 있어 media를 다시 불러온 뒤 같은 목표로 seek
  function reloadStuckMedia(reason: "seek_timeout" | "unverified_after_resume") {
    inFlight = null;
    clearPresentWait();
    resumed = false;
    consecutiveTimeouts += 1;
    log?.({
      type: "stalled",
      group: groupId,
      detail: `${reason}#${consecutiveTimeouts} rs=${video.readyState} ns=${video.networkState} seeking=${video.seeking}`,
    });

    snapshot.presentedFrame = null;
    snapshot.presentedMediaTime = null;
    if (consecutiveTimeouts >= MAX_SEEK_TIMEOUTS) {
      snapshot.status = "error";
      snapshot.error = { kind: "seek_timeout" };
    } else {
      snapshot.status = target ? "waiting_metadata" : "idle";
      onBeforeReload?.();
      video.load();
    }
    emit();
  }

  function handleSeekTimeout(request: InFlight) {
    if (inFlight === request) reloadStuckMedia("seek_timeout");
  }

  function waitForFrame(request: InFlight) {
    frameCallback = video.requestVideoFrameCallback((_, metadata) => {
      frameCallback = null;
      // seek 전에 합성 대기 중이던 이전 프레임은 건너뜀
      if (Math.round(metadata.mediaTime * WEB_FPS) !== request.webFrame && !request.seeked) {
        waitForFrame(request);
        return;
      }
      complete(request, metadata.mediaTime);
    });
  }

  function startSeek() {
    if (!target) return;
    clearPresentWait();
    const request: InFlight = {
      requestId: target.requestId,
      webFrame: target.webFrame,
      requestedAt: performance.now(),
      seeked: false,
    };
    const seconds = (target.ptsTicks + HALF_FRAME_TICKS) / PTS_TICKS_PER_SECOND;
    inFlight = request;
    snapshot.status = "seeking";
    if (supportsFrameCallback) waitForFrame(request);
    staleTimer = setTimeout(() => handleSeekTimeout(request), resumed ? RESUME_STALE_SEEK_MS : STALE_SEEK_MS);
    log?.({ type: "seek", requestId: request.requestId, group: groupId, webFrame: request.webFrame, seconds });
    video.currentTime = seconds;
    emit();
  }

  function complete(request: InFlight, mediaTime: number | null) {
    if (inFlight !== request) return;
    const verified = mediaTime !== null;
    // 탭 복귀 후 화면 표시 확인 없이 seeked만 오면 Safari에서 다음 seek가 멈추는 전조(QA 2026-10-31 로그)
    if (resumed && !verified && supportsFrameCallback) {
      reloadStuckMedia("unverified_after_resume");
      return;
    }
    inFlight = null;
    clearPresentWait();
    consecutiveTimeouts = 0;
    if (verified) resumed = false;

    const latencyMs = performance.now() - request.requestedAt;
    snapshot.presentedFrame = verified ? Math.round(mediaTime * WEB_FPS) : request.webFrame;
    snapshot.presentedMediaTime = mediaTime;
    snapshot.lastLatencyMs = latencyMs;
    snapshot.status = verified ? "presented" : "presented_unverified";
    log?.({
      type: "presented",
      requestId: request.requestId,
      group: groupId,
      webFrame: snapshot.presentedFrame,
      mediaTime,
      verified,
      latencyMs,
    });
    emit();

    if (target && target.requestId !== request.requestId && target.webFrame !== request.webFrame) {
      log?.({ type: "superseded", requestId: request.requestId, group: groupId });
      startSeek();
    }
  }

  function abandonInFlight() {
    if (!inFlight) return;
    log?.({ type: "superseded", requestId: inFlight.requestId, group: groupId });
    inFlight = null;
    clearPresentWait();
  }

  function setTarget(webFrame: number, ptsTicks: number) {
    if (disposed) return;
    requestCounter += 1;
    target = { requestId: requestCounter, webFrame, ptsTicks };
    snapshot.targetWebFrame = webFrame;

    if (snapshot.status === "error" || inFlight) {
      emit();
      return;
    }
    if (video.readyState < HAVE_METADATA) {
      snapshot.status = "waiting_metadata";
      emit();
      return;
    }
    if (isPresented(snapshot.status) && snapshot.presentedFrame === webFrame) {
      emit();
      return;
    }
    startSeek();
  }

  const handleLoadedMetadata = () => {
    if (snapshot.status === "waiting_metadata" && target) startSeek();
  };

  const handleSeeked = () => {
    const request = inFlight;
    if (!request) return;
    request.seeked = true;
    if (!supportsFrameCallback) {
      complete(request, null);
      return;
    }
    fallbackTimer = setTimeout(() => complete(request, null), PRESENT_FALLBACK_MS);
  };

  const handleError = () => {
    inFlight = null;
    clearPresentWait();
    snapshot.status = "error";
    snapshot.error = {
      kind: video.networkState === NETWORK_NO_SOURCE ? "no_source" : "media_error",
      code: video.error?.code,
    };
    log?.({ type: "error", group: groupId, detail: `${snapshot.error.kind}:${snapshot.error.code ?? "-"}` });
    emit();
  };

  const handleWaiting = () => log?.({ type: "waiting", group: groupId });
  const handleStalled = () => log?.({ type: "stalled", group: groupId });

  const listeners: [string, () => void][] = [
    ["loadedmetadata", handleLoadedMetadata],
    ["seeked", handleSeeked],
    ["error", handleError],
    ["waiting", handleWaiting],
    ["stalled", handleStalled],
  ];
  listeners.forEach(([type, listener]) => video.addEventListener(type, listener));
  video.src = src;

  return {
    setTarget,
    release() {
      target = null;
      snapshot.targetWebFrame = null;
      if (snapshot.status === "waiting_metadata") snapshot.status = "idle";
    },
    resume() {
      if (disposed || snapshot.status === "error") return;
      abandonInFlight();
      resumed = true;
      // 숨김 동안 디코딩 프레임이 버려졌을 수 있어 같은 프레임도 다시 seek
      snapshot.presentedFrame = null;
      if (!target) {
        if (snapshot.status === "seeking") snapshot.status = "idle";
        return;
      }
      if (video.readyState === HAVE_NOTHING) {
        snapshot.status = "waiting_metadata";
        if (video.networkState !== NETWORK_LOADING) video.load();
        emit();
        return;
      }
      startSeek();
    },
    retry() {
      if (disposed) return;
      inFlight = null;
      clearPresentWait();
      consecutiveTimeouts = 0;
      snapshot.error = null;
      snapshot.presentedFrame = null;
      snapshot.presentedMediaTime = null;
      snapshot.status = target ? "waiting_metadata" : "idle";
      video.load();
      emit();
    },
    getSnapshot: () => ({ ...snapshot }),
    dispose() {
      if (disposed) return;
      disposed = true;
      inFlight = null;
      clearPresentWait();
      listeners.forEach(([type, listener]) => video.removeEventListener(type, listener));
      // 4K 디코더·버퍼 즉시 해제
      video.removeAttribute("src");
      video.load();
    },
  };
}
