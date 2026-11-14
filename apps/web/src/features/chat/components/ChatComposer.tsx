"use client";

import { type KeyboardEvent, type Ref, useId, useState } from "react";

import { Button } from "@/shared/components/ui/button";
import { Label } from "@/shared/components/ui/label";
import { Textarea } from "@/shared/components/ui/textarea";

import { MAX_MESSAGE_CODE_POINTS } from "../model/limits";
import { validateQuestion } from "../model/history";
import { selectPhase, useChatStore } from "../model/chatStore";

export function ChatComposer({ inputRef }: { inputRef: Ref<HTMLTextAreaElement> }) {
  const draft = useChatStore((state) => state.draft);
  const setDraft = useChatStore((state) => state.setDraft);
  const send = useChatStore((state) => state.send);
  const cancel = useChatStore((state) => state.cancel);
  const busy = useChatStore((state) => {
    const phase = selectPhase(state);
    return phase === "connecting" || phase === "streaming";
  });
  const [triedEmpty, setTriedEmpty] = useState(false);
  const inputId = useId();
  const hintId = useId();

  const length = [...draft].length;
  const tooLong = validateQuestion(draft) === "too_long";
  const hint = tooLong ? `${MAX_MESSAGE_CODE_POINTS}자 이하로 입력해 주세요` : triedEmpty ? "질문을 입력해 주세요" : null;

  function submit() {
    if (busy) return;
    const problem = validateQuestion(draft);
    setTriedEmpty(problem === "empty");
    if (!problem) send(draft);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    submit();
  }

  return (
    <form
      className="flex flex-col gap-2 border-t border-border p-4"
      onSubmit={(event) => {
        event.preventDefault();
        submit();
      }}
    >
      <Label htmlFor={inputId} className="sr-only">
        질문 입력
      </Label>
      <Textarea
        id={inputId}
        ref={inputRef}
        value={draft}
        rows={2}
        placeholder="궁금한 점을 입력해 주세요"
        className="max-h-40"
        aria-invalid={hint ? true : undefined}
        aria-describedby={hintId}
        onChange={(event) => {
          setDraft(event.target.value);
          if (triedEmpty) setTriedEmpty(false);
        }}
        onKeyDown={handleKeyDown}
      />
      <div className="flex items-center justify-between gap-3">
        <p id={hintId} className="flex min-w-0 flex-wrap gap-x-3 text-xs text-muted-foreground">
          <span className={tooLong ? "text-destructive" : undefined}>
            {length} / {MAX_MESSAGE_CODE_POINTS}
          </span>
          {hint ? <span className="text-destructive">{hint}</span> : null}
        </p>
        {busy ? (
          <Button type="button" variant="outline" size="sm" onClick={cancel}>
            생성 중단
          </Button>
        ) : (
          <Button type="submit" size="sm" disabled={tooLong}>
            보내기
          </Button>
        )}
      </div>
    </form>
  );
}
