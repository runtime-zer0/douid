import Link from "next/link";

import { SiteFooter, SiteHeader } from "@/features/site";

export default function NotFound() {
  return (
    <>
      <SiteHeader tone="light" />
      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-8 px-4 pt-32 pb-24 sm:px-6">
        <h1 className="font-heading text-4xl tracking-tight md:text-5xl">페이지를 찾을 수 없습니다</h1>
        <p className="text-muted-foreground">주소가 바뀌었거나 아직 공개되지 않은 페이지입니다.</p>
        <div className="flex gap-6">
          <Link href="/work" className="underline underline-offset-4">
            WORK 보기
          </Link>
          <Link href="/" className="underline underline-offset-4">
            홈으로
          </Link>
        </div>
      </main>
      <SiteFooter />
    </>
  );
}
