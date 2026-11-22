"use client";

import Image from "next/image";
import { useState } from "react";

import type { WorkImage } from "../model/types";

/** 작업물 이미지, 크기 정보가 없으면 최적화 없이 원본 비율, 로드 실패 시 같은 비율(없으면 4:3) 박스에 대체 텍스트 표시 */
export function ProjectImage({ image }: { image: WorkImage }) {
  const [failed, setFailed] = useState(false);
  const sized = image.width !== null && image.height !== null;

  if (failed) {
    return (
      <div
        role="img"
        aria-label={image.alt}
        // 비율은 자료마다 다른 런타임 값
        style={{ aspectRatio: sized ? `${image.width} / ${image.height}` : "4 / 3" }}
        className="flex w-full items-center justify-center bg-muted p-6 text-center text-sm text-muted-foreground"
      >
        {image.alt}
      </div>
    );
  }

  return (
    <Image
      src={image.src}
      // 서버 미디어는 크기 정보가 없고 `/api/media/*` rewrite를 최적화 경로로 다시 거치지 않도록 원본 사용
      width={image.width ?? 0}
      height={image.height ?? 0}
      unoptimized={!sized}
      alt={image.alt}
      sizes="(min-width: 640px) 50vw, 100vw"
      onError={() => setFailed(true)}
      className="h-auto w-full bg-muted"
    />
  );
}
