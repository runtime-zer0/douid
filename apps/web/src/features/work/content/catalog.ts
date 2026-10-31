import type { WorkCategory } from "./categories";
import type { PortfolioProject } from "./projects";

const PROJECT_ID = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

/** 카테고리·프로젝트 정적 데이터 검증, 오류 메시지 목록 반환, 빈 배열이면 유효 */
export function validateCatalog(categories: readonly WorkCategory[], projects: readonly PortfolioProject[]): string[] {
  const errors: string[] = [];
  if (categories.length !== 6) errors.push(`카테고리는 6개여야 함: ${categories.length}`);
  for (const key of ["id", "order", "slug"] as const) {
    if (new Set(categories.map((category) => category[key])).size !== categories.length) errors.push(`카테고리 ${key} 중복`);
  }

  const categoryIds = new Set<string>(categories.map((category) => category.id));
  const seen = new Set<string>();
  for (const project of projects) {
    if (seen.has(project.id)) errors.push(`중복 프로젝트 ID: ${project.id}`);
    seen.add(project.id);
    if (!PROJECT_ID.test(project.id)) errors.push(`프로젝트 ID 형식 오류: ${project.id}`);
    if (project.categoryIds.length === 0) errors.push(`${project.id}: 카테고리 없음`);
    for (const categoryId of project.categoryIds) {
      if (!categoryIds.has(categoryId)) errors.push(`${project.id}: 존재하지 않는 카테고리 ${categoryId}`);
    }
    if (project.status !== "published") continue;
    if (project.media.length === 0) errors.push(`${project.id}: 공개 프로젝트에 미디어 없음`);
    for (const media of project.media) {
      if (!media.alt.trim()) errors.push(`${project.id}: 대체 텍스트 없는 미디어 ${media.src}`);
      if (media.rightsCleared !== true) errors.push(`${project.id}: 권리 미확인 미디어 ${media.src}`);
    }
  }
  return errors;
}
