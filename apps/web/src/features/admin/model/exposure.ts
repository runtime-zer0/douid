import type { Category, WorkVisibility } from "../api/types";

export type Exposure = "hidden" | "uncategorized" | "categoryHidden" | "eligible" | "unknown";

/**
 * 서버 공개 조회 조건 판정: 작업물 VISIBLE AND 카테고리 존재 AND 카테고리 visible
 * 작업물 응답에는 카테고리 공개 여부가 없어 관리자 카테고리 목록과 ID로 대조
 */
export function publicExposure(
  work: { visibility: WorkVisibility; categoryId: number | null },
  categories: Category[] | undefined,
): Exposure {
  if (work.visibility === "HIDDEN") return "hidden";
  if (work.categoryId === null) return "uncategorized";
  const category = categories?.find((item) => item.id === work.categoryId);
  if (!category) return "unknown";
  return category.visible ? "eligible" : "categoryHidden";
}
