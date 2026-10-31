"use client";

import Image from "next/image";
import { useState } from "react";

import type { ProjectMedia } from "../content/projects";

/** 프로젝트 이미지, 로드 실패 시 같은 비율 박스에 대체 텍스트 표시 */
export function ProjectImage({ media }: { media: ProjectMedia }) {
  const [failed, setFailed] = useState(false);

  if (failed) {
    return (
      <div
        role="img"
        aria-label={media.alt}
        // 비율은 자료마다 다른 런타임 값
        style={{ aspectRatio: `${media.width} / ${media.height}` }}
        className="flex w-full items-center justify-center bg-muted p-6 text-center text-sm text-muted-foreground"
      >
        {media.alt}
      </div>
    );
  }

  return (
    <Image
      src={media.src}
      width={media.width}
      height={media.height}
      alt={media.alt}
      sizes="(min-width: 640px) 50vw, 100vw"
      onError={() => setFailed(true)}
      className="h-auto w-full bg-muted"
    />
  );
}
