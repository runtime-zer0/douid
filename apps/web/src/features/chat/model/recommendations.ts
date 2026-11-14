import type { PublicProject } from "./types";

export type RecommendationCard = { id: string; title: string; href: string };

/**
 * 서버 추천 ID와 공개 카탈로그 대조, 카탈로그에 있는 ID만 입력 순서대로 중복 없이
 * 링크는 기존 상세 경로 `/work/projects/<id>`만 사용(FR-031~033)
 */
export function matchRecommendations(ids: readonly (string | number)[], projects: readonly PublicProject[]): RecommendationCard[] {
  const catalog = new Map(projects.map((project) => [project.id, project]));
  return [...new Set(ids.map(String))].flatMap((id) => {
    const project = catalog.get(id);
    return project ? [{ id, title: project.title, href: `/work/projects/${encodeURIComponent(id)}` }] : [];
  });
}
