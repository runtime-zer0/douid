import { useCallback, useRef } from "react";

/**
 * 진행 중인 사용자 동작 하나에 요청 하나만 허용하는 잠금
 * `disabled` 반영 전 연타도 막기 위해 렌더와 무관한 ref로 판정
 */
export function useSubmitLock() {
  const locked = useRef(false);
  return useCallback(async (task: () => Promise<void>) => {
    if (locked.current) return;
    locked.current = true;
    try {
      await task();
    } finally {
      locked.current = false;
    }
  }, []);
}
