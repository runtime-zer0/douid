"use client";

import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useId, useRef, useState, type FormEvent, type ReactNode } from "react";

import { Button } from "@/shared/components/ui/button";
import { buttonVariants } from "@/shared/components/ui/button-variants";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { Textarea } from "@/shared/components/ui/textarea";

import { useAdminCategories } from "../api/categories";
import { adminKeys } from "../api/queryKeys";
import type { AdminWorkDetail, Media, WorkCategoryRef, WorkVisibility } from "../api/types";
import { fetchWork, useAdminWork, useCreateWork, useDeleteWork, useUpdateWork } from "../api/works";
import { describeError } from "../model/errorMessage";
import { publicExposure } from "../model/exposure";
import {
  addMedia,
  draftFromDetail,
  emptyDraft,
  hasErrors,
  isDirty,
  move,
  removeItem,
  setAltText,
  setRole,
  toWorkBody,
  validateWorkDraft,
  type WorkDraft,
  type WorkDraftErrors,
} from "../model/workDraft";
import { ConfirmDialog } from "./ConfirmDialog";
import { ExposureBadge } from "./ExposureBadge";
import { MediaPicker } from "./MediaPicker";
import { Notice } from "./Notice";
import { PageHeader } from "./PageHeader";
import { useReauth, useReauthOnError } from "./ReauthProvider";
import { useUnsavedChangesGuard } from "./useUnsavedChangesGuard";
import { useSubmitLock } from "./useSubmitLock";
import { WorkMediaEditor } from "./WorkMediaEditor";

type WorkFormProps = { mode: "create" } | { mode: "edit"; id: number; created?: boolean };

/** 작업물 생성·편집 화면, 편집은 관리자 상세 응답으로만 초기화 */
export function WorkForm(props: WorkFormProps) {
  if (props.mode === "create") return <WorkEditor mode="create" />;
  return <WorkEditLoader id={props.id} created={props.created ?? false} />;
}

function WorkEditLoader({ id, created }: { id: number; created: boolean }) {
  const work = useAdminWork(id);
  useReauthOnError(work.error);

  if (work.data) return <WorkEditor key={id} mode="edit" detail={work.data} created={created} />;

  const header = <PageHeader title="작업물 편집" />;
  if (work.isPending) {
    return (
      <section className="grid gap-8">
        {header}
        <p role="status" className="text-sm text-muted-foreground">
          작업물을 불러오는 중…
        </p>
      </section>
    );
  }
  const described = describeError(work.error);
  return (
    <section className="grid gap-8">
      {header}
      <Notice tone="alert" className="grid justify-items-start gap-3">
        <span>
          {described.code === "WORK_NOT_FOUND" ? "작업물을 찾을 수 없습니다. 이미 삭제되었을 수 있습니다." : `작업물을 불러오지 못했습니다. ${described.message}`}
        </span>
        <div className="flex flex-wrap gap-2">
          {described.code !== "WORK_NOT_FOUND" ? (
            <Button variant="outline" size="sm" disabled={work.isFetching} onClick={() => void work.refetch()}>
              다시 시도
            </Button>
          ) : null}
          <Link href="/admin/works" className={buttonVariants({ variant: "outline", size: "sm" })}>
            작업물 목록으로
          </Link>
        </div>
      </Notice>
    </section>
  );
}

type FieldErrors = WorkDraftErrors & Partial<Record<"categoryId" | "description" | "visibility", string>>;

type EditorProps = { mode: "create" } | { mode: "edit"; detail: AdminWorkDetail; created: boolean };

function WorkEditor(props: EditorProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { openReauth } = useReauth();
  const lock = useSubmitLock();
  const formId = useId();
  const summaryRef = useRef<HTMLDivElement>(null);
  const addMediaRef = useRef<HTMLButtonElement>(null);
  const categories = useAdminCategories();
  const editId = props.mode === "edit" ? props.detail.id : null;
  const create = useCreateWork();
  const update = useUpdateWork(editId ?? 0);
  const remove = useDeleteWork();

  const [initialCategory] = useState<WorkCategoryRef | null>(props.mode === "edit" ? props.detail.category : null);
  const [baseline, setBaseline] = useState<WorkDraft>(() =>
    props.mode === "edit" ? draftFromDetail(props.detail) : emptyDraft(),
  );
  const [draft, setDraft] = useState<WorkDraft>(baseline);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [message, setMessage] = useState<{ tone: "alert" | "status"; content: ReactNode } | null>(() =>
    props.mode === "edit" && props.created ? { tone: "status", content: "작업물을 만들었습니다." } : null,
  );
  const [pickerOpen, setPickerOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const dirty = isDirty(draft, baseline);
  const confirmLeave = useUnsavedChangesGuard(dirty);
  const saving = create.isPending || update.isPending;
  const exposure = publicExposure(draft, categories.data);
  const savedExposure = publicExposure(baseline, categories.data);

  const edit = (next: (current: WorkDraft) => WorkDraft) => setDraft(next);
  const fieldProps = (name: keyof FieldErrors) => ({
    "aria-invalid": errors[name] ? true : undefined,
    "aria-describedby": errors[name] ? `${formId}-${name}-error` : undefined,
  });

  const showFailure = (caught: unknown) => {
    const described = describeError(caught);
    if (described.scope === "auth") openReauth();
    if (described.code === "CATEGORY_NOT_FOUND") void queryClient.invalidateQueries({ queryKey: adminKeys.categories });
    if (described.code === "MEDIA_NOT_FOUND" || described.code === "WORK_THUMBNAIL_DUPLICATE") {
      void queryClient.invalidateQueries({ queryKey: adminKeys.mediaAll });
    }
    const serverFields: FieldErrors = {};
    for (const [field, text] of Object.entries(described.fieldErrors ?? {})) {
      const name = field.startsWith("mediaItems") ? "mediaItems" : field;
      if (["title", "slug", "summary", "description", "categoryId", "visibility", "mediaItems"].includes(name)) {
        serverFields[name as keyof Omit<FieldErrors, "altText">] = text;
      }
    }
    setErrors(serverFields);

    let content: ReactNode = described.message;
    if (described.uncertain) {
      content = (
        <>
          {props.mode === "create"
            ? "응답을 받지 못해 작업물이 만들어졌는지 확인하지 못했습니다. 다시 저장하기 전에 "
            : "응답을 받지 못해 저장 여부를 확인하지 못했습니다. 다시 저장하기 전에 "}
          <Link href="/admin/works" target="_blank" className="underline underline-offset-4">
            작업물 목록
          </Link>
          에서 반영 여부를 확인해 주세요. 입력한 내용은 그대로 남아 있습니다.
        </>
      );
    } else if (described.code === "WORK_NOT_FOUND") {
      content = (
        <>
          {described.message}{" "}
          <Link href="/admin/works" className="underline underline-offset-4">
            작업물 목록으로
          </Link>
        </>
      );
    }
    setMessage({ tone: "alert", content });
    summaryRef.current?.focus();
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void lock(async () => {
      const found = validateWorkDraft(draft);
      if (hasErrors(found)) {
        setErrors(found);
        setMessage({ tone: "alert", content: "입력값을 확인해 주세요. 표시된 항목을 고친 뒤 다시 저장하세요." });
        summaryRef.current?.focus();
        return;
      }
      setErrors({});
      setMessage(null);
      const body = toWorkBody(draft);
      try {
        if (props.mode === "create") {
          const { id } = await create.mutateAsync(body);
          setBaseline(draft);
          router.replace(`/admin/works/${id}/edit?created=1`);
          return;
        }
        await update.mutateAsync(body);
        try {
          const fresh = await queryClient.fetchQuery({
            queryKey: adminKeys.work(props.detail.id),
            queryFn: () => fetchWork(props.detail.id),
            staleTime: 0,
          });
          const next = draftFromDetail(fresh);
          setBaseline(next);
          setDraft(next);
          setMessage({ tone: "status", content: "저장했습니다." });
        } catch {
          setBaseline(draft);
          setMessage({ tone: "status", content: "저장했습니다. 최신 내용을 다시 불러오지 못했으니 새로고침해 확인해 주세요." });
        }
      } catch (caught) {
        showFailure(caught);
      }
    });
  };

  const confirmDelete = () =>
    void lock(async () => {
      if (props.mode !== "edit") return;
      setDeleteError(null);
      try {
        await remove.mutateAsync(props.detail.id);
        setBaseline(draft);
        router.replace("/admin/works");
      } catch (caught) {
        const described = describeError(caught);
        if (described.scope === "auth") openReauth();
        setDeleteError(described.message);
      }
    });

  const categoryOptions = categories.data ?? [];
  const missingCategory = draft.categoryId !== null && !categoryOptions.some((category) => category.id === draft.categoryId);

  return (
    <section className="grid gap-8">
      <PageHeader
        title={props.mode === "create" ? "새 작업물" : "작업물 편집"}
        description={
          props.mode === "create"
            ? "제목과 슬러그만 있으면 저장할 수 있습니다. 새 작업물은 비공개로 시작합니다."
            : `생성 ${props.detail.createdAt.slice(0, 10)}`
        }
      />

      <div ref={summaryRef} tabIndex={-1} className="outline-none focus-visible:ring-2 focus-visible:ring-ring/40">
        {message ? (
          <Notice tone={message.tone}>
            {message.content}
            {message.tone === "status" && props.mode === "edit" && savedExposure !== "eligible" && !dirty ? (
              <span className="mt-1 block text-muted-foreground">
                지금은 서버 공개 조건을 충족하지 않습니다. 공개 조회에 포함하려면 공개 설정과 공개 카테고리가 모두 필요합니다.
              </span>
            ) : null}
          </Notice>
        ) : null}
      </div>

      <form onSubmit={submit} noValidate className="grid gap-12 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <div className="grid content-start gap-8">
          <TextField
            id={`${formId}-title`}
            label="제목"
            required
            value={draft.title}
            error={errors.title}
            errorId={`${formId}-title-error`}
            onChange={(title) => edit((current) => ({ ...current, title }))}
          />
          <div className="grid gap-2">
            <TextField
              id={`${formId}-slug`}
              label="슬러그"
              required
              value={draft.slug}
              error={errors.slug}
              errorId={`${formId}-slug-error`}
              hint="작업물 주소에 쓰이는 고유 값입니다. 입력한 그대로 저장합니다."
              spellCheck={false}
              onChange={(slug) => edit((current) => ({ ...current, slug }))}
            />
            {props.mode === "edit" && draft.slug !== baseline.slug ? (
              <p className="text-xs text-foreground">슬러그를 바꾸면 기존 주소가 바뀌고, 이전 주소에서 자동으로 이동하지 않습니다.</p>
            ) : null}
          </div>
          <TextField
            id={`${formId}-summary`}
            label="요약"
            value={draft.summary ?? ""}
            error={errors.summary}
            errorId={`${formId}-summary-error`}
            hint="선택 입력, 255자 이하"
            onChange={(summary) => edit((current) => ({ ...current, summary }))}
          />
          <div className="grid gap-2">
            <Label htmlFor={`${formId}-description`}>상세 설명</Label>
            <Textarea
              id={`${formId}-description`}
              value={draft.description ?? ""}
              onChange={(event) => edit((current) => ({ ...current, description: event.target.value }))}
              className="min-h-40"
              {...fieldProps("description")}
            />
            <p className="text-xs text-muted-foreground">일반 텍스트로 저장하며 줄바꿈을 그대로 유지합니다.</p>
            <FieldError id={`${formId}-description-error`} message={errors.description} />
          </div>

          <fieldset className="grid gap-4 border-t pt-8" aria-describedby={errors.mediaItems ? `${formId}-mediaItems-error` : undefined}>
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <legend className="font-heading text-xl">이미지 구성</legend>
                <p className="mt-1 text-xs text-muted-foreground">
                  대표 이미지는 하나만 지정할 수 있습니다. 연결 해제는 이 작업물에서만 빠지며 라이브러리 파일은 지우지 않습니다.
                </p>
              </div>
              <Button ref={addMediaRef} type="button" variant="outline" size="sm" onClick={() => setPickerOpen(true)}>
                이미지 추가
              </Button>
            </div>
            <FieldError id={`${formId}-mediaItems-error`} message={errors.mediaItems} />
            <WorkMediaEditor
              items={draft.mediaItems}
              altTextErrors={errors.altText}
              onRole={(key, role) => edit((current) => setRole(current, key, role))}
              onAltText={(key, value) => edit((current) => setAltText(current, key, value))}
              onMove={(key, direction) => edit((current) => move(current, key, direction))}
              onRemove={(key) => edit((current) => removeItem(current, key))}
            />
          </fieldset>
        </div>

        <aside className="grid content-start gap-8 lg:sticky lg:top-8 lg:self-start">
          <fieldset className="grid gap-3" {...fieldProps("visibility")}>
            <legend className="mb-3 text-xs font-semibold tracking-wide uppercase">공개 설정</legend>
            {(["HIDDEN", "VISIBLE"] as WorkVisibility[]).map((value) => (
              <label key={value} className="flex min-h-6 items-center gap-3 text-sm">
                <input
                  type="radio"
                  name={`${formId}-visibility`}
                  value={value}
                  checked={draft.visibility === value}
                  onChange={() => edit((current) => ({ ...current, visibility: value }))}
                  className="size-4 accent-foreground"
                />
                {value === "VISIBLE" ? "공개" : "비공개"}
              </label>
            ))}
            <FieldError id={`${formId}-visibility-error`} message={errors.visibility} />
            <div className="mt-2 grid gap-1 border-l-2 pl-3">
              <span className="text-xs text-muted-foreground">{dirty ? "저장하면 이 상태가 됩니다" : "저장된 상태"}</span>
              <ExposureBadge exposure={exposure} />
            </div>
          </fieldset>

          <div className="grid gap-2">
            <Label htmlFor={`${formId}-category`}>카테고리</Label>
            <select
              id={`${formId}-category`}
              value={draft.categoryId ?? ""}
              onChange={(event) =>
                edit((current) => ({ ...current, categoryId: event.target.value === "" ? null : Number(event.target.value) }))
              }
              className="h-10 border-b border-input bg-background text-sm text-foreground outline-none focus-visible:border-ring aria-invalid:border-destructive"
              {...fieldProps("categoryId")}
            >
              <option value="">미분류</option>
              {categoryOptions.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.visible ? category.name : `${category.name} (비공개)`}
                </option>
              ))}
              {missingCategory ? (
                <option value={draft.categoryId ?? ""}>
                  {initialCategory?.id === draft.categoryId ? initialCategory.name : `ID ${draft.categoryId}`} (목록에서 확인 안 됨)
                </option>
              ) : null}
            </select>
            {categories.isError ? (
              <p className="text-xs text-muted-foreground">카테고리 목록을 불러오지 못해 공개 가능 여부를 확인할 수 없습니다.</p>
            ) : null}
            <FieldError id={`${formId}-categoryId-error`} message={errors.categoryId} />
          </div>

          <div className="grid gap-3 border-t pt-6">
            <Button type="submit" disabled={saving}>
              {saving ? "저장 중…" : props.mode === "create" ? "작업물 만들기" : "변경 저장"}
            </Button>
            <Link
              href="/admin/works"
              className={buttonVariants({ variant: "ghost" })}
              onClick={(event) => !confirmLeave() && event.preventDefault()}
            >
              목록으로
            </Link>
            {dirty ? <p className="text-xs text-muted-foreground">저장하지 않은 변경이 있습니다.</p> : null}
          </div>

          {props.mode === "edit" ? (
            <div className="grid gap-2 border-t pt-6">
              <Button
                type="button"
                variant="outline"
                className="border-destructive/40 text-destructive hover:bg-destructive/10 hover:text-destructive"
                onClick={() => {
                  setDeleteError(null);
                  setDeleteOpen(true);
                }}
              >
                작업물 삭제
              </Button>
            </div>
          ) : null}
        </aside>
      </form>

      {pickerOpen ? (
        <MediaPicker
          open
          onOpenChange={(open) => {
            setPickerOpen(open);
            // 닫히면 대화상자가 언마운트되어 Radix 복귀가 없으므로 여는 버튼으로 직접 복귀
            if (!open) requestAnimationFrame(() => addMediaRef.current?.focus());
          }}
          onPick={(media: Media) => edit((current) => addMedia(current, media))}
        />
      ) : null}

      {props.mode === "edit" ? (
        <ConfirmDialog
          open={deleteOpen}
          onOpenChange={setDeleteOpen}
          title="작업물을 삭제할까요?"
          description={
            <>
              <p>
                <strong className="text-foreground">{baseline.title}</strong>을 삭제합니다. 삭제한 작업물은 복구할 수 없습니다.
              </p>
              <p>연결했던 이미지는 미디어 라이브러리에 남습니다.</p>
            </>
          }
          confirmLabel="삭제"
          pendingLabel="삭제 중…"
          pending={remove.isPending}
          destructive
          error={deleteError}
          onConfirm={confirmDelete}
        />
      ) : null}
    </section>
  );
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return message ? (
    <p id={id} className="text-xs text-destructive">
      {message}
    </p>
  ) : null;
}

type TextFieldProps = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  errorId: string;
  hint?: string;
  required?: boolean;
  spellCheck?: boolean;
};

function TextField({ id, label, value, onChange, error, errorId, hint, required, spellCheck }: TextFieldProps) {
  const hintId = `${id}-hint`;
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(" ") || undefined;
  return (
    <div className="grid gap-2">
      <Label htmlFor={id}>
        {label}
        {required ? <span className="text-muted-foreground normal-case">(필수)</span> : null}
      </Label>
      <Input
        id={id}
        value={value}
        required={required}
        autoComplete="off"
        spellCheck={spellCheck}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
      />
      {hint ? (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      ) : null}
      <FieldError id={errorId} message={error} />
    </div>
  );
}
