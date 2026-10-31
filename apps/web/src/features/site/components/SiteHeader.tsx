"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSyncExternalStore } from "react";

import { cn } from "@/shared/lib/utils";

import { BrandMark } from "./BrandMark";
import { NAV_ITEMS } from "./navigation";
import { FOCUS_RING } from "./styles";

// scroll마다 읽되 React 통지는 rAF당 1회, 값이 바뀔 때만 다시 렌더링
function subscribeScroll(onChange: () => void) {
  let frame = 0;
  const schedule = () => {
    if (!frame) frame = requestAnimationFrame(() => {
      frame = 0;
      onChange();
    });
  };
  window.addEventListener("scroll", schedule, { passive: true });
  return () => {
    window.removeEventListener("scroll", schedule);
    cancelAnimationFrame(frame);
  };
}
const subscribeNothing = () => () => {};
const getScrolled = () => window.scrollY > 0;
const getServerScrolled = () => false;

type SiteHeaderProps = {
  /** 홈 인트로, 맨 위에서는 로고를 화면 중앙에 크게 표시 */
  intro?: boolean;
  /** 배경 기준 색, dark는 영상 위 밝은 전경색 */
  tone?: "dark" | "light";
};

export function SiteHeader({ intro = false, tone = "dark" }: SiteHeaderProps) {
  const pathname = usePathname();
  const scrolled = useSyncExternalStore(intro ? subscribeScroll : subscribeNothing, getScrolled, getServerScrolled);
  const centered = intro && !scrolled;

  return (
    <header
      data-intro={intro ? (centered ? "center" : "compact") : undefined}
      className={cn(
        "fixed inset-x-0 top-0 z-40 flex items-center justify-between gap-4 px-4 py-5 sm:px-6",
        tone === "dark" ? "text-white" : "bg-background/90 text-foreground backdrop-blur-sm",
      )}
    >
      {/* 로고 요소는 하나, 인트로 중앙 ↔ 헤더 좌상단을 transform으로만 전환 */}
      <Link
        href="/"
        aria-label="DOUID 홈"
        className={cn(
          "block h-14 w-28 shrink-0 origin-center motion-safe:transition-transform motion-safe:duration-700 motion-safe:ease-out",
          FOCUS_RING,
          centered && "translate-x-[calc(50vw-4.5rem)] translate-y-[calc(50svh-3rem)] scale-[2.5] sm:translate-x-[calc(50vw-5rem)] md:scale-[3.5]",
        )}
      >
        <BrandMark tone={tone} decorative className="size-full object-contain" />
      </Link>
      <nav aria-label="주요 메뉴">
        <ul className="flex gap-4 text-sm tracking-[0.2em] sm:gap-8">
          {NAV_ITEMS.map((item) => {
            const current = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  aria-current={current ? "page" : undefined}
                  className={cn(FOCUS_RING, "opacity-80 hover:opacity-100 aria-[current=page]:opacity-100")}
                >
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </header>
  );
}
