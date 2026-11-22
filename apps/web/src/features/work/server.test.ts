import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";

import { WORK_CATEGORIES } from "./content/categories";
import { getCategories, getCategory, getRecommendationCatalog, getWork, getWorks, isServerMode } from "./server";

vi.mock("next/server", () => ({ connection: vi.fn(async () => {}) }));

const API = "http://api.test";

afterEach(() => {
  vi.unstubAllEnvs();
  vi.restoreAllMocks();
});

describe("정적 모드 (API_ORIGIN 미설정)", () => {
  it("승인 카테고리와 정적 카탈로그를 반환하고 서버를 호출하지 않음", async () => {
    vi.stubEnv("API_ORIGIN", " ");
    const fetchSpy = vi.spyOn(globalThis, "fetch");

    expect(isServerMode()).toBe(false);
    expect((await getCategories()).map((category) => category.slug)).toEqual(
      WORK_CATEGORIES.map((category) => category.slug),
    );
    expect(await getCategory("branding")).toEqual({ slug: "branding", name: "Branding", summary: null });
    expect(await getCategory("unknown")).toBeNull();
    expect(await getCategory("projects")).toBeNull();
    expect(await getWorks()).toEqual([]);
    expect(await getWork("unknown")).toBeNull();
    expect(await getRecommendationCatalog()).toEqual([]);
    expect(fetchSpy).not.toHaveBeenCalled();
  });
});

describe("서버 모드 (API_ORIGIN 설정)", () => {
  const work = (slug: string, categorySlug: string) => ({
    title: `Title ${slug}`,
    slug,
    summary: null,
    category: { id: null, name: categorySlug, slug: categorySlug },
    thumbnail: null,
  });
  const useWorks = (...content: unknown[]) =>
    server.use(
      http.get(`${API}/api/public/works`, () =>
        HttpResponse.json({ status: "SUCCESS", data: { content, page: 0, size: 100, totalElements: 0, hasNext: false } }),
      ),
    );

  it("카테고리는 서버 순서, 없는 카테고리는 null", async () => {
    vi.stubEnv("API_ORIGIN", API);
    server.use(
      http.get(`${API}/api/public/categories`, () =>
        HttpResponse.json({
          status: "SUCCESS",
          data: [
            { id: 1, name: "Marketing Design", slug: "marketing-design", displayOrder: 1, visible: true },
            { id: 2, name: "Branding", slug: "branding", displayOrder: 2, visible: true },
          ],
        }),
      ),
    );

    expect(isServerMode()).toBe(true);
    expect((await getCategories()).map((category) => category.slug)).toEqual(["marketing-design", "branding"]);
    expect(await getCategory("editorial-design")).toBeNull();
  });

  it("카테고리별 작업물은 서버 순서를 유지한 채 필터", async () => {
    vi.stubEnv("API_ORIGIN", API);
    useWorks(work("c", "branding"), work("b", "marketing-design"), work("a", "branding"));

    expect((await getWorks("branding")).map((item) => item.slug)).toEqual(["c", "a"]);
  });

  it("없는 상세는 null", async () => {
    vi.stubEnv("API_ORIGIN", API);
    useWorks();
    server.use(http.get(`${API}/api/public/works/:slug`, () => new HttpResponse(null, { status: 404 })));

    expect(await getWork("missing")).toBeNull();
  });

  it("상세 응답이 남아 있어도 공개 목록에 없는 slug는 null (비공개 전환 반영)", async () => {
    vi.stubEnv("API_ORIGIN", API);
    const detail = { ...work("a", "branding"), description: null, mediaItems: [] };
    server.use(http.get(`${API}/api/public/works/:slug`, () => HttpResponse.json({ status: "SUCCESS", data: detail })));

    useWorks(work("a", "branding"));
    expect((await getWork("a"))?.slug).toBe("a");

    useWorks();
    expect(await getWork("a")).toBeNull();
  });

  it("추천 대조 목록은 공개 작업물의 { id: slug, title }을 서버 순서로", async () => {
    vi.stubEnv("API_ORIGIN", API);
    useWorks(work("b", "branding"), work("a", "branding"));

    expect(await getRecommendationCatalog()).toEqual([
      { id: "b", title: "Title b" },
      { id: "a", title: "Title a" },
    ]);
  });

  it.each([
    ["5xx", () => new HttpResponse(null, { status: 502 })],
    ["연결 실패", () => HttpResponse.error()],
  ])("추천 대조 목록 조회 %s 시 예외 없이 빈 배열", async (_, resolver) => {
    vi.stubEnv("API_ORIGIN", API);
    vi.spyOn(console, "warn").mockImplementation(() => {});
    server.use(http.get(`${API}/api/public/works`, resolver));

    expect(await getRecommendationCatalog()).toEqual([]);
  });
});
