"use client";

import Link from "next/link";
import { useLayoutEffect, useRef, useState } from "react";

import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/lib/utils";

import { describeFailure } from "../model/failure";
import { matchRecommendations } from "../model/recommendations";
import type { AssistantMessage, ChatMessage, PublicProject } from "../model/types";

// 이 거리 안이면 하단을 보고 있는 것으로 간주
const BOTTOM_THRESHOLD_PX = 48;

type Props = {
  messages: ChatMessage[];
  projects: PublicProject[];
  onRetry: () => void;
};

export function MessageList({ messages, projects, onRetry }: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const atBottom = useRef(true);
  const [hasUnseen, setHasUnseen] = useState(false);
  const lastText = messages.at(-1)?.text;

  // 하단을 보고 있을 때만 따라가고, 과거 메시지를 읽는 중이면 위치 유지(FR-038)
  useLayoutEffect(() => {
    const element = scrollRef.current;
    if (!element) return;
    if (atBottom.current) element.scrollTop = element.scrollHeight;
    else setHasUnseen(true);
  }, [messages.length, lastText]);

  function scrollToBottom() {
    const element = scrollRef.current;
    if (!element) return;
    element.scrollTop = element.scrollHeight;
    atBottom.current = true;
    setHasUnseen(false);
  }

  const lastIndex = messages.length - 1;

  return (
    <div className="relative min-h-0 flex-1">
      <div
        ref={scrollRef}
        className="h-full overflow-y-auto overscroll-contain px-4 py-4"
        onScroll={(event) => {
          const element = event.currentTarget;
          atBottom.current = element.scrollHeight - element.scrollTop - element.clientHeight < BOTTOM_THRESHOLD_PX;
          if (atBottom.current) setHasUnseen(false);
        }}
      >
        {messages.length === 0 ? (
          <p className="text-sm text-muted-foreground">DOUID 작업과 브랜드에 대해 궁금한 점을 물어보세요.</p>
        ) : (
          <ol className="flex flex-col gap-3">
            {messages.map((message, index) =>
              message.role === "user" ? (
                <li key={message.id} className="ml-8 self-end bg-primary px-3 py-2 text-sm text-primary-foreground">
                  <p className="break-words whitespace-pre-wrap">{message.text}</p>
                </li>
              ) : (
                <AssistantBubble
                  key={message.id}
                  message={message}
                  projects={projects}
                  onRetry={index === lastIndex ? onRetry : undefined}
                />
              ),
            )}
          </ol>
        )}
      </div>
      {hasUnseen ? (
        <Button type="button" size="xs" variant="secondary" className="absolute bottom-3 left-1/2 -translate-x-1/2 shadow" onClick={scrollToBottom}>
          새 답변 보기
        </Button>
      ) : null}
    </div>
  );
}

function AssistantBubble({
  message,
  projects,
  onRetry,
}: {
  message: AssistantMessage;
  projects: PublicProject[];
  onRetry?: () => void;
}) {
  const failure = message.status === "error" && message.error ? describeFailure(message.error) : null;
  const showCards = message.status === "done" || message.status === "streaming";
  const cards = showCards && message.recommendations ? matchRecommendations(message.recommendations, projects) : [];

  return (
    <li className="mr-8 flex flex-col gap-2 self-start border border-border px-3 py-2 text-sm">
      {message.status === "pending" ? (
        <span className="flex gap-1 py-1" aria-hidden="true">
          {[0, 1, 2].map((dot) => (
            <span key={dot} className="size-1.5 rounded-full bg-muted-foreground motion-safe:animate-pulse" />
          ))}
        </span>
      ) : null}
      {message.text ? (
        <p className="break-words whitespace-pre-wrap">{message.text}</p>
      ) : message.status === "done" ? (
        <p className="text-muted-foreground">답변 내용이 없습니다</p>
      ) : null}
      {message.status === "cancelled" ? <StatusTag>생성을 중단했어요</StatusTag> : null}
      {failure ? (
        <>
          <StatusTag tone="error">완료되지 않은 답변</StatusTag>
          <p className="text-destructive">{failure.message}</p>
        </>
      ) : null}
      {onRetry && failure?.retryable ? (
        <Button type="button" size="xs" variant="outline" className="self-start" onClick={onRetry}>
          다시 시도
        </Button>
      ) : null}
      {cards.length > 0 ? (
        <ul aria-label="추천 작업물" className="flex flex-col gap-1 border-t border-border pt-2">
          {cards.map((card) => (
            <li key={card.id}>
              <Link href={card.href} className="underline underline-offset-4 hover:text-muted-foreground">
                {card.title}
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </li>
  );
}

function StatusTag({ tone, children }: { tone?: "error"; children: string }) {
  return (
    <span className={cn("text-[0.625rem] font-semibold tracking-widest uppercase", tone === "error" ? "text-destructive" : "text-muted-foreground")}>
      {children}
    </span>
  );
}
