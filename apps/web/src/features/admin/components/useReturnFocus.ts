import { useRef } from "react";

/**
 * 제어형 Radix 대화상자의 포커스 복귀
 * Trigger 없이 열면 Radix가 닫을 때 포커스를 돌려줄 대상이 없어, 열기 직전 포커스를 기억해 복귀
 */
export function useReturnFocus() {
  const origin = useRef<HTMLElement | null>(null);
  return {
    onOpenAutoFocus: () => {
      origin.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    },
    onCloseAutoFocus: (event: Event) => {
      // 삭제 성공 등으로 트리거가 사라졌으면 기본 동작에 맡김
      if (origin.current?.isConnected) {
        event.preventDefault();
        origin.current.focus();
      }
    },
  };
}
