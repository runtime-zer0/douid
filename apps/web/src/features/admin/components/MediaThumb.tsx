"use client";

import { ImageOff } from "lucide-react";
import Image from "next/image";
import { useState } from "react";

import { cn } from "@/shared/lib/utils";

type MediaThumbProps = {
  src: string | null;
  alt: string;
  /** 크기는 className(예: `size-12`)으로 지정, 이 값은 이미지 고유 크기 힌트 */
  size: number;
  className: string;
  fit?: "cover" | "contain";
};

/**
 * 서버 상대 URL 미리보기, 최적화 서버를 거치지 않도록 unoptimized
 * 로드 실패는 자산 부재로 단정하지 않고 대체 표시만 함
 */
export function MediaThumb({ src, alt, size, className, fit = "cover" }: MediaThumbProps) {
  const [failed, setFailed] = useState(false);
  const box = cn("flex shrink-0 items-center justify-center overflow-hidden bg-muted", className);

  if (!src || failed) {
    // 작은 칸에서는 글자가 잘리므로 아이콘만 보이고 문구는 보조기기와 큰 칸에서 제공
    return (
      <span className={cn(box, "@container flex-col gap-1 p-1 text-center text-[0.6875rem] leading-tight text-muted-foreground")}>
        <ImageOff aria-hidden="true" className="size-4 shrink-0" />
        <span className="sr-only @[5rem]:not-sr-only">{src ? "미리보기 실패" : "이미지 없음"}</span>
      </span>
    );
  }
  return (
    <span className={box}>
      <Image
        src={src}
        alt={alt}
        width={size}
        height={size}
        unoptimized
        className={cn("size-full", fit === "cover" ? "object-cover" : "object-contain")}
        onError={() => setFailed(true)}
      />
    </span>
  );
}
