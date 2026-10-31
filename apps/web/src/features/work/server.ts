import "server-only";

import { WORK_CATEGORIES, type WorkCategory, type WorkCategoryId } from "./content/categories";
import { type PortfolioProject, PROJECTS } from "./content/projects";

export { categoryHref, projectHref } from "./content/routes";
export type { PortfolioProject, ProjectMedia } from "./content/projects";
export type { WorkCategory, WorkCategoryId } from "./content/categories";

/** 승인 카테고리 6개, 표시 순서 */
export function getCategories(): readonly WorkCategory[] {
  return WORK_CATEGORIES;
}

/** slug로 카테고리 조회, 없으면 null */
export function getCategoryBySlug(slug: string): WorkCategory | null {
  return WORK_CATEGORIES.find((category) => category.slug === slug) ?? null;
}

/** 공개 프로젝트 목록, 카테고리 지정 시 해당 카테고리 소속만, 데이터 순서 유지 */
export function getPublishedProjects(categoryId?: WorkCategoryId): PortfolioProject[] {
  return PROJECTS.filter(
    (project) => project.status === "published" && (!categoryId || project.categoryIds.includes(categoryId)),
  );
}

/** 공개 프로젝트 단건 조회, 없거나 비공개면 null */
export function getPublishedProject(id: string): PortfolioProject | null {
  return getPublishedProjects().find((project) => project.id === id) ?? null;
}

/** 프로젝트 소속 카테고리 목록, 카테고리 표시 순서 */
export function getProjectCategories(project: PortfolioProject): WorkCategory[] {
  return WORK_CATEGORIES.filter((category) => project.categoryIds.includes(category.id));
}
