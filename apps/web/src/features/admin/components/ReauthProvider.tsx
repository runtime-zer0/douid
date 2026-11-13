"use client";

import { useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

import { adminKeys } from "../api/queryKeys";
import { describeError } from "../model/errorMessage";
import { Notice } from "./Notice";
import { ReauthDialog } from "./ReauthDialog";

type Reauth = { openReauth: () => void };

const ReauthContext = createContext<Reauth>({ openReauth: () => {} });

/**
 * 세션 만료 재인증 다이얼로그 제공
 * 화면을 언마운트하지 않고 위에 띄워 입력을 메모리에 유지, 게이트도 쓰므로 게이트 바깥에 위치
 */
export function ReauthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const openReauth = useCallback(() => setOpen(true), []);
  const value = useMemo(() => ({ openReauth }), [openReauth]);

  return (
    <ReauthContext.Provider value={value}>
      {children}
      <ReauthDialog
        open={open}
        onOpenChange={setOpen}
        onSuccess={() => {
          setOpen(false);
          setNotice("다시 로그인했습니다. 작성 중인 내용은 그대로이니 저장을 다시 눌러 주세요.");
          // 실패했던 조회만 다시 받음, 편집 폼은 초기화 이후 쿼리 변경을 무시
          void queryClient.invalidateQueries({ queryKey: adminKeys.all, predicate: (query) => query.state.status === "error" });
        }}
      />
      {notice ? (
        <div className="fixed inset-x-4 bottom-4 z-40 mx-auto max-w-md">
          <Notice tone="status" className="flex items-start justify-between gap-4 shadow-md">
            <span>{notice}</span>
            <button type="button" className="shrink-0 text-xs underline underline-offset-4" onClick={() => setNotice(null)}>
              닫기
            </button>
          </Notice>
        </div>
      ) : null}
    </ReauthContext.Provider>
  );
}

/** 재인증 다이얼로그 열기 */
export function useReauth() {
  return useContext(ReauthContext);
}

/** 조회 오류가 세션 만료(401)면 재인증 다이얼로그 열기 */
export function useReauthOnError(error: unknown) {
  const { openReauth } = useReauth();
  useEffect(() => {
    if (error && describeError(error).scope === "auth") openReauth();
  }, [error, openReauth]);
}
