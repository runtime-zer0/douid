"use client";

import { useEffect, useRef, useState } from "react";

import type { CinematicTable } from "../core/types";
import { createGroupSeeker, type GroupSeeker, isPresented, type SeekerStatus } from "../playback/groupSeeker";
import { cn } from "@/shared/lib/utils";

import { CinematicOverlay } from "./CinematicOverlay";

/** 움직임 감소·coarse pointer용 정적 화면, 마지막 승인 프레임을 정지 상태로 표시 */
export function StaticCinematic({ table }: { table: CinematicTable }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const seekerRef = useRef<GroupSeeker | null>(null);
  const [status, setStatus] = useState<SeekerStatus>("idle");

  useEffect(() => {
    const last = table.progress.length - 1;
    const group = table.groups[table.groupIndex[last]];
    const seeker = createGroupSeeker(videoRef.current!, group.src, {
      groupId: group.id,
      onChange: (snapshot) => setStatus(snapshot.status),
    });
    seekerRef.current = seeker;
    seeker.setTarget(table.webFrame[last], table.ptsTicks[last]);
    return () => seeker.dispose();
  }, [table]);

  return (
    <div className="relative h-svh overflow-hidden bg-black">
      <video
        ref={videoRef}
        muted
        playsInline
        disablePictureInPicture
        preload="metadata"
        aria-hidden="true"
        tabIndex={-1}
        className={cn("absolute inset-0 size-full object-cover", !isPresented(status) && "invisible")}
      />
      <CinematicOverlay
        state={status === "error" ? "error" : isPresented(status) ? "hidden" : "loading"}
        onRetry={() => seekerRef.current?.retry()}
      />
    </div>
  );
}
