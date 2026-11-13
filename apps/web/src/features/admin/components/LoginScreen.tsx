"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { useAdminMe } from "../api/auth";
import { adminKeys } from "../api/queryKeys";
import { safeNext } from "../model/safeNext";
import { LoginForm } from "./LoginForm";

/**
 * 로그인 화면, 이 화면에서 서버로 다시 확인한 세션만 신뢰
 * 로그아웃 직후 남은 `me` 캐시는 서버 확인이 실패하면 비워 관리자 정보를 남기지 않음
 */
export function LoginScreen({ next }: { next?: string }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [mountedAt] = useState(() => Date.now());
  const me = useAdminMe();
  const target = safeNext(next);
  const confirmedNow = me.data !== undefined && me.dataUpdatedAt >= mountedAt;
  const staleAfterFailure = me.isError && me.data !== undefined && me.dataUpdatedAt < mountedAt;

  useEffect(() => {
    if (confirmedNow) router.replace(target);
  }, [confirmedNow, router, target]);

  useEffect(() => {
    if (staleAfterFailure) void queryClient.resetQueries({ queryKey: adminKeys.me, exact: true });
  }, [staleAfterFailure, queryClient]);

  return (
    <main className="flex min-h-dvh items-center justify-center px-4 py-16">
      <div className="w-full max-w-sm">
        <p className="font-heading text-2xl tracking-wide">DOUID</p>
        <h1 className="mt-8 font-heading text-3xl">관리자 로그인</h1>
        <p className="mt-2 text-sm text-muted-foreground">작업물, 카테고리, 이미지를 관리하려면 관리자 계정으로 로그인하세요.</p>
        <div className="mt-10">
          <LoginForm onSuccess={() => undefined} />
        </div>
      </div>
    </main>
  );
}
