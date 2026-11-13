"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";

import { cn } from "@/shared/lib/utils";

import { useAdminMe, useLogout } from "../api/auth";
import { describeError } from "../model/errorMessage";
import { Notice } from "./Notice";
import { useConfirmLeave } from "./useUnsavedChangesGuard";
import { useSubmitLock } from "./useSubmitLock";

const NAV = [
  { href: "/admin/works", label: "작업물" },
  { href: "/admin/categories", label: "카테고리" },
  { href: "/admin/media", label: "미디어" },
] as const;

/** 관리자 공통 레이아웃, 내비게이션·관리자 이메일·로그아웃 */
export function AdminShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const admin = useAdminMe().data;
  const logout = useLogout();
  const lock = useSubmitLock();
  const confirmLeave = useConfirmLeave();
  const [error, setError] = useState<string | null>(null);

  const onLogout = () => {
    if (!confirmLeave()) return;
    void lock(async () => {
      setError(null);
      try {
        await logout.mutateAsync();
        router.replace("/admin/login");
      } catch (caught) {
        setError(`로그아웃하지 못했습니다. ${describeError(caught).message}`);
      }
    });
  };

  return (
    <div className="flex min-h-dvh flex-col">
      <a
        href="#admin-main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:bg-background focus:px-4 focus:py-2 focus:text-sm"
      >
        본문으로 건너뛰기
      </a>
      <header className="bg-footer text-footer-foreground">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-8 gap-y-3 px-4 py-4 sm:px-8">
          <Link href="/admin/works" translate="no" className="font-heading text-xl tracking-wide" onClick={(event) => !confirmLeave() && event.preventDefault()}>
            DOUID <span className="text-sm text-footer-foreground/60">관리</span>
          </Link>
          <nav aria-label="관리 메뉴" className="order-3 w-full sm:order-none sm:w-auto">
            <ul className="flex gap-6 text-sm">
              {NAV.map((item) => {
                const current = pathname === item.href || pathname.startsWith(`${item.href}/`);
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      aria-current={current ? "page" : undefined}
                      onClick={(event) => !confirmLeave() && event.preventDefault()}
                      className={cn(
                        "inline-flex min-h-10 items-center border-b-2 border-transparent text-footer-foreground/70 transition-colors hover:text-footer-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-footer-foreground",
                        current && "border-footer-foreground text-footer-foreground",
                      )}
                    >
                      {item.label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>
          <div className="ml-auto flex min-w-0 items-center gap-4 text-sm">
            {admin ? <span className="hidden truncate text-footer-foreground/70 sm:inline">{admin.email}</span> : null}
            <button
              type="button"
              onClick={onLogout}
              disabled={logout.isPending}
              className="min-h-10 shrink-0 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-footer-foreground disabled:opacity-50"
            >
              {logout.isPending ? "로그아웃 중…" : "로그아웃"}
            </button>
          </div>
        </div>
      </header>
      {error ? (
        <div className="mx-auto w-full max-w-6xl px-4 pt-4 sm:px-8">
          <Notice tone="alert">{error}</Notice>
        </div>
      ) : null}
      <main id="admin-main" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-8 sm:py-12">
        {children}
      </main>
    </div>
  );
}
