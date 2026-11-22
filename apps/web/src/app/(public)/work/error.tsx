"use client";

import Link from "next/link";

const LINK = "rounded-sm underline underline-offset-4 outline-offset-4 focus-visible:outline-2 focus-visible:outline-current";

// 서버 공개 API 장애 안내, 404(없는 작업물)와 구분
export default function WorkError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div className="flex flex-col items-start gap-4 py-16">
      <h1 className="font-heading text-3xl tracking-tight md:text-4xl">작업물을 잠시 불러올 수 없습니다</h1>
      <p className="text-muted-foreground">잠시 후 다시 시도해 주세요.</p>
      <div className="flex gap-6">
        <button type="button" onClick={reset} className={LINK}>
          다시 시도
        </button>
        <Link href="/work" className={LINK}>
          WORK 목록으로
        </Link>
      </div>
    </div>
  );
}
