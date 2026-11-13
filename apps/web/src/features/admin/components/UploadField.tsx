"use client";

import { useId, useRef, useState } from "react";

import { Button } from "@/shared/components/ui/button";

import { useUploadMedia } from "../api/media";
import type { Media } from "../api/types";
import { describeError } from "../model/errorMessage";
import { checkUpload, MAX_UPLOAD_LABEL } from "../model/uploadCheck";
import { Notice } from "./Notice";
import { useReauth } from "./ReauthProvider";
import { useSubmitLock } from "./useSubmitLock";

type UploadFieldProps = {
  onUploaded: (media: Media) => void;
  /** 응답 유실 시 목록을 다시 받는 동작 */
  onRecheck: () => void;
};

/** 이미지 1개 업로드, 사전 검증 후 서버 결과로만 성공 처리 */
export function UploadField({ onUploaded, onRecheck }: UploadFieldProps) {
  const id = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const upload = useUploadMedia();
  const lock = useSubmitLock();
  const { openReauth } = useReauth();
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<{ text: string; uncertain?: boolean } | null>(null);

  const submit = () =>
    lock(async () => {
      if (!file) return;
      const problem = checkUpload(file);
      if (problem) {
        setError({ text: problem });
        return;
      }
      setError(null);
      try {
        const media = await upload.mutateAsync(file);
        setFile(null);
        if (inputRef.current) inputRef.current.value = "";
        onUploaded(media);
      } catch (caught) {
        const described = describeError(caught);
        if (described.scope === "auth") openReauth();
        setError({ text: described.message, uncertain: described.uncertain });
      }
    });

  return (
    <div className="grid gap-3">
      <div className="flex flex-wrap items-end gap-3">
        <div className="grid min-w-0 flex-1 gap-2">
          <label htmlFor={id} className="text-xs font-semibold tracking-wide uppercase">
            이미지 업로드
          </label>
          <input
            ref={inputRef}
            id={id}
            type="file"
            accept="image/*"
            aria-describedby={`${id}-hint${error ? ` ${id}-error` : ""}`}
            aria-invalid={error ? true : undefined}
            onChange={(event) => {
              const picked = event.target.files?.[0] ?? null;
              const problem = picked ? checkUpload(picked) : null;
              setFile(picked);
              setError(problem ? { text: problem } : null);
            }}
            className="block w-full text-sm file:mr-4 file:border file:border-border file:bg-transparent file:px-4 file:py-2 file:text-xs file:font-semibold file:tracking-widest file:uppercase"
          />
        </div>
        <Button type="button" variant="outline" disabled={!file || upload.isPending} onClick={() => void submit()}>
          {upload.isPending ? "업로드 중…" : "업로드"}
        </Button>
      </div>
      <p id={`${id}-hint`} className="text-xs text-muted-foreground">
        이미지 파일 1개, {MAX_UPLOAD_LABEL} 미만. 업로드한 이미지는 작업물 저장과 별개로 라이브러리에 남습니다.
      </p>
      {error ? (
        <Notice tone="alert" id={`${id}-error`} className="grid justify-items-start gap-2">
          <span>{error.uncertain ? "업로드 결과를 확인하지 못했습니다. 라이브러리에 이미 올라갔을 수 있으니 목록을 새로 불러와 확인해 주세요." : error.text}</span>
          {error.uncertain ? (
            <Button type="button" variant="outline" size="xs" onClick={onRecheck}>
              목록 새로 불러오기
            </Button>
          ) : null}
        </Notice>
      ) : null}
    </div>
  );
}
