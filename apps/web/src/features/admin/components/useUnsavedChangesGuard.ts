"use client";

import { createContext, createElement, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

const LEAVE_MESSAGE = "저장하지 않은 변경이 사라집니다. 이동할까요?";

type UnsavedChanges = { isDirty: boolean; setDirty: (dirty: boolean) => void };

const UnsavedChangesContext = createContext<UnsavedChanges>({ isDirty: false, setDirty: () => {} });

/** 관리자 레이아웃 전체의 미저장 상태 공유, 내비게이션 링크가 이탈 확인에 사용 */
export function UnsavedChangesProvider({ children }: { children: ReactNode }) {
  const [isDirty, setDirty] = useState(false);
  const value = useMemo(() => ({ isDirty, setDirty }), [isDirty]);
  return createElement(UnsavedChangesContext.Provider, { value }, children);
}

/**
 * 미저장 변경 이탈 경고
 * dirty일 때만 `beforeunload` 등록, 화면 내 이동은 반환한 `confirmLeave`로 확인
 * 브라우저 뒤로가기는 App Router에 차단 API가 없어 막지 않음
 */
export function useUnsavedChangesGuard(isDirty: boolean) {
  const { setDirty } = useContext(UnsavedChangesContext);

  useEffect(() => {
    setDirty(isDirty);
    return () => setDirty(false);
  }, [isDirty, setDirty]);

  useEffect(() => {
    if (!isDirty) return;
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [isDirty]);

  return useCallback(() => !isDirty || window.confirm(LEAVE_MESSAGE), [isDirty]);
}

/** 레이아웃 내비게이션용 이탈 확인, 등록된 화면이 dirty일 때만 묻기 */
export function useConfirmLeave() {
  const { isDirty } = useContext(UnsavedChangesContext);
  return useCallback(() => !isDirty || window.confirm(LEAVE_MESSAGE), [isDirty]);
}
