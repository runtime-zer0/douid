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
  /** 안정 프로젝트 ID, 소문자 kebab-case, URL 세그먼트, 운영 연동 시 CMS slug와 같은 값 */
  id: string;
  title: string;
  /** 서버 계약과 같이 카테고리 1개 */
  categoryId: WorkCategoryId;
  /** 목록 카드 요약, 없으면 null */
  summary: string | null;
  status: "published" | "draft";
  description: string | null;
  /** 첫 장이 대표 이미지 */
  media: ProjectMedia[];
};

// 정적 모드(API_ORIGIN 미설정) 원천, 승인 자료만, 템플릿 검증용 데이터는 테스트 fixture에만 둠
export const PROJECTS: PortfolioProject[] = [];
