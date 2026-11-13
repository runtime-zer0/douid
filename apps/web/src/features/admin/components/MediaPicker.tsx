"use client";

import { useState } from "react";

import { Button } from "@/shared/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/components/ui/dialog";

import { useAdminMedia } from "../api/media";
import type { Media } from "../api/types";
import { MediaThumb } from "./MediaThumb";
import { Notice } from "./Notice";
import { Pager } from "./Pager";
import { useReauthOnError } from "./ReauthProvider";
import { UploadField } from "./UploadField";
import { useReturnFocus } from "./useReturnFocus";

type MediaPickerProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onPick: (media: Media) => void;
};

/** 작업물에 연결할 이미지 선택, 라이브러리에서 고르거나 새로 업로드하면 바로 연결 */
export function MediaPicker({ open, onOpenChange, onPick }: MediaPickerProps) {
  const [page, setPage] = useState(1);
  const library = useAdminMedia(page - 1);
  const returnFocus = useReturnFocus();
  useReauthOnError(library.error);

  const pick = (media: Media) => {
    onPick(media);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent {...returnFocus} className="max-h-[calc(100dvh-2rem)] overflow-y-auto overscroll-contain sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle className="font-heading text-xl">이미지 추가</DialogTitle>
          <DialogDescription>라이브러리 이미지를 고르거나 새로 업로드하면 작업물 구성 끝에 상세 이미지로 추가됩니다.</DialogDescription>
        </DialogHeader>

        <UploadField onUploaded={pick} onRecheck={() => void library.refetch()} />

        <section aria-labelledby="picker-library" className="grid gap-4 border-t pt-6">
          <h2 id="picker-library" className="text-sm font-semibold">
            라이브러리
          </h2>
          {library.isPending ? (
            <p role="status" className="text-sm text-muted-foreground">
              이미지를 불러오는 중…
            </p>
          ) : !library.data ? (
            <Notice tone="alert" className="grid justify-items-start gap-2">
              <span>라이브러리를 불러오지 못했습니다.</span>
              <Button variant="outline" size="xs" onClick={() => void library.refetch()}>
                다시 시도
              </Button>
            </Notice>
          ) : library.data.totalElements === 0 ? (
            <p className="text-sm text-muted-foreground">라이브러리에 이미지가 없습니다. 위에서 업로드해 주세요.</p>
          ) : (
            <>
              <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                {library.data.content.map((media) => (
                  <li key={media.id}>
                    <button
                      type="button"
                      onClick={() => pick(media)}
                      aria-label={`${media.originalFilename} 연결`}
                      className="group grid w-full gap-2 text-left outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                      <MediaThumb src={media.url} alt="" size={160} className="aspect-square w-full" />
                      <span className="truncate text-xs text-muted-foreground group-hover:text-foreground">
                        {media.originalFilename}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
              <Pager
                page={page}
                totalElements={library.data.totalElements}
                size={library.data.size}
                onPageChange={setPage}
                label="라이브러리 페이지"
              />
            </>
          )}
        </section>
      </DialogContent>
    </Dialog>
  );
}
