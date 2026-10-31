export type WorkCategoryId =
  | "WORK_CAT_01_BRANDING"
  | "WORK_CAT_02_PACKAGING_DESIGN"
  | "WORK_CAT_03_PRODUCT_DETAIL_PAGE_DESIGN"
  | "WORK_CAT_04_PRESENTATION_DESIGN_PPT"
  | "WORK_CAT_05_MARKETING_DESIGN"
  | "WORK_CAT_06_EDITORIAL_DESIGN";

export type WorkCategory = {
  /** 안정 ID, 변경 금지 */
  id: WorkCategoryId;
  order: number;
  /** URL 세그먼트, `projects` 사용 금지 */
  slug: string;
  /** 승인 정식 명칭 */
  name: string;
  /** 작업 범위 설명, 승인 문구만, 없으면 null */
  summary: string | null;
};

/** 승인된 WORK 분류 6개, 배열 순서가 표시 순서 */
export const WORK_CATEGORIES: readonly WorkCategory[] = [
  { id: "WORK_CAT_01_BRANDING", order: 1, slug: "branding", name: "Branding", summary: null },
  { id: "WORK_CAT_02_PACKAGING_DESIGN", order: 2, slug: "packaging-design", name: "Packaging Design", summary: null },
  {
    id: "WORK_CAT_03_PRODUCT_DETAIL_PAGE_DESIGN",
    order: 3,
    slug: "product-detail-page-design",
    name: "Product Detail Page Design",
    summary: null,
  },
  {
    id: "WORK_CAT_04_PRESENTATION_DESIGN_PPT",
    order: 4,
    slug: "presentation-design",
    name: "Presentation Design (PPT)",
    summary: null,
  },
  { id: "WORK_CAT_05_MARKETING_DESIGN", order: 5, slug: "marketing-design", name: "Marketing Design", summary: null },
  { id: "WORK_CAT_06_EDITORIAL_DESIGN", order: 6, slug: "editorial-design", name: "Editorial Design", summary: null },
];
