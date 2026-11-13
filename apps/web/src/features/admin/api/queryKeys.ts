/** 관리자 쿼리 키, 모두 `["admin"]` 접두어라 로그아웃 시 한 번에 제거 */
export const adminKeys = {
  all: ["admin"] as const,
  me: ["admin", "me"] as const,
  categories: ["admin", "categories"] as const,
  worksAll: ["admin", "works"] as const,
  works: (page: number) => ["admin", "works", { page }] as const,
  work: (id: number) => ["admin", "works", id] as const,
  mediaAll: ["admin", "media"] as const,
  media: (page: number) => ["admin", "media", { page }] as const,
};
