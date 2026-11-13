"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Button } from "@/shared/components/ui/button";

import { useAdminMedia, useDeleteMedia } from "../api/media";
import type { Media } from "../api/types";
import { describeError } from "../model/errorMessage";
import { formatDateTime } from "../model/formatDateTime";
import { fallbackPage, toApiPage } from "../model/pagination";
import { ConfirmDialog } from "./ConfirmDialog";
import { MediaThumb } from "./MediaThumb";
import { Notice } from "./Notice";
import { PageHeader } from "./PageHeader";
import { Pager } from "./Pager";
import { useReauth, useReauthOnError } from "./ReauthProvider";
import { UploadField } from "./UploadField";
import { useSubmitLock } from "./useSubmitLock";

const pageHref = (page: number) => (page <= 1 ? "/admin/media" : `/admin/media?page=${page}`);
const bytes = new Intl.NumberFormat("ko-KR");

/** 이미지 라이브러리, 업로드와 미사용 자산 삭제 */
export function MediaLibrary({ page }: { page: number }) {
  const router = useRouter();
  const library = useAdminMedia(toApiPage(page));
  const remove = useDeleteMedia();
  const lock = useSubmitLock();
  const { openReauth } = useReauth();
  const [target, setTarget] = useState<Media | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  useReauthOnError(library.error);

  const data = library.data;
  const fallback = data && !library.isPlaceholderData ? fallbackPage(data) : null;
  useEffect(() => {
    if (fallback !== null && fallback !== page) router.replace(pageHref(fallback));
  }, [fallback, page, router]);

  const confirmDelete = () =>
    void lock(async () => {
      if (!target) return;
      setDeleteError(null);
      try {
        await remove.mutateAsync(target.id);
        setMessage(`${target.originalFilename}을 삭제했습니다.`);
        setTarget(null);
      } catch (caught) {
        const described = describeError(caught);
        if (described.scope === "auth") openReauth();
        setDeleteError(described.message);
      }
    });

  return (
    <section className="grid gap-8">
      <PageHeader
        title="미디어"
        description="업로드한 이미지는 작업물과 별개로 보관됩니다. 작업물에서 연결을 해제해도 파일은 남고, 삭제는 여기에서만 합니다."
      />

      <div className="border p-4 sm:p-6">
        <UploadField
          onUploaded={(media) => {
            setMessage(`${media.originalFilename}을 업로드했습니다.`);
            if (page !== 1) router.push(pageHref(1));
          }}
          onRecheck={() => void library.refetch()}
        />
      </div>

      {message ? <Notice tone="status">{message}</Notice> : null}

      {library.isPending ? (
        <p role="status" className="text-sm text-muted-foreground">
          이미지를 불러오는 중…
        </p>
      ) : !data ? (
        <Notice tone="alert" className="grid justify-items-start gap-3">
          <span>미디어 목록을 불러오지 못했습니다. {describeError(library.error).message}</span>
          <Button variant="outline" size="sm" disabled={library.isFetching} onClick={() => void library.refetch()}>
            다시 시도
          </Button>
        </Notice>
      ) : data.totalElements === 0 ? (
        <div className="grid gap-2 border border-dashed px-6 py-12">
          <p className="font-heading text-xl">아직 이미지가 없습니다</p>
          <p className="text-sm text-muted-foreground">위에서 이미지를 업로드하면 작업물에 연결할 수 있습니다.</p>
        </div>
      ) : (
        <>
          <ul aria-label="미디어 목록" className="grid grid-cols-2 gap-px border bg-border md:grid-cols-3 lg:grid-cols-4">
            {data.content.map((media) => (
              <li key={media.id} className="grid content-start gap-3 bg-background p-3">
                <MediaThumb src={media.url} alt={media.originalFilename} size={320} className="aspect-square w-full" fit="contain" />
                <div className="grid min-w-0 gap-1">
                  <p className="text-sm font-medium break-all">{media.originalFilename}</p>
                  <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5 text-xs text-muted-foreground">
                    <dt>형식</dt>
                    <dd className="break-all">{media.contentType}</dd>
                    <dt>크기</dt>
                    <dd className="tabular-nums">{bytes.format(media.fileSize)} bytes</dd>
                    <dt>업로드</dt>
                    <dd className="tabular-nums">{formatDateTime(media.createdAt)}</dd>
                    <dt>ID</dt>
                    <dd className="tabular-nums">{media.id}</dd>
                  </dl>
                </div>
                <Button
                  variant="ghost"
                  size="xs"
                  className="justify-self-start text-destructive max-sm:h-9"
                  aria-label={`${media.originalFilename} 삭제`}
                  onClick={() => {
                    setDeleteError(null);
                    setTarget(media);
                  }}
                >
                  삭제
                </Button>
              </li>
            ))}
          </ul>
          <Pager page={page} totalElements={data.totalElements} size={data.size} hrefFor={pageHref} label="미디어 페이지" />
        </>
      )}

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => !open && setTarget(null)}
        title="이미지를 삭제할까요?"
        description={
          <>
            <p>
              <strong className="text-foreground break-all">{target?.originalFilename}</strong>을 라이브러리에서 삭제합니다. 삭제한 파일은
              복구할 수 없습니다.
            </p>
            <p>작업물(숨김 포함)에서 사용 중이면 삭제가 거부됩니다.</p>
          </>
        }
        confirmLabel="삭제"
        pendingLabel="삭제 중…"
        pending={remove.isPending}
        destructive
        error={deleteError}
        onConfirm={confirmDelete}
      />
    </section>
  );
}
