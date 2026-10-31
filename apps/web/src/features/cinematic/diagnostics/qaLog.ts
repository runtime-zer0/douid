import type { EndingPhase } from "../core/endingTimeline";
import type { GroupId } from "../core/types";

export type QaEvent =
  | { t: number; type: "target"; progress: number; masterFrame: number; group: GroupId }
  | { t: number; type: "seek"; requestId: number; group: GroupId; webFrame: number; seconds: number }
  | {
      t: number;
      type: "presented";
      requestId: number;
      group: GroupId;
      webFrame: number;
      mediaTime: number | null;
      verified: boolean;
      latencyMs: number;
    }
  | { t: number; type: "superseded"; requestId: number; group: GroupId }
  | { t: number; type: "visible_group"; from: GroupId | null; to: GroupId }
  | { t: number; type: "waiting" | "stalled" | "error"; group: GroupId; detail?: string }
  | { t: number; type: "lifecycle"; detail: "hidden" | "visible" | "pageshow" | "focus" | "raf_stalled" | "scroll_restored" }
  | { t: number; type: "ending"; phase: EndingPhase; e: number }
  | { t: number; type: "loop"; detail: "presented" | "error" | "stalled" | "waiting" | "buffered" | "fallback" }
  | { t: number; type: "loop"; detail: "wrap"; droppedFrames: number | null; totalFrames: number | null };

export type QaEventInput = QaEvent extends infer E ? (E extends QaEvent ? Omit<E, "t"> : never) : never;

export type QaLog = {
  push: (event: QaEventInput) => void;
  export: () => string;
  dispose: () => void;
};

type QaGlobal = { export: () => string; events: () => QaEvent[] };

declare global {
  interface Window {
    __DOUID_CINEMATIC_QA__?: QaGlobal;
  }
}

const CAPACITY = 2000;
const NOOP_LOG: QaLog = { push: () => {}, export: () => "[]", dispose: () => {} };

/** 진단 이벤트 링버퍼 생성, 활성 시에만 window.__DOUID_CINEMATIC_QA__ 노출 */
export function createQaLog(enabled: boolean): QaLog {
  if (!enabled) return NOOP_LOG;

  const events: QaEvent[] = [];
  const exportEvents = () => JSON.stringify(events);
  const qaGlobal: QaGlobal = { export: exportEvents, events: () => [...events] };
  window.__DOUID_CINEMATIC_QA__ = qaGlobal;

  return {
    push: (event) => {
      events.push({ ...event, t: performance.now() } as QaEvent);
      if (events.length > CAPACITY) events.splice(0, events.length - CAPACITY);
    },
    export: exportEvents,
    dispose: () => {
      if (window.__DOUID_CINEMATIC_QA__ === qaGlobal) delete window.__DOUID_CINEMATIC_QA__;
    },
  };
}
