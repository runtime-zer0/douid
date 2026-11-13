import type { Category, CategoryBody } from "../api/types";

/** 순서는 입력 문자열로 보관하고 전송 시 정수로 변환 */
export type CategoryDraft = { name: string; slug: string; displayOrder: string; visible: boolean };
export type CategoryDraftErrors = Partial<Record<keyof CategoryDraft, string>>;

const MAX_TEXT = 100;
const INTEGER = /^-?\d+$/;

/** 새 카테고리 초기값 */
export function emptyCategoryDraft(): CategoryDraft {
  return { name: "", slug: "", displayOrder: "0", visible: true };
}

/** 최신 목록 항목으로 편집 초기값 생성 */
export function categoryDraftFrom(category: Category): CategoryDraft {
  return {
    name: category.name,
    slug: category.slug,
    displayOrder: String(category.displayOrder),
    visible: category.visible,
  };
}

/** 전송 본문, 서버 기본값에 기대지 않도록 네 필드 모두 포함 */
export function toCategoryBody(draft: CategoryDraft): CategoryBody {
  return { name: draft.name, slug: draft.slug, displayOrder: Number(draft.displayOrder), visible: draft.visible };
}

/** 사전 검증, 원본 값은 바꾸지 않음 */
export function validateCategoryDraft(draft: CategoryDraft): CategoryDraftErrors {
  const errors: CategoryDraftErrors = {};
  if (draft.name.trim().length === 0) errors.name = "이름을 입력해 주세요.";
  else if (draft.name.length > MAX_TEXT) errors.name = `이름은 ${MAX_TEXT}자 이하로 입력해 주세요.`;
  if (draft.slug.trim().length === 0) errors.slug = "슬러그를 입력해 주세요.";
  else if (draft.slug.length > MAX_TEXT) errors.slug = `슬러그는 ${MAX_TEXT}자 이하로 입력해 주세요.`;

  const order = Number(draft.displayOrder);
  if (!INTEGER.test(draft.displayOrder) || order < -(2 ** 31) || order > 2 ** 31 - 1) {
    errors.displayOrder = "순서는 -2147483648 ~ 2147483647 사이의 정수로 입력해 주세요.";
  }
  return errors;
}

/** 공개에서 비공개로 바꿀 때만 확인 필요 */
export function needsHideConfirm(baseline: CategoryDraft | null, draft: CategoryDraft) {
  return baseline?.visible === true && !draft.visible;
}
