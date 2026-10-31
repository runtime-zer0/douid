import { describe, expect, it } from "vitest";

import { validateCatalog } from "./catalog";
import { WORK_CATEGORIES } from "./categories";
import { CRT_SLOTS } from "./crtSlots";
import { type PortfolioProject, PROJECTS } from "./projects";

// 템플릿 검증 전용 fixture, 배포 데이터 아님
const validProject: PortfolioProject = {
  id: "fixture-project",
  title: "Fixture Project",
  categoryIds: ["WORK_CAT_01_BRANDING"],
  status: "published",
  description: null,
  media: [{ src: "/work/fixture-project/cover.jpg", width: 1600, height: 900, alt: "fixture cover", rightsCleared: true }],
};

describe("WORK 카테고리", () => {
  it("승인된 6개 카테고리의 순서·안정 ID·정식 명칭·slug가 정확히 일치", () => {
    expect(WORK_CATEGORIES.map(({ order, id, name, slug }) => [order, id, name, slug])).toEqual([
      [1, "WORK_CAT_01_BRANDING", "Branding", "branding"],
      [2, "WORK_CAT_02_PACKAGING_DESIGN", "Packaging Design", "packaging-design"],
      [3, "WORK_CAT_03_PRODUCT_DETAIL_PAGE_DESIGN", "Product Detail Page Design", "product-detail-page-design"],
      [4, "WORK_CAT_04_PRESENTATION_DESIGN_PPT", "Presentation Design (PPT)", "presentation-design"],
      [5, "WORK_CAT_05_MARKETING_DESIGN", "Marketing Design", "marketing-design"],
      [6, "WORK_CAT_06_EDITORIAL_DESIGN", "Editorial Design", "editorial-design"],
    ]);
  });

  it("slug는 고유하고 프로젝트 상세 경로 세그먼트와 겹치지 않음", () => {
    const slugs = WORK_CATEGORIES.map((category) => category.slug);
    expect(new Set(slugs).size).toBe(6);
    expect(slugs).not.toContain("projects");
  });

  it("승인 문구가 없는 작업 범위 설명은 비워 둠", () => {
    WORK_CATEGORIES.forEach((category) => expect(category.summary).toBeNull());
  });
});

describe("validateCatalog", () => {
  it("배포 데이터는 유효", () => {
    expect(validateCatalog(WORK_CATEGORIES, PROJECTS)).toEqual([]);
  });

  it("공개 프로젝트가 없어도 유효", () => {
    expect(validateCatalog(WORK_CATEGORIES, [])).toEqual([]);
  });

  it("권리 확인 미디어와 대체 텍스트가 있는 공개 프로젝트는 유효", () => {
    expect(validateCatalog(WORK_CATEGORIES, [validProject])).toEqual([]);
  });

  it("중복 프로젝트 ID 거부", () => {
    expect(validateCatalog(WORK_CATEGORIES, [validProject, validProject])).toEqual([
      "중복 프로젝트 ID: fixture-project",
    ]);
  });

  it("kebab-case가 아닌 프로젝트 ID 거부", () => {
    expect(validateCatalog(WORK_CATEGORIES, [{ ...validProject, id: "Fixture_Project" }])).toEqual([
      "프로젝트 ID 형식 오류: Fixture_Project",
    ]);
  });

  it("존재하지 않는 카테고리 참조 거부", () => {
    const project = { ...validProject, categoryIds: ["WORK_CAT_99_UNKNOWN"] } as unknown as PortfolioProject;
    expect(validateCatalog(WORK_CATEGORIES, [project])).toEqual([
      "fixture-project: 존재하지 않는 카테고리 WORK_CAT_99_UNKNOWN",
    ]);
  });

  it("카테고리 없는 프로젝트 거부", () => {
    expect(validateCatalog(WORK_CATEGORIES, [{ ...validProject, categoryIds: [] }])).toEqual([
      "fixture-project: 카테고리 없음",
    ]);
  });

  it("미디어 없는 공개 프로젝트 거부, 비공개는 허용", () => {
    expect(validateCatalog(WORK_CATEGORIES, [{ ...validProject, media: [] }])).toEqual([
      "fixture-project: 공개 프로젝트에 미디어 없음",
    ]);
    expect(validateCatalog(WORK_CATEGORIES, [{ ...validProject, status: "draft", media: [] }])).toEqual([]);
  });

  it("대체 텍스트가 빈 미디어 거부", () => {
    const media = [{ ...validProject.media[0], alt: " " }];
    expect(validateCatalog(WORK_CATEGORIES, [{ ...validProject, media }])).toEqual([
      "fixture-project: 대체 텍스트 없는 미디어 /work/fixture-project/cover.jpg",
    ]);
  });

  it("권리 확인되지 않은 미디어 거부", () => {
    const media = [{ ...validProject.media[0], rightsCleared: false }] as unknown as PortfolioProject["media"];
    expect(validateCatalog(WORK_CATEGORIES, [{ ...validProject, media }])).toEqual([
      "fixture-project: 권리 미확인 미디어 /work/fixture-project/cover.jpg",
    ]);
  });

  it("카테고리 수가 6개가 아니면 거부", () => {
    expect(validateCatalog(WORK_CATEGORIES.slice(0, 5), [])).toEqual(["카테고리는 6개여야 함: 5"]);
  });
});

describe("CRT 슬롯 예약", () => {
  it("CH01 슬롯 6개는 카테고리 01~06에 순서대로 1:1 대응", () => {
    const ch01 = CRT_SLOTS.filter((slot) => slot.id.startsWith("CH01"));
    expect(ch01.map((slot) => [slot.id, slot.target])).toEqual(
      WORK_CATEGORIES.map((category, index) => [
        `CH01_CRT_SLOT_0${index + 1}`,
        { kind: "WORK_CATEGORY", categoryId: category.id },
      ]),
    );
  });

  it("CH02 프로젝트 슬롯은 배정되지 않음", () => {
    expect(CRT_SLOTS.find((slot) => slot.id === "CH02_CRT_SLOT_01")?.target).toEqual({ kind: "UNASSIGNED" });
    expect(CRT_SLOTS).toHaveLength(7);
  });
});
