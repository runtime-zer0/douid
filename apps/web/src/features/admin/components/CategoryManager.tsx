"use client";

import { useId, useState, type FormEvent } from "react";

import { Button } from "@/shared/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/components/ui/dialog";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/components/ui/table";

import { useAdminCategories, useCreateCategory, useDeleteCategory, useUpdateCategory } from "../api/categories";
import type { Category } from "../api/types";
import {
  categoryDraftFrom,
  emptyCategoryDraft,
  needsHideConfirm,
  toCategoryBody,
  validateCategoryDraft,
  type CategoryDraft,
  type CategoryDraftErrors,
} from "../model/categoryDraft";
import { describeError } from "../model/errorMessage";
import { ConfirmDialog } from "./ConfirmDialog";
import { Notice } from "./Notice";
import { PageHeader } from "./PageHeader";
import { useReauth, useReauthOnError } from "./ReauthProvider";
import { useUnsavedChangesGuard } from "./useUnsavedChangesGuard";
import { useReturnFocus } from "./useReturnFocus";
import { useSubmitLock } from "./useSubmitLock";

type Editing = { mode: "create" } | { mode: "edit"; id: number };

/** 공개·비공개 전체 카테고리 관리 */
export function CategoryManager() {
  const categories = useAdminCategories();
  const remove = useDeleteCategory();
  const lock = useSubmitLock();
  const { openReauth } = useReauth();
  const [editing, setEditing] = useState<Editing | null>(null);
  const [target, setTarget] = useState<Category | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  useReauthOnError(categories.error);

  const confirmDelete = () =>
    void lock(async () => {
      if (!target) return;
      setDeleteError(null);
      try {
        await remove.mutateAsync(target.id);
        setMessage(`“${target.name}” 카테고리를 삭제했습니다.`);
        setTarget(null);
      } catch (caught) {
        const described = describeError(caught);
        if (described.scope === "auth") openReauth();
        setDeleteError(described.message);
      }
    });

  const header = (
    <PageHeader
      title="카테고리"
      description="작업물이 공개 조회에 포함되려면 작업물과 카테고리가 모두 공개여야 합니다. 순서는 작은 값부터 표시됩니다."
      action={<Button onClick={() => setEditing({ mode: "create" })}>새 카테고리</Button>}
    />
  );

  let body;
  if (categories.isPending) {
    body = (
      <p role="status" className="text-sm text-muted-foreground">
        카테고리를 불러오는 중…
      </p>
    );
  } else if (!categories.data) {
    body = (
      <Notice tone="alert" className="grid justify-items-start gap-3">
        <span>카테고리 목록을 불러오지 못했습니다. {describeError(categories.error).message}</span>
        <Button variant="outline" size="sm" disabled={categories.isFetching} onClick={() => void categories.refetch()}>
          다시 시도
        </Button>
      </Notice>
    );
  } else if (categories.data.length === 0) {
    body = (
      <div className="grid justify-items-start gap-4 border border-dashed px-6 py-12">
        <p className="font-heading text-xl">아직 카테고리가 없습니다</p>
        <p className="text-sm text-muted-foreground">카테고리가 없어도 작업물은 미분류로 저장할 수 있습니다.</p>
        <Button variant="outline" onClick={() => setEditing({ mode: "create" })}>
          새 카테고리 만들기
        </Button>
      </div>
    );
  } else {
    body = (
      <Table aria-label="카테고리 목록">
        <TableHeader>
          <TableRow>
            <TableHead>이름</TableHead>
            <TableHead className="hidden sm:table-cell">슬러그</TableHead>
            <TableHead className="text-right">순서</TableHead>
            <TableHead>공개 여부</TableHead>
            <TableHead className="text-right">동작</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {categories.data.map((category) => (
            <TableRow key={category.id}>
              <TableCell className="whitespace-normal">
                <span className="font-medium break-words">{category.name}</span>
                <span translate="no" className="block text-xs break-all text-muted-foreground sm:hidden">/{category.slug}</span>
              </TableCell>
              <TableCell translate="no" className="hidden break-all text-muted-foreground sm:table-cell">/{category.slug}</TableCell>
              <TableCell className="text-right tabular-nums">{category.displayOrder}</TableCell>
              <TableCell>
                <span className="inline-flex items-center gap-2">
                  <span
                    aria-hidden="true"
                    className={category.visible ? "size-2 bg-foreground" : "size-2 border border-muted-foreground"}
                  />
                  {category.visible ? "공개" : "비공개"}
                </span>
              </TableCell>
              <TableCell>
                <div className="flex flex-col items-end gap-1 max-sm:[&_[data-size=xs]]:h-9 sm:flex-row sm:justify-end">
                  <Button
                    variant="ghost"
                    size="xs"
                    aria-label={`${category.name} 편집`}
                    onClick={() => setEditing({ mode: "edit", id: category.id })}
                  >
                    편집
                  </Button>
                  <Button
                    variant="ghost"
                    size="xs"
                    className="text-destructive"
                    aria-label={`${category.name} 삭제`}
                    onClick={() => {
                      setDeleteError(null);
                      setTarget(category);
                    }}
                  >
                    삭제
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    );
  }

  return (
    <section className="grid gap-8">
      {header}
      {message ? <Notice tone="status">{message}</Notice> : null}
      {body}

      {editing ? (
        <CategoryFormDialog
          key={editing.mode === "edit" ? editing.id : "new"}
          editing={editing}
          categories={categories.data ?? []}
          onClose={() => setEditing(null)}
          onSaved={(text) => {
            setMessage(text);
            setEditing(null);
          }}
        />
      ) : null}

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => !open && setTarget(null)}
        title="카테고리를 삭제할까요?"
        description={
          <>
            <p>
              <strong className="text-foreground">{target?.name}</strong> 카테고리를 삭제합니다.
            </p>
            <p>연결된 작업물이 있으면(비공개 작업물 포함) 삭제가 거부됩니다.</p>
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

type CategoryFormDialogProps = {
  editing: Editing;
  categories: Category[];
  onClose: () => void;
  onSaved: (message: string) => void;
};

function CategoryFormDialog({ editing, categories, onClose, onSaved }: CategoryFormDialogProps) {
  const id = useId();
  const lock = useSubmitLock();
  const { openReauth } = useReauth();
  const create = useCreateCategory();
  const update = useUpdateCategory();
  const [baseline] = useState<CategoryDraft | null>(() => {
    if (editing.mode === "create") return null;
    const found = categories.find((category) => category.id === editing.id);
    return found ? categoryDraftFrom(found) : null;
  });
  const [draft, setDraft] = useState<CategoryDraft>(baseline ?? emptyCategoryDraft());
  const [errors, setErrors] = useState<CategoryDraftErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [hideConfirm, setHideConfirm] = useState(false);
  const dirty = JSON.stringify(draft) !== JSON.stringify(baseline ?? emptyCategoryDraft());
  const confirmLeave = useUnsavedChangesGuard(dirty);
  const saving = create.isPending || update.isPending;
  const returnFocus = useReturnFocus();

  if (editing.mode === "edit" && !baseline) {
    return (
      <Dialog open onOpenChange={(open) => !open && onClose()}>
        <DialogContent {...returnFocus}>
          <DialogHeader>
            <DialogTitle className="font-heading text-xl">카테고리 편집</DialogTitle>
            <DialogDescription>최신 목록에서 이 카테고리를 찾지 못했습니다. 이미 삭제되었을 수 있습니다.</DialogDescription>
          </DialogHeader>
        </DialogContent>
      </Dialog>
    );
  }

  const save = () =>
    lock(async () => {
      setFormError(null);
      const body = toCategoryBody(draft);
      try {
        if (editing.mode === "create") {
          await create.mutateAsync(body);
          onSaved(`“${draft.name}” 카테고리를 만들었습니다.`);
        } else {
          await update.mutateAsync({ id: editing.id, body });
          onSaved(`“${draft.name}” 카테고리를 저장했습니다.`);
        }
      } catch (caught) {
        setHideConfirm(false);
        const described = describeError(caught);
        if (described.scope === "auth") openReauth();
        const fieldErrors = described.fieldErrors ?? {};
        setErrors({
          name: fieldErrors.name,
          slug: fieldErrors.slug,
          displayOrder: fieldErrors.displayOrder,
        });
        setFormError(described.code === "CATEGORY_NOT_FOUND" ? "카테고리를 찾을 수 없습니다. 이미 삭제되었을 수 있습니다." : described.message);
      }
    });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const found = validateCategoryDraft(draft);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      setFormError("입력값을 확인해 주세요.");
      return;
    }
    if (needsHideConfirm(baseline, draft)) {
      setHideConfirm(true);
      return;
    }
    void save();
  };

  const field = (name: keyof CategoryDraftErrors) => ({
    "aria-invalid": errors[name] ? true : undefined,
    "aria-describedby": errors[name] ? `${id}-${name}-error` : undefined,
  });

  return (
    <>
      <Dialog open onOpenChange={(open) => !open && !saving && confirmLeave() && onClose()}>
        <DialogContent {...returnFocus}>
          <DialogHeader>
            <DialogTitle className="font-heading text-xl">{editing.mode === "create" ? "새 카테고리" : "카테고리 편집"}</DialogTitle>
            <DialogDescription>이름, 슬러그, 순서, 공개 여부를 모두 저장합니다.</DialogDescription>
          </DialogHeader>
          <form onSubmit={submit} noValidate className="grid gap-6">
            {formError ? <Notice tone="alert">{formError}</Notice> : null}
            <div className="grid gap-2">
              <Label htmlFor={`${id}-name`}>이름</Label>
              <Input id={`${id}-name`} autoComplete="off" value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} {...field("name")} />
              <ErrorText id={`${id}-name-error`} message={errors.name} />
            </div>
            <div className="grid gap-2">
              <Label htmlFor={`${id}-slug`}>슬러그</Label>
              <Input id={`${id}-slug`} autoComplete="off" spellCheck={false} value={draft.slug} onChange={(event) => setDraft({ ...draft, slug: event.target.value })} {...field("slug")} />
              <ErrorText id={`${id}-slug-error`} message={errors.slug} />
            </div>
            <div className="grid gap-2">
              <Label htmlFor={`${id}-order`}>순서</Label>
              <Input
                id={`${id}-order`}
                inputMode="numeric"
                autoComplete="off"
                value={draft.displayOrder}
                onChange={(event) => setDraft({ ...draft, displayOrder: event.target.value })}
                aria-invalid={errors.displayOrder ? true : undefined}
                aria-describedby={`${id}-order-hint${errors.displayOrder ? ` ${id}-displayOrder-error` : ""}`}
                className="tabular-nums"
              />
              <p id={`${id}-order-hint`} className="text-xs text-muted-foreground">
                정수, 음수도 가능합니다.
              </p>
              <ErrorText id={`${id}-displayOrder-error`} message={errors.displayOrder} />
            </div>
            <label className="flex items-center gap-3 text-sm">
              <input
                type="checkbox"
                checked={draft.visible}
                onChange={(event) => setDraft({ ...draft, visible: event.target.checked })}
                className="size-4 accent-foreground"
              />
              공개 카테고리
            </label>
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" disabled={saving} onClick={() => confirmLeave() && onClose()}>
                취소
              </Button>
              <Button type="submit" disabled={saving}>
                {saving ? "저장 중…" : editing.mode === "create" ? "카테고리 만들기" : "변경 저장"}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={hideConfirm}
        onOpenChange={setHideConfirm}
        title="카테고리를 비공개로 바꿀까요?"
        description={
          <p>이 카테고리의 공개 작업물도 서버 공개 조회에서 제외됩니다. 각 작업물의 공개 설정은 그대로 유지됩니다.</p>
        }
        confirmLabel="비공개로 저장"
        pendingLabel="저장 중…"
        pending={saving}
        onConfirm={() => void save()}
      />
    </>
  );
}

function ErrorText({ id, message }: { id: string; message?: string }) {
  return message ? (
    <p id={id} className="text-xs text-destructive">
      {message}
    </p>
  ) : null;
}
