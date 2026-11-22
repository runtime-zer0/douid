import "server-only";

import { unstable_rethrow } from "next/navigation";
import { connection } from "next/server";

import { fetchAllPublicWorks, fetchPublicCategories, fetchPublicWork } from "./api/publicWorks.server";
import { staticCategories, staticWorkDetail, staticWorkSummaries } from "./model/fromStatic";
import type { PublicWorkDetail, PublicWorkSummary, WorkCategoryView } from "./model/types";

export { WorkSourceUnavailableError } from "./api/parsePublicWorks";

/**
 * 작업물 원천 모드, 요청 시점의 `API_ORIGIN`이 있으면 서버 공개 API, 없으면 정적 카탈로그
 * 두 원천은 섞지 않으며 서버 장애 시에도 정적 카탈로그로 대체하지 않음
 */
export function isServerMode(): boolean {
  return Boolean(process.env.API_ORIGIN?.trim());
}

/** 공개 카테고리, 표시 순서 */
export async function getCategories(): Promise<WorkCategoryView[]> {
  return isServerMode() ? fetchPublicCategories() : staticCategories();
}

/** slug로 공개 카테고리 조회, 없거나 비공개면 null */
export async function getCategory(slug: string): Promise<WorkCategoryView | null> {
  return (await getCategories()).find((category) => category.slug === slug) ?? null;
}

/** 공개 작업물 요약, 카테고리 slug 지정 시 해당 카테고리만, 원천 순서 유지 */
export async function getWorks(categorySlug?: string): Promise<PublicWorkSummary[]> {
  if (!isServerMode()) return staticWorkSummaries(categorySlug);
  const works = await fetchAllPublicWorks();
  return categorySlug ? works.filter((work) => work.category.slug === categorySlug) : works;
}

/** 공개 작업물 상세, 없거나 비공개면 null, 서버 모드는 공개 목록에 있는 slug만 */
export async function getWork(slug: string): Promise<PublicWorkDetail | null> {
  if (!isServerMode()) return staticWorkDetail(slug);
  // 데이터 캐시는 재검증 응답 404를 저장하지 않아 비공개 전환 후에도 이전 상세가 남으므로 갱신되는 목록으로 대조
  const [works, detail] = await Promise.all([fetchAllPublicWorks(), fetchPublicWork(slug)]);
  return works.some((work) => work.slug === slug) ? detail : null;
}

/** 챗봇 추천 카드 대조 목록 `{ id: slug, title }`, 조회 실패 시 빈 배열로 채팅에 영향 없음 */
export async function getRecommendationCatalog(): Promise<{ id: string; title: string }[]> {
  if (isServerMode()) await connection();
  try {
    return (await getWorks()).map(({ slug, title }) => ({ id: slug, title }));
  } catch (error) {
    // Next 내부 제어 흐름(동적 렌더링 전환 등)은 삼키지 않음
    unstable_rethrow(error);
    console.warn("[work] 추천 카드 대조 목록 조회 실패", error);
    return [];
  }
}
