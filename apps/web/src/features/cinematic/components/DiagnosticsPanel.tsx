"use client";

import { useEffect, useRef } from "react";

import { type CinematicTable, PTS_TICKS_PER_SECOND } from "../core/types";
import type { PlayerSnapshot } from "../playback/cinematicPlayer";

export type DiagnosticsSource = {
  progress: number | null;
  player: { getSnapshot: () => PlayerSnapshot } | null;
};

type DiagnosticsPanelProps = {
  table: CinematicTable;
  sourceRef: React.RefObject<DiagnosticsSource>;
};

/** `?cinematic-debug=1` 전용 패널, rAF마다 textContent 직접 갱신 */
export function DiagnosticsPanel({ table, sourceRef }: DiagnosticsPanelProps) {
  const outputRef = useRef<HTMLPreElement>(null);

  useEffect(() => {
    let frame = requestAnimationFrame(function render() {
      if (outputRef.current) outputRef.current.textContent = describe(table, sourceRef.current);
      frame = requestAnimationFrame(render);
    });
    return () => cancelAnimationFrame(frame);
  }, [table, sourceRef]);

  return (
    <div className="fixed right-3 bottom-3 z-50 flex max-w-sm flex-col gap-2 rounded bg-black/80 p-3">
      <pre ref={outputRef} aria-hidden="true" className="font-mono text-[11px] leading-4 text-lime-300" />
      <button
        type="button"
        onClick={downloadQaLog}
        className="self-end rounded bg-lime-300 px-2 py-1 text-xs font-medium text-black"
      >
        로그 저장
      </button>
    </div>
  );
}

// 콘솔 없이 QA 이벤트 로그를 파일로 받기 위한 다운로드
function downloadQaLog() {
  const json = window.__DOUID_CINEMATIC_QA__?.export() ?? "[]";
  const browser = /Chrome\//.test(navigator.userAgent) ? "chrome" : /Safari\//.test(navigator.userAgent) ? "safari" : "browser";
  const link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob([json], { type: "application/json" }));
  link.download = `douid-qa-${browser}-${new Date().toISOString().replace(/[:.]/g, "-")}.json`;
  link.click();
  // Safari는 click 직후 해제하면 다운로드가 취소될 수 있어 지연 해제
  setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}

function describe(table: CinematicTable, source: DiagnosticsSource): string {
  const snapshot = source.player?.getSnapshot();
  if (!snapshot) return "cinematic-debug: player 없음";

  const { target, view, seekers, targetMedia } = snapshot;
  const visible = view.visibleGroupIndex;
  const presented = visible === null ? null : seekers[visible];
  const presentedMaster =
    visible !== null && presented?.presentedFrame != null
      ? table.groups[visible].masterStart + presented.presentedFrame
      : null;
  const frameError =
    target && presentedMaster !== null && visible === target.groupIndex ? presentedMaster - target.masterFrame : null;
  const targetSeeker = target ? seekers[target.groupIndex] : null;

  return [
    `progress     ${source.progress?.toFixed(6) ?? "—"}`,
    target
      ? `target       master ${target.masterFrame} · ${table.groups[target.groupIndex].id} web ${target.webFrame} · ${(target.ptsTicks / PTS_TICKS_PER_SECOND).toFixed(4)}s`
      : "target       —",
    visible !== null && presented
      ? `presented    ${table.groups[visible].id} mediaTime ${presented.presentedMediaTime?.toFixed(4) ?? "—"} → web ${presented.presentedFrame ?? "—"} / master ${presentedMaster ?? "—"} · verified ${presented.status === "presented"}`
      : "presented    —",
    `frameError   ${frameError ?? "—"}`,
    `latencyMs    ${targetSeeker?.lastLatencyMs?.toFixed(1) ?? "—"}`,
    `phase        ${view.phase}${view.error ? ` · ${view.error.kind}(${view.error.code ?? "-"}) @ ${table.groups[view.error.groupIndex].id}` : ""}`,
    `ready/net    ${targetMedia ? `${targetMedia.readyState} / ${targetMedia.networkState}` : "—"}`,
  ].join("\n");
}
