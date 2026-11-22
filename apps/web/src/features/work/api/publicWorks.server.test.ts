import { delay, http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";

import { WorkSourceUnavailableError } from "./parsePublicWorks";
import { fetchAllPublicWorks, fetchPublicCategories, fetchPublicWork } from "./publicWorks.server";

vi.mock("next/server", () => ({ connection: vi.fn(async () => {}) }));

const API = "http://api.test";
const category = { id: null, name: "Branding", slug: "branding" };
const item = (slug: string) => ({ title: slug.toUpperCase(), slug, summary: null, category, thumbnail: null });
const page = (content: unknown[], hasNext: boolean) => ({
  status: "SUCCESS",
  data: { content, page: 0, size: 100, totalElements: 0, hasNext },
});

beforeEach(() => vi.stubEnv("API_ORIGIN", `${API}/`));
afterEach(() => {
  vi.unstubAllEnvs();
  vi.restoreAllMocks();
});

describe("fetchAllPublicWorks", () => {
  it("hasNext가 false일 때까지 size=100으로 페이지를 읽어 순서대로 합치고 중복 slug는 처음 항목만", async () => {
    const requested: string[] = [];
    const pages = [page([item("a"), item("b")], true), page([item("b"), item("c")], true), page([item("d")], false)];
    server.use(
      http.get(`${API}/api/public/works`, ({ request }) => {
        const url = new URL(request.url);
        requested.push(`${url.searchParams.get("page")}:${url.searchParams.get("size")}`);
        return HttpResponse.json(pages[Number(url.searchParams.get("page"))]);
      }),
    );

    const works = await fetchAllPublicWorks();

    expect(works.map((work) => work.slug)).toEqual(["a", "b", "c", "d"]);
    expect(requested).toEqual(["0:100", "1:100", "2:100"]);
  });

  it("10페이지 상한에서 멈추고 경고", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    let calls = 0;
    server.use(
      http.get(`${API}/api/public/works`, ({ request }) => {
        calls += 1;
        return HttpResponse.json(page([item(`w${new URL(request.url).searchParams.get("page")}`)], true));
      }),
    );

    expect(await fetchAllPublicWorks()).toHaveLength(10);
    expect(calls).toBe(10);
    expect(warn).toHaveBeenCalled();
  });

  it.each([
    ["5xx", () => new HttpResponse(null, { status: 503 })],
    ["404 외 4xx", () => new HttpResponse(null, { status: 400 })],
    ["목록 404", () => new HttpResponse(null, { status: 404 })],
    ["연결 실패", () => HttpResponse.error()],
    ["잘못된 JSON", () => new HttpResponse("{oops", { headers: { "Content-Type": "application/json" } })],
    ["status ERROR", () => HttpResponse.json({ status: "ERROR", data: { code: "X" } })],
  ])("%s는 WorkSourceUnavailableError", async (_, resolver) => {
    server.use(http.get(`${API}/api/public/works`, resolver));

    await expect(fetchAllPublicWorks()).rejects.toThrow(WorkSourceUnavailableError);
  });

  it("3초 넘게 응답이 없으면 WorkSourceUnavailableError", { timeout: 10_000 }, async () => {
    server.use(
      http.get(`${API}/api/public/works`, async () => {
        await delay("infinite");
        return HttpResponse.json(page([], false));
      }),
    );

    await expect(fetchAllPublicWorks()).rejects.toThrow(WorkSourceUnavailableError);
  });
});

describe("fetchPublicWork", () => {
  const detail = (slug: string) => ({
    status: "SUCCESS",
    data: { ...item(slug), description: null, mediaItems: [] },
  });

  it("상세 404는 null", async () => {
    server.use(
      http.get(`${API}/api/public/works/:slug`, () =>
        HttpResponse.json({ status: "FAILURE", data: { code: "WORK_NOT_FOUND" } }, { status: 404 }),
      ),
    );

    expect(await fetchPublicWork("missing")).toBeNull();
  });

  it("인코딩이 필요한 slug는 경로 세그먼트로 인코딩해 요청", async () => {
    let requestedPath = "";
    server.use(
      http.get(`${API}/api/public/works/:slug`, ({ request }) => {
        requestedPath = new URL(request.url).pathname;
        return HttpResponse.json(detail("a b/c"));
      }),
    );

    expect((await fetchPublicWork("a b/c"))?.slug).toBe("a b/c");
    expect(requestedPath).toBe("/api/public/works/a%20b%2Fc");
  });

  it("상세 5xx는 404와 구분해 WorkSourceUnavailableError", async () => {
    server.use(http.get(`${API}/api/public/works/:slug`, () => new HttpResponse(null, { status: 500 })));

    await expect(fetchPublicWork("alpha")).rejects.toThrow(WorkSourceUnavailableError);
  });
});

describe("fetchPublicCategories", () => {
  it("공개 카테고리를 서버 순서대로 반환", async () => {
    server.use(
      http.get(`${API}/api/public/categories`, () =>
        HttpResponse.json({
          status: "SUCCESS",
          data: [
            { id: 2, name: "Editorial Design", slug: "editorial-design", displayOrder: 1, visible: true },
            { ...category, id: 1, displayOrder: 2, visible: true },
          ],
        }),
      ),
    );

    expect((await fetchPublicCategories()).map((item) => item.slug)).toEqual(["editorial-design", "branding"]);
  });
});
