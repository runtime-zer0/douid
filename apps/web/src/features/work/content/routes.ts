import { WORK_CATEGORIES, type WorkCategoryId } from "./categories";

/** 카테고리 정본 경로, CRT `WORK_CATEGORY(id)` 목적지 */
export function categoryHref(id: WorkCategoryId): string {
  const category = WORK_CATEGORIES.find((item) => item.id === id);
  if (!category) throw new Error(`Unknown work category: ${id}`);
  return `/work/${category.slug}`;
}

/** 프로젝트 정본 경로, 어느 카테고리에서 들어와도 같은 URL */
export function projectHref(id: string): string {
  return `/work/projects/${id}`;
}
