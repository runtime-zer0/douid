"use client";

import Link from "next/link";
import { type KeyboardEvent, useEffect, useId, useRef, useState } from "react";

import { Button } from "@/shared/components/ui/button";

import { KAKAO_CHANNEL_URL } from "../content";
import { type ChatPhase, selectPhase, useChatStore } from "../model/chatStore";
import { describeFailure } from "../model/failure";
import type { ChatMessage, PublicProject } from "../model/types";

import { ChatComposer } from "./ChatComposer";
import { MessageList } from "./MessageList";

type Props = {
  id: string;
  projects: PublicProject[];
  onClose: () => void;
};

function announcement(phase: ChatPhase, last: ChatMessage | undefined): string {
  switch (phase) {
    case "connecting":
    case "streaming":
      return "답변을 준비하고 있어요";
    case "completed":
      return "답변이 완료됐어요";
    case "cancelled":
      return "생성을 중단했어요";
    case "error":
      return last?.role === "assistant" && last.error ? describeFailure(last.error).message : "답변을 처리하지 못했어요";
    default:
      return "";
  }
}

export function ChatPanel({ id, projects, onClose }: Props) {
  const messages = useChatStore((state) => state.messages);
  const phase = useChatStore(selectPhase);
  const retry = useChatStore((state) => state.retry);
  const reset = useChatStore((state) => state.reset);
  const titleId = useId();
  const inputRef = useRef<HTMLTextAreaElement>(null);
  // 단계가 바뀔 때만 알림 문구 갱신, 델타마다 낭독하지 않음(FR-037)
  const [status, setStatus] = useState({ phase, text: "" });
  if (status.phase !== phase) setStatus({ phase, text: announcement(phase, messages.at(-1)) });

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  function handleKeyDown(event: KeyboardEvent<HTMLElement>) {
    if (event.key !== "Escape") return;
    event.stopPropagation();
    onClose();
  }

  // 모바일은 헤더까지 덮는 전체 화면, 데스크톱은 헤더 아래 높이로 제한해 내비게이션을 가리지 않음
  return (
    <section
      id={id}
      role="dialog"
      aria-labelledby={titleId}
      onKeyDown={handleKeyDown}
      className="fixed inset-0 z-50 flex h-dvh flex-col bg-background text-foreground sm:inset-auto sm:right-4 sm:bottom-20 sm:z-30 sm:h-[min(600px,calc(100dvh-11rem))] sm:w-[min(400px,calc(100vw-2rem))] sm:border sm:border-border sm:shadow-lg motion-safe:sm:animate-in motion-safe:sm:fade-in"
    >
      <header className="flex items-center justify-between gap-2 border-b border-border px-4 py-3">
        <h2 id={titleId} className="font-heading text-base">
          DOUID AI 상담
        </h2>
        <div className="flex gap-1">
          {messages.length > 0 ? (
            <Button
              type="button"
              size="xs"
              variant="ghost"
              onClick={() => {
                reset();
                inputRef.current?.focus();
              }}
            >
              새 대화
            </Button>
          ) : null}
          <Button type="button" size="xs" variant="ghost" onClick={onClose}>
            닫기
          </Button>
        </div>
      </header>
      <p role="status" className="sr-only">
        {status.text}
      </p>
      <MessageList messages={messages} projects={projects} onRetry={retry} />
      <ChatComposer inputRef={inputRef} />
      <ConsultLink />
    </section>
  );
}

function ConsultLink() {
  if (!KAKAO_CHANNEL_URL?.startsWith("https:")) return null;
  return (
    <p className="flex flex-wrap items-center gap-x-2 gap-y-1 border-t border-border px-4 py-3 text-xs text-muted-foreground">
      <a href={KAKAO_CHANNEL_URL} target="_blank" rel="noopener noreferrer" className="font-semibold text-foreground underline underline-offset-4">
        카카오톡으로 상담하기
      </a>
      <span>
        열리지 않으면{" "}
        <Link href="/contact" className="underline underline-offset-4">
          Contact 페이지
        </Link>
        를 이용해 주세요
      </span>
    </p>
  );
}
