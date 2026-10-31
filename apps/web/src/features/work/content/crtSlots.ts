import type { WorkCategoryId } from "./categories";

export type CrtSlotTarget = { kind: "WORK_CATEGORY"; categoryId: WorkCategoryId } | { kind: "UNASSIGNED" };

/** S11·S22 CRT 의미 목적지, 후속 3D 인터랙션용 예약, UI 연결 없음 */
export const CRT_SLOTS: readonly { id: string; target: CrtSlotTarget }[] = [
  { id: "CH01_CRT_SLOT_01", target: { kind: "WORK_CATEGORY", categoryId: "WORK_CAT_01_BRANDING" } },
  { id: "CH01_CRT_SLOT_02", target: { kind: "WORK_CATEGORY", categoryId: "WORK_CAT_02_PACKAGING_DESIGN" } },
  { id: "CH01_CRT_SLOT_03", target: { kind: "WORK_CATEGORY", categoryId: "WORK_CAT_03_PRODUCT_DETAIL_PAGE_DESIGN" } },
  { id: "CH01_CRT_SLOT_04", target: { kind: "WORK_CATEGORY", categoryId: "WORK_CAT_04_PRESENTATION_DESIGN_PPT" } },
  { id: "CH01_CRT_SLOT_05", target: { kind: "WORK_CATEGORY", categoryId: "WORK_CAT_05_MARKETING_DESIGN" } },
  { id: "CH01_CRT_SLOT_06", target: { kind: "WORK_CATEGORY", categoryId: "WORK_CAT_06_EDITORIAL_DESIGN" } },
  { id: "CH02_CRT_SLOT_01", target: { kind: "UNASSIGNED" } },
];
