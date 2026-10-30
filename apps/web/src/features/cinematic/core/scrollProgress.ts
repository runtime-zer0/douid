type ScrollMetrics = {
  scrollY: number;
  sectionTop: number;
  sectionHeight: number;
  viewportHeight: number;
};

/** 시네마틱 섹션 안 스크롤 위치를 0~1 진행률로 변환, 비유한 결과면 null */
export function computeProgress({ scrollY, sectionTop, sectionHeight, viewportHeight }: ScrollMetrics): number | null {
  const values = [scrollY, sectionTop, sectionHeight, viewportHeight];
  if (!values.every(Number.isFinite)) return null;

  const scrollable = sectionHeight - viewportHeight;
  if (scrollable <= 0) return 0;

  return Math.min(1, Math.max(0, (scrollY - sectionTop) / scrollable));
}
