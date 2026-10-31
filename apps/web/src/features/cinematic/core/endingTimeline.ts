export type EndingPhase = "G4_END" | "S43_SETTLE" | "LOGO_REVEAL" | "LOGO_HOLD" | "RESISTANCE" | "CURTAIN" | "COVERED";

export type EndingState = {
  phase: EndingPhase;
  /** S43 로고 투명도 0~1 */
  logoOpacity: number;
  /** S50 커튼이 화면을 덮은 비율 0~1 */
  curtainProgress: number;
};

/** 엔딩 진행률 e 기준 구간 시작점, 초기값이며 브라우저 QA에서 조정 (research R6) */
export const ENDING_BOUNDS = { settle: 0.2, reveal: 0.4, hold: 0.65, resistance: 0.75 } as const;

/** 엔딩 진행률 e(0~1)를 표시 상태로 변환, 이력 없이 e만으로 결정, 비유한 입력이면 null */
export function resolveEndingState(e: number): EndingState | null {
  if (!Number.isFinite(e)) return null;
  const p = Math.min(1, Math.max(0, e));
  const { settle, reveal, hold, resistance } = ENDING_BOUNDS;

  if (p === 0) return { phase: "G4_END", logoOpacity: 0, curtainProgress: 0 };
  if (p < settle) return { phase: "S43_SETTLE", logoOpacity: 0, curtainProgress: 0 };
  if (p < reveal) return { phase: "LOGO_REVEAL", logoOpacity: (p - settle) / (reveal - settle), curtainProgress: 0 };
  if (p < hold) return { phase: "LOGO_HOLD", logoOpacity: 1, curtainProgress: 0 };
  if (p < resistance) return { phase: "RESISTANCE", logoOpacity: 1, curtainProgress: 0 };
  if (p < 1) return { phase: "CURTAIN", logoOpacity: 1, curtainProgress: (p - resistance) / (1 - resistance) };
  return { phase: "COVERED", logoOpacity: 1, curtainProgress: 1 };
}
