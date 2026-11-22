import { WORK_CATEGORIES, type WorkCategory } from "../content/categories";
import { type PortfolioProject, type ProjectMedia, PROJECTS } from "../content/projects";
import type { PublicWorkDetail, PublicWorkSummary, WorkCategoryView, WorkImage } from "./types";

export type StaticCatalog = { categories: readonly WorkCategory[]; projects: readonly PortfolioProject[] };

const STATIC_CATALOG: StaticCatalog = { categories: WORK_CATEGORIES, projects: PROJECTS };

const toImage = ({ src, alt, width, height }: ProjectMedia): WorkImage => ({ src, alt, width, height });

/** 정적 공개 작업물을 카테고리와 함께 변환, 카테고리를 찾지 못한 항목은 제외 */
function publishedWithCategory(catalog: StaticCatalog) {
  return catalog.projects.flatMap((project) => {
    const category = catalog.categories.find((item) => item.id === project.categoryId);
    if (project.status !== "published" || !category) return [];
    return [{ project, category: { slug: category.slug, name: category.name } }];
  });
}

/** 정적 카테고리를 표시 순서(`order`)대로 공통 모델로 변환 */
export function staticCategories(catalog: StaticCatalog = STATIC_CATALOG): WorkCategoryView[] {
  return [...catalog.categories]
    .sort((a, b) => a.order - b.order)
    .map(({ slug, name, summary }) => ({ slug, name, summary }));
}

/** 정적 공개 작업물 요약, 카테고리 slug 지정 시 해당 카테고리만, 데이터 순서 유지, 대표 이미지는 첫 미디어 */
export function staticWorkSummaries(categorySlug?: string, catalog: StaticCatalog = STATIC_CATALOG): PublicWorkSummary[] {
  return publishedWithCategory(catalog)
    .filter(({ category }) => !categorySlug || category.slug === categorySlug)
    .map(({ project, category }) => ({
      slug: project.id,
      title: project.title,
      summary: project.summary,
      category,
      thumbnail: project.media[0] ? toImage(project.media[0]) : null,
    }));
}

/** 정적 공개 작업물 상세, 없거나 비공개면 null */
export function staticWorkDetail(slug: string, catalog: StaticCatalog = STATIC_CATALOG): PublicWorkDetail | null {
  const found = publishedWithCategory(catalog).find(({ project }) => project.id === slug);
  if (!found) return null;
  const { project, category } = found;
  return {
    slug: project.id,
    title: project.title,
    summary: project.summary,
    category,
    description: project.description,
    images: project.media.map(toImage),
  };
}
