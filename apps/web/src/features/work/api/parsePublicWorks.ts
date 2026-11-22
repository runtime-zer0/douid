import type { PublicWorkDetail, PublicWorkSummary, WorkCategoryView, WorkImage } from "../model/types";

/** 서버 공개 API를 쓸 수 없는 상태(장애·타임아웃·형식 오류), 404와 구분 */
export class WorkSourceUnavailableError extends Error {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = "WorkSourceUnavailableError";
  }
}

type JsonRecord = Record<string, unknown>;

// `/work/projects/<slug>` 상세 경로와 겹치는 카테고리 slug
const RESERVED_CATEGORY_SLUG = "projects";

const isRecord = (value: unknown): value is JsonRecord =>
  typeof value === "object" && value !== null && !Array.isArray(value);
const isText = (value: unknown): value is string => typeof value === "string" && value.trim() !== "";
const isOptionalString = (value: unknown): value is string | null | undefined =>
  value === null || value === undefined || typeof value === "string";

function unavailable(reason: string): never {
  throw new WorkSourceUnavailableError(`공개 API 응답 형식 오류: ${reason}`);
}

/** `ApiResponse` 래퍼에서 SUCCESS의 `data`만 꺼냄 */
function unwrap(json: unknown): unknown {
  if (!isRecord(json) || json.status !== "SUCCESS") unavailable("status");
  return json.data;
}

function toImage(media: unknown, fallbackAlt: string): WorkImage | null {
  if (!isRecord(media) || !isText(media.fileUrl)) return null;
  const alt = isText(media.altText) ? media.altText : fallbackAlt;
  return { src: media.fileUrl, alt, width: null, height: null };
}

function toWorkBase(item: unknown): Omit<PublicWorkSummary, "thumbnail"> | null {
  if (!isRecord(item) || !isText(item.slug) || !isText(item.title) || !isOptionalString(item.summary)) return null;
  const { category } = item;
  if (!isRecord(category) || !isText(category.slug) || !isText(category.name)) return null;
  return {
    slug: item.slug,
    title: item.title,
    summary: item.summary ?? null,
    category: { slug: category.slug, name: category.name },
  };
}

/** `GET /api/public/categories` 응답 변환, 형식 오류 항목과 `projects` slug는 제외 */
export function parseCategoryList(json: unknown): WorkCategoryView[] {
  const data = unwrap(json);
  if (!Array.isArray(data)) unavailable("categories");
  return data.flatMap((item) => {
    if (!isRecord(item) || !isText(item.slug) || !isText(item.name)) return [];
    if (item.slug === RESERVED_CATEGORY_SLUG) {
      console.warn(`[work] 상세 경로와 겹치는 카테고리 slug 제외: ${item.slug}`);
      return [];
    }
    return [{ slug: item.slug, name: item.name, summary: null }];
  });
}

/** `GET /api/public/works` 한 페이지 변환, 형식 오류 항목은 제외하고 래퍼·페이지 형식 오류는 장애 */
export function parseWorkPage(json: unknown): { items: PublicWorkSummary[]; hasNext: boolean } {
  const data = unwrap(json);
  if (!isRecord(data) || !Array.isArray(data.content) || typeof data.hasNext !== "boolean") unavailable("page");
  const items = data.content.flatMap((item) => {
    const base = toWorkBase(item);
    if (!base) return [];
    return [{ ...base, thumbnail: toImage((item as JsonRecord).thumbnail, `${base.title} 이미지`) }];
  });
  return { items, hasNext: data.hasNext };
}

/** `GET /api/public/works/{slug}` 응답 변환, `ATTACHMENT` 제외, 요청 slug와 다르거나 형식 오류면 장애 */
export function parseWorkDetail(json: unknown, slug: string): PublicWorkDetail {
  const data = unwrap(json);
  const base = toWorkBase(data);
  if (!base || base.slug !== slug) unavailable("detail");
  const { description, mediaItems } = data as JsonRecord;
  if (!isOptionalString(description) || !Array.isArray(mediaItems)) unavailable("detail");

  const displayable = mediaItems.filter(
    (media) => isRecord(media) && media.role !== "ATTACHMENT" && isText(media.fileUrl),
  );
  const images = displayable.flatMap((media, index) => {
    const fallbackAlt = displayable.length > 1 ? `${base.title} 이미지 ${index + 1}` : `${base.title} 이미지`;
    const image = toImage(media, fallbackAlt);
    return image ? [image] : [];
  });
  return { ...base, description: description ?? null, images };
}
