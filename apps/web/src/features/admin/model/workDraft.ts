import type { AdminWorkDetail, WorkBody, WorkMediaRole, WorkVisibility } from "../api/types";

export type DraftMediaItem = {
  /** 클라이언트 전용 식별자, 같은 mediaId가 여러 번 연결될 수 있어 mediaId로 식별하지 않음 */
  key: string;
  mediaId: number;
  role: WorkMediaRole;
  /** 서버 원래 값, 순서를 바꾸지 않으면 그대로 전송 */
  sortOrder: number;
  altText: string | null;
  /** 미리보기 전용, 전송하지 않음 */
  fileUrl: string;
};

export type WorkDraft = {
  title: string;
  slug: string;
  /** 서버 값 그대로 보관, 빈 문자열↔null 변환 없음 */
  summary: string | null;
  description: string | null;
  categoryId: number | null;
  visibility: WorkVisibility;
  mediaItems: DraftMediaItem[];
  orderChanged: boolean;
};

export type WorkDraftErrors = Partial<Record<"title" | "slug" | "summary" | "mediaItems", string>> & {
  altText?: Record<string, string>;
};

const MAX_TEXT = 255;
const INT32_MIN = -(2 ** 31);
const INT32_MAX = 2 ** 31 - 1;

const newKey = () => crypto.randomUUID();

/** 새 작업물 초기값, 비공개·미분류·이미지 없음 */
export function emptyDraft(): WorkDraft {
  return {
    title: "",
    slug: "",
    summary: null,
    description: null,
    categoryId: null,
    visibility: "HIDDEN",
    mediaItems: [],
    orderChanged: false,
  };
}

/** 관리자 상세 응답으로 편집 초기값 생성, 응답 순서 유지 */
export function draftFromDetail(detail: AdminWorkDetail): WorkDraft {
  return {
    title: detail.title,
    slug: detail.slug,
    summary: detail.summary ?? null,
    description: detail.description ?? null,
    categoryId: detail.category?.id ?? null,
    visibility: detail.visibility,
    mediaItems: detail.mediaItems.map((item) => ({
      key: newKey(),
      mediaId: item.mediaId,
      role: item.role,
      sortOrder: item.sortOrder,
      altText: item.altText ?? null,
      fileUrl: item.fileUrl,
    })),
    orderChanged: false,
  };
}

/** 전송 본문 생성, 순서를 바꿨을 때만 표시 순서대로 0..n-1 재배정 */
export function toWorkBody(draft: WorkDraft): WorkBody {
  return {
    title: draft.title,
    slug: draft.slug,
    summary: draft.summary,
    description: draft.description,
    categoryId: draft.categoryId,
    visibility: draft.visibility,
    mediaItems: draft.mediaItems.map((item, index) => ({
      mediaId: item.mediaId,
      role: item.role,
      sortOrder: draft.orderChanged ? index : item.sortOrder,
      altText: item.altText,
    })),
  };
}

/** 미저장 변경 여부, 전송 본문 기준 비교 */
export function isDirty(draft: WorkDraft, baseline: WorkDraft) {
  return JSON.stringify(toWorkBody(draft)) !== JSON.stringify(toWorkBody(baseline));
}

const isBlank = (value: string) => value.trim().length === 0;

/** 사전 검증, 원본 값은 바꾸지 않음 */
export function validateWorkDraft(draft: WorkDraft): WorkDraftErrors {
  const errors: WorkDraftErrors = {};
  if (isBlank(draft.title)) errors.title = "제목을 입력해 주세요.";
  else if (draft.title.length > MAX_TEXT) errors.title = `제목은 ${MAX_TEXT}자 이하로 입력해 주세요.`;
  if (isBlank(draft.slug)) errors.slug = "슬러그를 입력해 주세요.";
  else if (draft.slug.length > MAX_TEXT) errors.slug = `슬러그는 ${MAX_TEXT}자 이하로 입력해 주세요.`;
  if (draft.summary !== null && draft.summary.length > MAX_TEXT) {
    errors.summary = `요약은 ${MAX_TEXT}자 이하로 입력해 주세요.`;
  }

  const altText: Record<string, string> = {};
  for (const item of draft.mediaItems) {
    if (item.altText !== null && item.altText.length > MAX_TEXT) {
      altText[item.key] = `대체 텍스트는 ${MAX_TEXT}자 이하로 입력해 주세요.`;
    }
  }
  if (Object.keys(altText).length > 0) errors.altText = altText;

  if (draft.mediaItems.filter((item) => item.role === "THUMBNAIL").length > 1) {
    errors.mediaItems = "대표 이미지는 하나만 지정할 수 있습니다.";
  } else if (
    draft.mediaItems.some((item) => !Number.isInteger(item.sortOrder) || item.sortOrder < INT32_MIN || item.sortOrder > INT32_MAX)
  ) {
    errors.mediaItems = "이미지 순서 값이 허용 범위를 벗어났습니다. 순서를 다시 지정해 주세요.";
  }
  return errors;
}

export function hasErrors(errors: WorkDraftErrors) {
  return Object.keys(errors).length > 0;
}

/** 라이브러리 이미지를 끝에 상세 이미지로 연결, 같은 이미지 재연결 허용 */
export function addMedia(draft: WorkDraft, media: { id: number; url: string }): WorkDraft {
  const sortOrder = draft.mediaItems.length === 0 ? 0 : Math.max(...draft.mediaItems.map((item) => item.sortOrder)) + 1;
  return {
    ...draft,
    mediaItems: [
      ...draft.mediaItems,
      { key: newKey(), mediaId: media.id, role: "DETAIL_IMAGE", sortOrder, altText: null, fileUrl: media.url },
    ],
  };
}

/** 작업물 연결만 해제, 라이브러리 자산은 유지 */
export function removeItem(draft: WorkDraft, key: string): WorkDraft {
  return { ...draft, mediaItems: draft.mediaItems.filter((item) => item.key !== key) };
}

/** 역할 변경, 새 대표를 지정하면 기존 대표는 상세 이미지로 전환 */
export function setRole(draft: WorkDraft, key: string, role: WorkMediaRole): WorkDraft {
  return {
    ...draft,
    mediaItems: draft.mediaItems.map((item) => {
      if (item.key === key) return { ...item, role };
      if (role === "THUMBNAIL" && item.role === "THUMBNAIL") return { ...item, role: "DETAIL_IMAGE" };
      return item;
    }),
  };
}

/** 한 칸 이동, 양 끝에서는 변경 없음 */
export function move(draft: WorkDraft, key: string, direction: -1 | 1): WorkDraft {
  const index = draft.mediaItems.findIndex((item) => item.key === key);
  const target = index + direction;
  if (index < 0 || target < 0 || target >= draft.mediaItems.length) return draft;
  const mediaItems = [...draft.mediaItems];
  [mediaItems[index], mediaItems[target]] = [mediaItems[target], mediaItems[index]];
  return { ...draft, mediaItems, orderChanged: true };
}

/** 대체 텍스트 변경 */
export function setAltText(draft: WorkDraft, key: string, altText: string): WorkDraft {
  return {
    ...draft,
    mediaItems: draft.mediaItems.map((item) => (item.key === key ? { ...item, altText } : item)),
  };
}
