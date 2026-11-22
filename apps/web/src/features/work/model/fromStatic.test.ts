import { describe, expect, it } from "vitest";

import { WORK_CATEGORIES } from "../content/categories";
import type { PortfolioProject } from "../content/projects";
import { type StaticCatalog, staticCategories, staticWorkDetail, staticWorkSummaries } from "./fromStatic";

// 템플릿 검증 전용 fixture, 배포 데이터 아님
const cover = { src: "/work/alpha/cover.jpg", width: 1600, height: 900, alt: "alpha cover", rightsCleared: true } as const;
const projects: PortfolioProject[] = [
  {
    id: "alpha",
    title: "Alpha",
    categoryId: "WORK_CAT_01_BRANDING",
    summary: "alpha summary",
    status: "published",
    description: "alpha description",
    media: [cover, { ...cover, src: "/work/alpha/2.jpg", alt: "alpha second" }],
  },
  {
    id: "hidden",
    title: "Hidden",
    categoryId: "WORK_CAT_01_BRANDING",
    summary: null,
    status: "draft",
    description: null,
    media: [],
  },
  {
    id: "beta",
    title: "Beta",
    categoryId: "WORK_CAT_02_PACKAGING_DESIGN",
    summary: null,
    status: "published",
    description: null,
    media: [],
  },
];
const catalog: StaticCatalog = { categories: WORK_CATEGORIES, projects };

describe("staticCategories", () => {
  it("order 순서로 slug·name·summary만 변환", () => {
    const shuffled: StaticCatalog = { categories: [...WORK_CATEGORIES].reverse(), projects };
    const result = staticCategories(shuffled);

    expect(result.map((category) => category.slug)).toEqual(WORK_CATEGORIES.map((category) => category.slug));
    expect(result[0]).toEqual({ slug: "branding", name: "Branding", summary: null });
  });
});

describe("staticWorkSummaries", () => {
  it("공개 작업물만 데이터 순서대로, 첫 미디어를 대표 이미지로", () => {
    expect(staticWorkSummaries(undefined, catalog)).toEqual([
      {
        slug: "alpha",
        title: "Alpha",
        summary: "alpha summary",
        category: { slug: "branding", name: "Branding" },
        thumbnail: { src: cover.src, alt: "alpha cover", width: 1600, height: 900 },
      },
      {
        slug: "beta",
        title: "Beta",
        summary: null,
        category: { slug: "packaging-design", name: "Packaging Design" },
        thumbnail: null,
      },
    ]);
  });

  it("카테고리 slug로 필터", () => {
    expect(staticWorkSummaries("packaging-design", catalog).map((work) => work.slug)).toEqual(["beta"]);
    expect(staticWorkSummaries("editorial-design", catalog)).toEqual([]);
  });
});

describe("staticWorkDetail", () => {
  it("공개 작업물 상세는 미디어 순서 유지", () => {
    const detail = staticWorkDetail("alpha", catalog);

    expect(detail?.description).toBe("alpha description");
    expect(detail?.images.map((image) => image.alt)).toEqual(["alpha cover", "alpha second"]);
  });

  it("없거나 비공개인 slug는 null", () => {
    expect(staticWorkDetail("unknown", catalog)).toBeNull();
    expect(staticWorkDetail("hidden", catalog)).toBeNull();
  });
});
