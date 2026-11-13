"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Button } from "@/shared/components/ui/button";
import { buttonVariants } from "@/shared/components/ui/button-variants";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/components/ui/table";

import { useAdminCategories } from "../api/categories";
import type { AdminWorkListItem } from "../api/types";
import { useAdminWorks, useChangeWorkVisibility, useDeleteWork } from "../api/works";
import { describeError } from "../model/errorMessage";
import { publicExposure } from "../model/exposure";
import { formatDateTime } from "../model/formatDateTime";
import { fallbackPage, toApiPage } from "../model/pagination";
import { ConfirmDialog } from "./ConfirmDialog";
import { ExposureBadge } from "./ExposureBadge";
import { MediaThumb } from "./MediaThumb";
import { Notice } from "./Notice";
import { PageHeader } from "./PageHeader";
import { Pager } from "./Pager";
import { useReauth, useReauthOnError } from "./ReauthProvider";
import { useSubmitLock } from "./useSubmitLock";

const pageHref = (page: number) => (page <= 1 ? "/admin/works" : `/admin/works?page=${page}`);

/** 공개·비공개 전체 작업물 목록, 서버 순서 그대로 페이지 단위 표시 */
export function WorkList({ page }: { page: number }) {
  const router = useRouter();
  const works = useAdminWorks(toApiPage(page));
  const categories = useAdminCategories();
  const visibility = useChangeWorkVisibility();
  const remove = useDeleteWork();
  const lock = useSubmitLock();
  const { openReauth } = useReauth();
  const [message, setMessage] = useState<{ tone: "alert" | "status"; text: string } | null>(null);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [target, setTarget] = useState<AdminWorkListItem | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useReauthOnError(works.error);
  const data = works.data;
  const fallback = data && !works.isPlaceholderData ? fallbackPage(data) : null;

  useEffect(() => {
    if (fallback !== null && fallback !== page) router.replace(pageHref(fallback));
  }, [fallback, page, router]);

  const failed = (caught: unknown) => {
    const described = describeError(caught);
    if (described.scope === "auth") openReauth();
    return described.message;
  };

  const toggle = (work: AdminWorkListItem) =>
    lock(async () => {
      const next = work.visibility === "VISIBLE" ? "HIDDEN" : "VISIBLE";
      setPendingId(work.id);
      setMessage(null);
      try {
        await visibility.mutateAsync({ id: work.id, visibility: next });
        setMessage({ tone: "status", text: `“${work.title}”을 ${next === "VISIBLE" ? "공개" : "비공개"}로 바꿨습니다.` });
      } catch (caught) {
        setMessage({ tone: "alert", text: `공개 설정을 바꾸지 못했습니다. ${failed(caught)}` });
      } finally {
        setPendingId(null);
      }
    });

  const confirmDelete = () =>
    lock(async () => {
      if (!target) return;
      setDeleteError(null);
      try {
        await remove.mutateAsync(target.id);
        setMessage({ tone: "status", text: `“${target.title}”을 삭제했습니다. 연결했던 이미지는 미디어 라이브러리에 남아 있습니다.` });
        setTarget(null);
      } catch (caught) {
        setDeleteError(failed(caught));
      }
    });

  const header = (
    <PageHeader
      title="작업물"
      description="공개 여부와 관계없이 모든 작업물을 최근 생성 순으로 보여줍니다."
      action={
        <Link href="/admin/works/new" className={buttonVariants()}>
          새 작업물
        </Link>
      }
    />
  );

  if (works.isPending) {
    return (
      <section className="grid gap-8">
        {header}
        <p role="status" className="text-sm text-muted-foreground">
          작업물을 불러오는 중…
        </p>
      </section>
    );
  }

  if (!data) {
    return (
      <section className="grid gap-8">
        {header}
        <Notice tone="alert" className="grid gap-3 justify-items-start">
          <span>작업물 목록을 불러오지 못했습니다. {describeError(works.error).message}</span>
          <Button variant="outline" size="sm" disabled={works.isFetching} onClick={() => void works.refetch()}>
            {works.isFetching ? "다시 불러오는 중…" : "다시 시도"}
          </Button>
        </Notice>
      </section>
    );
  }

  return (
    <section className="grid gap-8">
      {header}
      {message ? <Notice tone={message.tone}>{message.text}</Notice> : null}
      {works.isFetching && !works.isPending ? (
        <p role="status" className="-mt-4 text-xs text-muted-foreground">
          목록을 새로 고치는 중…
        </p>
      ) : null}

      {data.totalElements === 0 ? (
        <div className="grid justify-items-start gap-4 border border-dashed px-6 py-12">
          <p className="font-heading text-xl">아직 작업물이 없습니다</p>
          <p className="text-sm text-muted-foreground">첫 작업물을 만들면 여기에서 공개 여부와 이미지 구성을 관리할 수 있습니다.</p>
          <Link href="/admin/works/new" className={buttonVariants({ variant: "outline" })}>
            새 작업물 만들기
          </Link>
        </div>
      ) : (
        <>
          <Table aria-label="작업물 목록">
            <TableHeader>
              <TableRow>
                <TableHead className="hidden w-16 md:table-cell">대표 이미지</TableHead>
                <TableHead>작업물</TableHead>
                <TableHead className="hidden md:table-cell">공개 상태</TableHead>
                <TableHead className="hidden lg:table-cell">카테고리</TableHead>
                <TableHead className="hidden lg:table-cell">생성 / 수정</TableHead>
                <TableHead className="text-right">동작</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((work) => {
                const exposure = publicExposure(
                  { visibility: work.visibility, categoryId: work.category?.id ?? null },
                  categories.data,
                );
                const busy = pendingId === work.id;
                return (
                  <TableRow key={work.id} className="align-top">
                    <TableCell className="hidden md:table-cell">
                      <MediaThumb
                        src={work.thumbnail?.fileUrl ?? null}
                        alt={work.thumbnail?.altText ?? ""}
                        size={48}
                        className="size-12"
                      />
                    </TableCell>
                    <TableCell className="max-w-72 whitespace-normal">
                      <Link
                        href={`/admin/works/${work.id}/edit`}
                        className="font-medium break-words underline-offset-4 hover:underline"
                      >
                        {work.title}
                      </Link>
                      <p translate="no" className="mt-1 text-xs break-all text-muted-foreground">/{work.slug}</p>
                      <div className="mt-2 grid gap-1 md:hidden">
                        <ExposureBadge exposure={exposure} />
                        <span className="text-xs text-muted-foreground">{work.category?.name ?? "미분류"}</span>
                      </div>
                    </TableCell>
                    <TableCell className="hidden whitespace-normal md:table-cell">
                      <ExposureBadge exposure={exposure} />
                    </TableCell>
                    <TableCell className="hidden lg:table-cell">
                      {work.category ? work.category.name : <span className="text-muted-foreground">미분류</span>}
                    </TableCell>
                    <TableCell className="hidden text-xs text-muted-foreground tabular-nums lg:table-cell">
                      <span className="block">{formatDateTime(work.createdAt)}</span>
                      <span className="block">{formatDateTime(work.updatedAt)}</span>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-col items-end gap-1 max-sm:[&_[data-size=xs]]:h-9 sm:flex-row sm:justify-end">
                        <Link
                          href={`/admin/works/${work.id}/edit`}
                          className={buttonVariants({ variant: "ghost", size: "xs", className: "max-sm:h-9" })}
                          aria-label={`${work.title} 편집`}
                        >
                          편집
                        </Link>
                        <Button
                          variant="ghost"
                          size="xs"
                          disabled={busy}
                          aria-label={`${work.title} ${work.visibility === "VISIBLE" ? "비공개로 전환" : "공개로 전환"}`}
                          onClick={() => void toggle(work)}
                        >
                          {busy ? "변경 중…" : work.visibility === "VISIBLE" ? "비공개로" : "공개로"}
                        </Button>
                        <Button
                          variant="ghost"
                          size="xs"
                          className="text-destructive"
                          aria-label={`${work.title} 삭제`}
                          onClick={() => {
                            setDeleteError(null);
                            setTarget(work);
                          }}
                        >
                          삭제
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
          <Pager page={page} totalElements={data.totalElements} size={data.size} hrefFor={pageHref} label="작업물 페이지" />
        </>
      )}

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => !open && setTarget(null)}
        title="작업물을 삭제할까요?"
        description={
          <>
            <p>
              <strong className="text-foreground">{target?.title}</strong>을 삭제합니다. 삭제한 작업물은 복구할 수 없습니다.
            </p>
            <p>연결했던 이미지는 미디어 라이브러리에 남습니다.</p>
          </>
        }
        confirmLabel="삭제"
        pendingLabel="삭제 중…"
        pending={remove.isPending}
        destructive
        error={deleteError}
        onConfirm={() => void confirmDelete()}
      />
    </section>
  );
}
