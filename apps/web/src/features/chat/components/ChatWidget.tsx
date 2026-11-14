"use client";

import { MessageCircle, X } from "lucide-react";
import { useEffect, useId, useRef } from "react";

import { Button } from "@/shared/components/ui/button";

import { hydrateChatOnce, useChatStore } from "../model/chatStore";
import type { PublicProject } from "../model/types";

import { ChatPanel } from "./ChatPanel";

/** 공개 페이지 AI 채팅 진입점, 닫힌 상태에서는 런처만 렌더 */
export function ChatWidget({ projects }: { projects: PublicProject[] }) {
  const isOpen = useChatStore((state) => state.isOpen);
  const open = useChatStore((state) => state.open);
  const close = useChatStore((state) => state.close);
  const launcherRef = useRef<HTMLButtonElement>(null);
  const panelId = useId();

  // 서버 HTML(빈 대화)과 첫 렌더를 맞춘 뒤 현재 탭 대화 복원
  useEffect(hydrateChatOnce, []);

  function handleClose() {
    close();
    launcherRef.current?.focus();
  }

  return (
    <>
      <Button
        ref={launcherRef}
        type="button"
        size="icon-lg"
        aria-label={isOpen ? "AI 상담 닫기" : "AI 상담 열기"}
        aria-expanded={isOpen}
        aria-controls={isOpen ? panelId : undefined}
        className="fixed right-4 bottom-4 z-30 rounded-full shadow-lg"
        onClick={isOpen ? handleClose : open}
      >
        {isOpen ? <X aria-hidden="true" /> : <MessageCircle aria-hidden="true" />}
      </Button>
      {isOpen ? <ChatPanel id={panelId} projects={projects} onClose={handleClose} /> : null}
    </>
  );
}
