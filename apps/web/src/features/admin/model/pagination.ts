/** 관리자 목록 요청 크기 */
export const PAGE_SIZE = 20;

/** 화면 쿼리 `?page=`를 1 기반 페이지로 해석, 잘못된 값은 1 */
export function parseUiPage(value: string | null | undefined) {
  const page = Number(value);
  return Number.isInteger(page) && page >= 1 ? page : 1;
}

/** 1 기반 화면 페이지를 0 기반 API 페이지로 변환 */
export function toApiPage(uiPage: number) {
  return uiPage - 1;
}

/** 전체 페이지 수, 0건이어도 1 */
export function totalPages(totalElements: number, size: number) {
  return Math.max(1, Math.ceil(totalElements / size));
}

/** 범위 밖 페이지면 마지막 유효 화면 페이지, 아니면 null */
export function fallbackPage(page: { content: unknown[]; totalElements: number; size: number }) {
  return page.content.length === 0 && page.totalElements > 0 ? totalPages(page.totalElements, page.size) : null;
}
