import { afterEach, describe, expect, it, vi } from "vitest";

import { parseCategoryList, parseWorkDetail, parseWorkPage, WorkSourceUnavailableError } from "./parsePublicWorks";

const ok = (data: unknown) => ({ status: "SUCCESS", data });
const category = { id: null, name: "Branding", slug: "branding" };
const media = (id: number, role: string, altText: string | null = null) => ({
  mediaId: null,
  role,
  sortOrder: null,
  altText,
  fileUrl: `/api/media/${id}/file`,
});
const listItem = (slug: string, overrides: Record<string, unknown> = {}) => ({
  title: `Title ${slug}`,
  slug,
  summary: null,
  category,
  thumbnail: media(1, "THUMBNAIL", "cover alt"),
  ...overrides,
});
const page = (content: unknown[], hasNext = false) => ok({ content, page: 0, size: 100, totalElements: 0, hasNext });

afterEach(() => vi.restoreAllMocks());

describe("parseCategoryList", () => {
  it("응답 순서대로 slug·name 변환, summary는 null", () => {
    const json = ok([
      { id: 2, name: "Packaging Design", slug: "packaging-design", displayOrder: 1, visible: true },
      { id: 1, name: "Branding", slug: "branding", displayOrder: 2, visible: true },
    ]);

    expect(parseCategoryList(json)).toEqual([
      { slug: "packaging-design", name: "Packaging Design", summary: null },
      { slug: "branding", name: "Branding", summary: null },
    ]);
  });

  it("상세 경로와 겹치는 projects slug와 형식 오류 항목은 제외", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const json = ok([{ name: "Projects", slug: "projects" }, { name: "", slug: "empty" }, category]);

    expect(parseCategoryList(json).map((item) => item.slug)).toEqual(["branding"]);
  });

  it.each([
    ["status 실패", { status: "ERROR", data: null }],
    ["data가 배열 아님", ok({})],
    ["래퍼 아님", []],
  ])("%s는 장애", (_, json) => {
    expect(() => parseCategoryList(json)).toThrow(WorkSourceUnavailableError);
  });
});

describe("parseWorkPage", () => {
  it("목록 항목을 순서대로 변환하고 null 필드는 null로", () => {
    const json = page([listItem("alpha", { summary: "요약" }), listItem("beta", { thumbnail: null })], true);

    expect(parseWorkPage(json)).toEqual({
      hasNext: true,
      items: [
        {
          slug: "alpha",
          title: "Title alpha",
          summary: "요약",
          category: { slug: "branding", name: "Branding" },
          thumbnail: { src: "/api/media/1/file", alt: "cover alt", width: null, height: null },
        },
        {
          slug: "beta",
          title: "Title beta",
          summary: null,
          category: { slug: "branding", name: "Branding" },
          thumbnail: null,
        },
      ],
    });
  });

  it("대체 텍스트가 비면 제목 기반, fileUrl이 없으면 대표 이미지 없음", () => {
    const json = page([
      listItem("alpha", { thumbnail: media(1, "THUMBNAIL", "  ") }),
      listItem("beta", { thumbnail: { role: "THUMBNAIL", altText: "x" } }),
    ]);
    const [alpha, beta] = parseWorkPage(json).items;

    expect(alpha.thumbnail?.alt).toBe("Title alpha 이미지");
    expect(beta.thumbnail).toBeNull();
  });

  it("제목·slug·카테고리 형식 오류 항목만 제외", () => {
    const json = page([
      listItem("no-title", { title: "" }),
      listItem("no-category", { category: null }),
      listItem("bad-summary", { summary: 3 }),
      { ...listItem("x"), slug: undefined },
      listItem("ok"),
    ]);

    expect(parseWorkPage(json).items.map((item) => item.slug)).toEqual(["ok"]);
  });

  it.each([
    ["status 실패", { status: "FAILURE", data: {} }],
    ["content가 배열 아님", ok({ content: null, hasNext: false })],
    ["hasNext 없음", ok({ content: [] })],
  ])("%s는 장애", (_, json) => {
    expect(() => parseWorkPage(json)).toThrow(WorkSourceUnavailableError);
  });
});

describe("parseWorkDetail", () => {
  const detail = (overrides: Record<string, unknown> = {}) =>
    ok({
      title: "Alpha",
      slug: "alpha",
      summary: null,
      description: "설명",
      category,
      mediaItems: [media(1, "THUMBNAIL", "커버"), media(2, "ATTACHMENT"), media(3, "DETAIL_IMAGE")],
      ...overrides,
    });

  it("ATTACHMENT를 빼고 배열 순서대로, 빈 대체 텍스트는 제목 기반 번호", () => {
    expect(parseWorkDetail(detail(), "alpha")).toEqual({
      slug: "alpha",
      title: "Alpha",
      summary: null,
      category: { slug: "branding", name: "Branding" },
      description: "설명",
      images: [
        { src: "/api/media/1/file", alt: "커버", width: null, height: null },
        { src: "/api/media/3/file", alt: "Alpha 이미지 2", width: null, height: null },
      ],
    });
  });

  it("이미지가 1장이면 번호 없는 대체 텍스트, description null 허용", () => {
    const result = parseWorkDetail(detail({ description: null, mediaItems: [media(3, "DETAIL_IMAGE")] }), "alpha");

    expect(result.description).toBeNull();
    expect(result.images.map((image) => image.alt)).toEqual(["Alpha 이미지"]);
  });

  it.each([
    ["요청 slug와 다름", detail({ slug: "beta" })],
    ["카테고리 없음", detail({ category: null })],
    ["mediaItems가 배열 아님", detail({ mediaItems: null })],
    ["status 실패", { status: "ERROR", data: null }],
  ])("%s는 장애", (_, json) => {
    expect(() => parseWorkDetail(json, "alpha")).toThrow(WorkSourceUnavailableError);
  });
});
