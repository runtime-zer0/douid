"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { Button } from "@/shared/components/ui/button";

import { useAdminMe } from "../api/auth";
import { describeError } from "../model/errorMessage";
import { useReauth } from "./ReauthProvider";

/**
 * 서버 세션으로 관리자 확인 전에는 보호 콘텐츠를 렌더링하지 않는 표시 경계
 * 로그인 이동은 아직 인증된 적 없는 첫 확인의 401에만, 인증 이후 401은 화면을 유지한 채 재인증
 */
export function AdminGate({ children }: { children: ReactNode }) {
  const router = useRouter();
  const { openReauth } = useReauth();
  const { data, error, errorUpdatedAt, isPending, refetch, isFetching } = useAdminMe();
  const scope = error ? describeError(error).scope : null;

  useEffect(() => {
    if (scope !== "auth") return;
    if (data) {
      openReauth();
      return;
    }
    const next = `${window.location.pathname}${window.location.search}`;
    router.replace(`/admin/login?next=${encodeURIComponent(next)}`);
  }, [scope, data, errorUpdatedAt, openReauth, router]);

  if (data) return children;

  if (isPending || scope === "auth") {
    return (
      <p role="status" className="px-4 py-16 text-center text-sm text-muted-foreground">
        관리자 확인 중…
      </p>
    );
  }

  return (
    <div role="alert" className="mx-auto grid max-w-md gap-4 px-4 py-16 text-center">
      <p className="text-sm">관리자 상태를 확인하지 못했습니다. 네트워크나 서버 상태를 확인한 뒤 다시 시도해 주세요.</p>
      <Button variant="outline" className="mx-auto" disabled={isFetching} onClick={() => void refetch()}>
        {isFetching ? "확인 중…" : "다시 시도"}
      </Button>
    </div>
  );
}
