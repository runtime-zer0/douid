import type { WorkCategoryId } from "./categories";

export type ProjectMedia = {
  /** `/work/<projectId>/...` 로컬 경로 */
  src: string;
  width: number;
  height: number;
  alt: string;
  /** 권리 확인된 자료만 데이터에 넣음 */
  rightsCleared: true;
};

export type PortfolioProject = {
  /** 안정 프로젝트 ID, 소문자 kebab-case, URL 세그먼트 */
  id: string;
  title: string;
  categoryIds: WorkCategoryId[];
  status: "published" | "draft";
  description: string | null;
  media: ProjectMedia[];
};

// 승인 자료만, 템플릿 검증용 데이터는 테스트 fixture에만 둠
export const PROJECTS: PortfolioProject[] = [];
