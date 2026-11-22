import "server-only";

import { connection } from "next/server";

import type { PublicWorkDetail, PublicWorkSummary, WorkCategoryView } from "../model/types";
import { parseCategoryList, parseWorkDetail, parseWorkPage, WorkSourceUnavailableError } from "./parsePublicWorks";

/** 공개 화면 갱신 지연 상한(초), 갱신 실패 시 마지막 성공 데이터 유지 */
export const REVALIDATE_SECONDS = 60;
/** 요청당 타임아웃, 백엔드 지연이 공개 페이지를 붙잡지 않도록 */
export const REQUEST_TIMEOUT_MS = 3000;
const PAGE_SIZE = 100;
const MAX_PAGES = 10;

/** `${API_ORIGIN}` 공개 API GET, 404면 null, 그 밖의 실패는 `WorkSourceUnavailableError` */
async function fetchPublicJson(path: string): Promise<unknown | null> {
  // 빌드 시점 호출을 막고 요청 시점 렌더링으로 전환
  await connection();
  const origin = (process.env.API_ORIGIN ?? "").trim().replace(/\/+$/, "");

  let response: Response;
  try {
    response = await fetch(`${origin}${path}`, {
      next: { revalidate: REVALIDATE_SECONDS },
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch (cause) {
    throw new WorkSourceUnavailableError(`공개 API 요청 실패: ${path}`, { cause });
  }
  if (response.status === 404) return null;
  if (!response.ok) throw new WorkSourceUnavailableError(`공개 API 응답 ${response.status}: ${path}`);
  try {
    return await response.json();
  } catch (cause) {
    throw new WorkSourceUnavailableError(`공개 API 응답 해석 실패: ${path}`, { cause });
  }
}

async function fetchRequired(path: string): Promise<unknown> {
  const json = await fetchPublicJson(path);
  if (json === null) throw new WorkSourceUnavailableError(`공개 API 응답 404: ${path}`);
  return json;
}

/** 공개 카테고리 목록, 서버 표시 순서 */
export async function fetchPublicCategories(): Promise<WorkCategoryView[]> {
  return parseCategoryList(await fetchRequired("/api/public/categories"));
}

/** 공개 작업물 전체, `hasNext`가 false일 때까지 최대 10페이지, 서버 순서 유지, slug 중복은 처음 항목만 */
export async function fetchAllPublicWorks(): Promise<PublicWorkSummary[]> {
  const works: PublicWorkSummary[] = [];
  const seen = new Set<string>();
  for (let page = 0; page < MAX_PAGES; page += 1) {
    const { items, hasNext } = parseWorkPage(await fetchRequired(`/api/public/works?page=${page}&size=${PAGE_SIZE}`));
    for (const item of items) {
      if (seen.has(item.slug)) continue;
      seen.add(item.slug);
      works.push(item);
    }
    if (!hasNext) return works;
  }
  console.warn(`[work] 공개 작업물이 ${MAX_PAGES * PAGE_SIZE}건을 넘어 이후 페이지 생략`);
  return works;
}

/** 공개 작업물 상세, 미존재·비공개·카테고리 비공개(404)는 null */
export async function fetchPublicWork(slug: string): Promise<PublicWorkDetail | null> {
  const json = await fetchPublicJson(`/api/public/works/${encodeURIComponent(slug)}`);
  return json === null ? null : parseWorkDetail(json, slug);
}
