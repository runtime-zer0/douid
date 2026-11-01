import type { QaEventInput } from "../diagnostics/qaLog";

export type LoopPlayer = {
  /** 엔딩 직전 전체 미리 받기 시작 */
  preload: () => void;
  /** 엔딩 진입, 처음부터 재생하고 첫 프레임 표시 확인 후 보이게 함 */
  enter: () => void;
  /** G4로 복귀, 정지하고 숨김 */
  exit: () => void;
  /** 탭 숨김·커튼 완전 덮음 동안 일시정지, 표시 상태는 유지 */
  pauseForHidden: () => void;
  /** 엔딩 안이면 재생 재개 */
  resume: () => void;
  isVisible: () => boolean;
  dispose: () => void;
};

type LoopPlayerOptions = {
  onVisibleChange: (visible: boolean) => void;
  log?: (event: QaEventInput) => void;
  /** 재생할 주소 준비, 기본은 전체를 받아 메모리 Blob URL로 변환 */
  loadSource?: (src: string, signal: AbortSignal) => Promise<string>;
  /** 끝에 닿으면 돌아갈 위치(초), 처음 진입은 항상 0부터 */
  restartAt?: number;
  /** 한 프레임 길이(초), 마지막 프레임 판정용 */
  frameSeconds?: number;
};

/** 응답은 받았지만 성공 상태가 아님, fallback 사유를 네트워크·CORS 실패와 구분하기 위함 */
class LoopFetchError extends Error {
  constructor(readonly status: number) {
    super(`L01 fetch failed: ${status}`);
  }
}

/** L01 전체를 받아 Blob URL 생성 */
async function loadAsObjectUrl(src: string, signal: AbortSignal): Promise<string> {
  const response = await fetch(src, { signal });
  if (!response.ok) throw new LoopFetchError(response.status);
  return URL.createObjectURL(await response.blob());
}

/**
 * S43 독립 루프(L01) 재생 제어, 메인 그룹 player와 상태를 공유하지 않음
 * 첫 프레임이 실제로 표시되기 전·오류·자동재생 차단 시에는 숨겨 두어 아래 G4 마지막 프레임이 그대로 보임
 * Safari는 고비트레이트 4K 루프를 네트워크에서 반복 재생할 때 버퍼를 유지하지 못해 재생이 끊기므로 전체를 메모리에 받아 재생
 * (qa/safari-2026-10-31.md), 받기 실패 시 원본 주소로 직접 재생
 * 반복은 `loop` 속성 대신 마지막 프레임 표시 시 restartAt으로 이동, rVFC 미지원이면 ended 후 이동
 */
export function createLoopPlayer(
  video: HTMLVideoElement,
  src: string,
  { onVisibleChange, log, loadSource = loadAsObjectUrl, restartAt = 0, frameSeconds = 1 / 30 }: LoopPlayerOptions,
): LoopPlayer {
  const supportsFrameCallback = typeof video.requestVideoFrameCallback === "function";
  let active = false;
  let visible = false;
  let failed = false;
  let disposed = false;
  // 탭 숨김·커튼 완전 덮음으로 멈춘 상태, 늦게 끝난 받기·ended가 다시 재생하지 않게 함
  let held = false;
  let playRequest = 0;
  let frameCallback: number | null = null;
  let lastTime = 0;
  let source: string | null = null;
  let loading = false;
  const loadAbort = new AbortController();

  const setVisible = (next: boolean) => {
    if (visible === next) return;
    visible = next;
    onVisibleChange(next);
  };
  const cancelFrameCallback = () => {
    if (frameCallback !== null) video.cancelVideoFrameCallback(frameCallback);
    frameCallback = null;
  };
  const presented = () => {
    if (!active || failed || visible) return;
    setVisible(true);
    log?.({ type: "loop", detail: "presented" });
  };
  const restart = () => {
    video.currentTime = restartAt;
  };
  // 매 프레임 확인, 마지막 프레임이 표시되면 바로 재시작 위치로 이동해 재생 정지 없이 반복
  const onFrame: VideoFrameRequestCallback = (_now, { mediaTime }) => {
    frameCallback = null;
    if (!active || failed || disposed) return;
    presented();
    if (mediaTime >= video.duration - frameSeconds * 1.5) restart();
    frameCallback = video.requestVideoFrameCallback(onFrame);
  };
  const play = () => {
    if (held) return;
    // 이전 요청의 늦은 거절이 새 재생 상태를 지우지 않게 요청을 구분
    const request = ++playRequest;
    if (supportsFrameCallback && frameCallback === null) frameCallback = video.requestVideoFrameCallback(onFrame);
    video.play().catch((error: unknown) => {
      // 재생 시작 전 pause()로 중단된 경우는 오류가 아님, 커튼 완전 덮음·탭 숨김에서 발생
      if (request !== playRequest || disposed || !active) return;
      if (error instanceof DOMException && error.name === "AbortError") return;
      cancelFrameCallback();
      setVisible(false);
      log?.({ type: "loop", detail: "error" });
    });
  };

  // 첫 프레임부터 재생, 받기가 끝나기 전 enter면 받은 직후 시작
  const start = () => {
    if (!active || failed || disposed || source === null) return;
    cancelFrameCallback();
    video.currentTime = 0;
    lastTime = 0;
    play();
  };
  const useSource = (next: string) => {
    source = next;
    video.preload = "auto";
    video.src = next;
    start();
  };
  const preload = () => {
    if (loading || disposed) return;
    loading = true;
    log?.({ type: "loop", detail: "preload_start" });
    loadSource(src, loadAbort.signal).then(
      (url) => {
        if (disposed) {
          if (url.startsWith("blob:")) URL.revokeObjectURL(url);
          return;
        }
        log?.({ type: "loop", detail: "buffered" });
        useSource(url);
      },
      (error: unknown) => {
        if (disposed) return;
        // 네트워크 끊김과 CORS 거부는 브라우저가 같은 TypeError로 주므로 network 하나로 기록
        log?.(
          error instanceof LoopFetchError
            ? { type: "loop", detail: "fallback", reason: "http", status: error.status }
            : { type: "loop", detail: "fallback", reason: "network", status: null },
        );
        useSource(src);
      },
    );
  };

  const handlePlaying = () => {
    if (!supportsFrameCallback) presented();
  };
  // rVFC 미지원이거나 마지막 프레임 표시를 놓친 경우
  const handleEnded = () => {
    if (!active || failed || disposed || held) return;
    restart();
    play();
  };
  // 장시간 체류 시 버벅임 추적용, 루프 한 바퀴마다 누적 드롭 프레임 기록 (미지원 브라우저는 null)
  const handleTimeUpdate = () => {
    if (video.currentTime < lastTime) {
      const quality = video.getVideoPlaybackQuality?.();
      log?.({
        type: "loop",
        detail: "wrap",
        droppedFrames: quality?.droppedVideoFrames ?? null,
        totalFrames: quality?.totalVideoFrames ?? null,
      });
    }
    lastTime = video.currentTime;
  };
  const handleError = () => {
    failed = true;
    cancelFrameCallback();
    setVisible(false);
    log?.({ type: "loop", detail: "error" });
  };
  const handleStalled = () => log?.({ type: "loop", detail: "stalled" });
  const handleWaiting = () => log?.({ type: "loop", detail: "waiting" });

  const listeners: [string, EventListener][] = [
    ["playing", handlePlaying],
    ["ended", handleEnded],
    ["timeupdate", handleTimeUpdate],
    ["error", handleError],
    ["stalled", handleStalled],
    ["waiting", handleWaiting],
  ];
  listeners.forEach(([type, listener]) => video.addEventListener(type, listener));

  return {
    preload,
    enter: () => {
      if (active || disposed) return;
      active = true;
      preload();
      start();
    },
    exit: () => {
      if (!active) return;
      active = false;
      held = false;
      cancelFrameCallback();
      video.pause();
      setVisible(false);
    },
    pauseForHidden: () => {
      if (!active) return;
      held = true;
      video.pause();
    },
    resume: () => {
      held = false;
      if (active && !failed && !disposed && source !== null) play();
    },
    isVisible: () => visible,
    dispose: () => {
      disposed = true;
      active = false;
      cancelFrameCallback();
      listeners.forEach(([type, listener]) => video.removeEventListener(type, listener));
      loadAbort.abort();
      video.pause();
      if (source?.startsWith("blob:")) {
        video.removeAttribute("src");
        video.load();
        URL.revokeObjectURL(source);
      }
    },
  };
}
