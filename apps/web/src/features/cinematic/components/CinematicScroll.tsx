"use client";

import { type ReactNode, type RefObject, useEffect, useRef, useState, useSyncExternalStore } from "react";

import { LOOP_FRAME_SECONDS, LOOP_RESTART_SECONDS, resolveLoopSrc } from "../config/media";
import { type EndingPhase, resolveEndingState } from "../core/endingTimeline";
import { computeProgress } from "../core/scrollProgress";
import { resolveFrame } from "../core/resolveFrame";
import type { CinematicTable } from "../core/types";
import { createQaLog } from "../diagnostics/qaLog";
import { type CinematicPlayer, createCinematicPlayer, type PlayerView } from "../playback/cinematicPlayer";
import { createLoopPlayer } from "../playback/loopPlayer";
import { cn } from "@/shared/lib/utils";

import { CinematicOverlay } from "./CinematicOverlay";
import { DiagnosticsPanel, type DiagnosticsSource } from "./DiagnosticsPanel";
import { StaticCinematic } from "./StaticCinematic";

type Mode = "scroll" | "static";

const STATIC_MODE_QUERY = "(prefers-reduced-motion: reduce), (pointer: coarse)";
const INITIAL_VIEW: PlayerView = { visibleGroupIndex: null, phase: "initializing", error: null };
const RAF_STALE_MS = 250;
// 메인 구간은 PHASE 01 스크롤 거리 그대로 두고 S43·S50 엔딩 구간만 뒤에 덧붙임 (research R5), 섹션 높이 클래스와 함께 수정
const MAIN_VH = 1500;
const ENDING_VH = 400;
const LOOP_PRELOAD_PROGRESS = 0.95;

function subscribeMode(onChange: () => void) {
  const query = window.matchMedia(STATIC_MODE_QUERY);
  query.addEventListener("change", onChange);
  return () => query.removeEventListener("change", onChange);
}
const getMode = (): Mode => (window.matchMedia(STATIC_MODE_QUERY).matches ? "static" : "scroll");
// 서버와 hydration 첫 렌더는 모드 미정(null)으로 같은 마크업 유지
const getServerMode = (): Mode | null => null;

const subscribeNothing = () => () => {};
const getDebug = () => new URLSearchParams(window.location.search).get("cinematic-debug") === "1";
const getServerDebug = () => false;

type CinematicScrollProps = {
  table: CinematicTable;
  /** S43에 표시할 브랜드 로고, 조립 계층에서 주입 */
  endingMark: ReactNode;
};

export function CinematicScroll({ table, endingMark }: CinematicScrollProps) {
  const sectionRef = useRef<HTMLElement>(null);
  const mode = useSyncExternalStore(subscribeMode, getMode, getServerMode);
  const debug = useSyncExternalStore(subscribeNothing, getDebug, getServerDebug);
  const isStatic = mode === "static";

  return (
    <section
      ref={sectionRef}
      aria-label="DOUID 시네마틱"
      data-mode={mode ?? undefined}
      className={cn("relative bg-black", isStatic ? "h-[200svh]" : "h-[1900vh]")}
    >
      <a
        href="#site-footer"
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:rounded-md focus:bg-background focus:px-4 focus:py-2 focus:text-foreground"
      >
        시네마틱 건너뛰기
      </a>
      {isStatic ? (
        <StaticCinematic table={table} endingMark={endingMark} />
      ) : (
        <ScrollStage table={table} endingMark={endingMark} sectionRef={sectionRef} active={mode === "scroll"} debug={debug} />
      )}
    </section>
  );
}

type ScrollStageProps = {
  table: CinematicTable;
  endingMark: ReactNode;
  sectionRef: RefObject<HTMLElement | null>;
  active: boolean;
  debug: boolean;
};

function ScrollStage({ table, endingMark, sectionRef, active, debug }: ScrollStageProps) {
  const videoRefs = useRef<(HTMLVideoElement | null)[]>([]);
  const loopRef = useRef<HTMLVideoElement>(null);
  const logoRef = useRef<HTMLDivElement>(null);
  const curtainRef = useRef<HTMLDivElement>(null);
  const playerRef = useRef<CinematicPlayer | null>(null);
  const diagnosticsRef = useRef<DiagnosticsSource>({ progress: null, player: null });
  const coverRef = useRef<HTMLCanvasElement>(null);
  const [view, setView] = useState<PlayerView>(INITIAL_VIEW);
  const [loopVisible, setLoopVisible] = useState(false);

  useEffect(() => {
    const section = sectionRef.current;
    const cover = coverRef.current;
    const loopVideo = loopRef.current;
    const logo = logoRef.current;
    const curtain = curtainRef.current;
    if (!active || !section || !cover || !loopVideo || !logo || !curtain) return;

    // 페이지 이동으로 언마운트될 때 ref가 effect 정리보다 먼저 null이 되므로, 남은 rAF가 null video를 건드리지 않도록 복사본 사용
    const videos = [...videoRefs.current] as HTMLVideoElement[];
    const qaLog = createQaLog(debug);

    // Safari는 탭 숨김 동안 프레임을 버리거나 복구 load() 때 화면을 지워 블랙 플래시가 생김
    // 화면 그룹의 마지막 프레임을 canvas로 덮고 새 프레임 표시 확인 후 걷어냄, 같은 task 안에서 바로 반영되도록 DOM 직접 갱신
    const isCovering = () => cover.style.visibility === "visible";
    const showCover = () => {
      const visibleGroupIndex = player.getSnapshot().view.visibleGroupIndex;
      if (isCovering() || visibleGroupIndex === null) return;
      if (drawFrame(videos[visibleGroupIndex], cover)) cover.style.visibility = "visible";
    };
    const hideCover = () => {
      cover.style.visibility = "hidden";
    };

    const player = createCinematicPlayer(videos, table.groups, {
      onView: (next) => {
        setView(next);
        if (next.phase === "presented" || next.phase === "presented_unverified") hideCover();
      },
      log: qaLog.push,
      onBeforeReload: (groupIndex) => {
        if (player.getSnapshot().view.visibleGroupIndex === groupIndex) showCover();
      },
    });
    const diagnostics = diagnosticsRef.current;
    playerRef.current = player;
    diagnostics.player = player;

    // L01은 메인 player·매핑과 분리된 독립 루프, 로딩 전·실패 시에는 아래 G4 3088이 그대로 보임
    const loop = createLoopPlayer(loopVideo, resolveLoopSrc(), {
      onVisibleChange: setLoopVisible,
      log: qaLog.push,
      restartAt: LOOP_RESTART_SECONDS,
      frameSeconds: LOOP_FRAME_SECONDS,
    });
    let loopPreloaded = false;
    let endingPhase: EndingPhase | null = null;

    // 로고 투명도·커튼 위치는 rAF마다 DOM 직접 갱신, L01 제어와 진단 기록은 phase가 바뀔 때만
    const applyEnding = (e: number) => {
      const ending = resolveEndingState(e);
      if (!ending) return;
      logo.style.opacity = String(ending.logoOpacity);
      curtain.style.translate = `0 ${(1 - ending.curtainProgress) * 100}%`;

      const previous = endingPhase;
      if (ending.phase === previous) return;
      endingPhase = ending.phase;
      qaLog.push({ type: "ending", phase: ending.phase, e });
      if (ending.phase === "G4_END") {
        loop.exit();
      } else if (ending.phase === "COVERED") {
        loop.pauseForHidden();
      } else {
        loop.enter();
        if (previous === "COVERED") loop.resume();
      }
    };

    let frame = 0;
    let frameRequestedAt = 0;
    let force = false;
    let lastMasterFrame: number | null = null;
    let lastProgress: number | null = null;
    let lastGeometry: string | null = null;
    let restored: { scrollY: number; progress: number } | null = null;

    // scroll 이벤트는 표시만 하고 계산·seek는 rAF당 1회
    const update = () => {
      frame = 0;
      const rect = section.getBoundingClientRect();
      const viewportHeight = window.innerHeight;
      const sectionTop = rect.top + window.scrollY;
      const mainHeight = (rect.height * MAIN_VH) / (MAIN_VH + ENDING_VH);
      const geometry = `${rect.height}:${viewportHeight}`;

      // 섹션 높이가 vh 기준이라 창 높이가 바뀌면 같은 scrollY의 진행률이 달라짐, 구간 안이면 직전 진행률 위치로 복원
      if (lastGeometry !== null && geometry !== lastGeometry && lastProgress !== null && lastProgress > 0 && lastProgress < 1) {
        window.scrollTo({ top: sectionTop + lastProgress * (mainHeight - viewportHeight), behavior: "instant" });
        restored = { scrollY: window.scrollY, progress: lastProgress };
        qaLog.push({ type: "lifecycle", detail: "scroll_restored" });
      }
      lastGeometry = geometry;

      // 복원 직후 픽셀 반올림으로 인접 프레임이 선택되지 않도록 사용자가 움직이기 전까지 복원 진행률 유지
      if (restored && window.scrollY !== restored.scrollY) restored = null;
      const progress =
        restored?.progress ??
        computeProgress({ scrollY: window.scrollY, sectionTop, sectionHeight: mainHeight, viewportHeight });
      const target = progress === null ? null : resolveFrame(table, progress);
      if (progress === null || !target) return;

      // 엔딩 진행률은 메인 구간 끝(프레임 3088 도달 위치)부터 섹션 끝까지, 메인 진행률은 그동안 1로 고정
      const endingProgress = computeProgress({
        scrollY: window.scrollY,
        sectionTop: sectionTop + mainHeight - viewportHeight,
        sectionHeight: rect.height - mainHeight + viewportHeight,
        viewportHeight,
      });
      if (progress > LOOP_PRELOAD_PROGRESS && !loopPreloaded) {
        loopPreloaded = true;
        loop.preload();
      }
      if (endingProgress !== null) applyEnding(endingProgress);

      lastProgress = progress;
      diagnostics.progress = progress;
      if (target.masterFrame === lastMasterFrame && !force) return;
      force = false;
      lastMasterFrame = target.masterFrame;
      qaLog.push({ type: "target", progress, masterFrame: target.masterFrame, group: table.groups[target.groupIndex].id });
      player.setTarget(target);
    };
    // Safari 탭 전환 후 예약된 rAF 콜백이 실행되지 않으면 이후 scroll이 모두 무시되므로 오래된 예약은 버리고 즉시 계산
    const schedule = () => {
      if (frame && performance.now() - frameRequestedAt < RAF_STALE_MS) return;
      if (frame) {
        cancelAnimationFrame(frame);
        qaLog.push({ type: "lifecycle", detail: "raf_stalled" });
        update();
        return;
      }
      frameRequestedAt = performance.now();
      frame = requestAnimationFrame(update);
    };
    // 리사이즈·탭 복귀·bfcache 복원 시 같은 프레임이어도 표시 상태 재확인
    const resync = () => {
      force = true;
      schedule();
    };
    // 탭 복귀·창 복귀·bfcache 복원 시 끊긴 seek와 rAF 예약 복구, Safari는 탭 전환에 visibilitychange를 보내지 않는 경우가 있어 focus도 사용
    const recover = (detail: "visible" | "pageshow" | "focus") => {
      qaLog.push({ type: "lifecycle", detail });
      cancelAnimationFrame(frame);
      frame = 0;
      player.resume();
      if (endingPhase !== "COVERED") loop.resume();
      resync();
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        recover("visible");
        return;
      }
      qaLog.push({ type: "lifecycle", detail: "hidden" });
      loop.pauseForHidden();
      // 숨김 직전 프레임이 아직 살아 있을 때 덮어 두고 복귀 후 표시 확인까지 유지
      showCover();
    };
    const handlePageShow = () => recover("pageshow");
    const handleFocus = () => recover("focus");

    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", resync);
    window.addEventListener("pageshow", handlePageShow);
    window.addEventListener("focus", handleFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    resync();

    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", resync);
      window.removeEventListener("pageshow", handlePageShow);
      window.removeEventListener("focus", handleFocus);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      player.dispose();
      loop.dispose();
      qaLog.dispose();
      playerRef.current = null;
      diagnostics.player = null;
      setView(INITIAL_VIEW);
      setLoopVisible(false);
      hideCover();
    };
  }, [active, debug, table, sectionRef]);

  const overlayState = view.phase === "error" ? "error" : view.visibleGroupIndex === null ? "loading" : "hidden";

  return (
    <div className="sticky top-0 h-svh overflow-hidden">
      {table.groups.map((group, index) => (
        <video
          key={group.id}
          ref={(video) => {
            videoRefs.current[index] = video;
          }}
          muted
          playsInline
          disablePictureInPicture
          preload="metadata"
          aria-hidden="true"
          tabIndex={-1}
          className={cn(
            "absolute inset-0 size-full object-cover",
            view.visibleGroupIndex === index ? "visible" : "invisible",
          )}
        />
      ))}
      <canvas
        ref={coverRef}
        aria-hidden="true"
        className="invisible absolute inset-0 size-full object-cover"
      />
      <video
        ref={loopRef}
        data-testid="ending-loop"
        muted
        playsInline
        disablePictureInPicture
        preload="none"
        aria-hidden="true"
        tabIndex={-1}
        className={cn("absolute inset-0 size-full object-cover", loopVisible ? "visible" : "invisible")}
      />
      {/* 화면 크기와 무관하게 로고 중심을 화면 정중앙에 고정 (2026-10-31 QA 조정) */}
      <div
        ref={logoRef}
        aria-hidden="true"
        data-testid="ending-mark"
        className="pointer-events-none absolute inset-0 flex items-center justify-center opacity-0"
      >
        {endingMark}
      </div>
      <div
        ref={curtainRef}
        aria-hidden="true"
        data-testid="ending-curtain"
        className="pointer-events-none absolute inset-0 translate-y-full bg-footer"
      />
      <CinematicOverlay state={overlayState} onRetry={() => playerRef.current?.retry()} />
      {debug && <DiagnosticsPanel table={table} sourceRef={diagnosticsRef} />}
    </div>
  );
}

/** video 현재 프레임을 canvas에 원본 해상도로 복사, 프레임이 없으면 false */
function drawFrame(video: HTMLVideoElement, canvas: HTMLCanvasElement): boolean {
  const context = canvas.getContext("2d");
  if (!context || !video.videoWidth || !video.videoHeight) return false;
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  context.drawImage(video, 0, 0, canvas.width, canvas.height);
  return true;
}
