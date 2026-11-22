export type CrtSlotTarget = { kind: "WORK_CATEGORY"; categorySlug: string } | { kind: "UNASSIGNED" };

/** S11·S22 CRT 의미 목적지, 후속 3D 인터랙션용 예약, UI 연결 없음, 현재 모드의 공개 카테고리에 slug가 없으면 비활성 */
export const CRT_SLOTS: readonly { id: string; target: CrtSlotTarget }[] = [
  { id: "CH01_CRT_SLOT_01", target: { kind: "WORK_CATEGORY", categorySlug: "branding" } },
  { id: "CH01_CRT_SLOT_02", target: { kind: "WORK_CATEGORY", categorySlug: "packaging-design" } },
  { id: "CH01_CRT_SLOT_03", target: { kind: "WORK_CATEGORY", categorySlug: "product-detail-page-design" } },
  { id: "CH01_CRT_SLOT_04", target: { kind: "WORK_CATEGORY", categorySlug: "presentation-design" } },
  { id: "CH01_CRT_SLOT_05", target: { kind: "WORK_CATEGORY", categorySlug: "marketing-design" } },
  { id: "CH01_CRT_SLOT_06", target: { kind: "WORK_CATEGORY", categorySlug: "editorial-design" } },
  { id: "CH02_CRT_SLOT_01", target: { kind: "UNASSIGNED" } },
];
