/** 화면에 표시하는 공개 카테고리, 배열 순서가 표시 순서 */
export type WorkCategoryView = {
  slug: string;
  name: string;
  /** 서버 모드는 항상 null */
  summary: string | null;
};

/** 화면에 표시하는 작업물 이미지, 서버 모드는 크기 정보 없음 */
export type WorkImage = {
  src: string;
  /** 비어 있지 않은 대체 텍스트 */
  alt: string;
  width: number | null;
  height: number | null;
};

/** 목록 카드와 추천 대조에 쓰는 공개 작업물 요약 */
export type PublicWorkSummary = {
  /** 정본 식별자, `/work/projects/<slug>` */
  slug: string;
  title: string;
  summary: string | null;
  category: { slug: string; name: string };
  thumbnail: WorkImage | null;
};

/** 공개 작업물 상세, 이미지는 표시 순서 */
export type PublicWorkDetail = Omit<PublicWorkSummary, "thumbnail"> & {
  description: string | null;
  images: WorkImage[];
};
