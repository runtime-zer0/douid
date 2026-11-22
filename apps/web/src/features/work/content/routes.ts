/** 카테고리 정본 경로, CRT `WORK_CATEGORY(categorySlug)` 목적지 */
export function categoryHref(slug: string): string {
  return `/work/${encodeURIComponent(slug)}`;
}

/** 프로젝트 정본 경로, 어느 카테고리에서 들어와도 같은 URL */
export function projectHref(slug: string): string {
  return `/work/projects/${encodeURIComponent(slug)}`;
}
