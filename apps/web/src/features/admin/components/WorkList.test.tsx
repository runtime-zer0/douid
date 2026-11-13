import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/renderWithQuery";

import { categories, csrfHandler, fail, listItem, ok, page } from "../test/fixtures";
import { WorkList } from "./WorkList";

const router = vi.hoisted(() => ({ replace: vi.fn(), push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

beforeEach(() => router.replace.mockClear());

function useServer(worksResolver: Parameters<typeof http.get>[1]) {
  server.use(csrfHandler, http.get("/api/admin/categories", () => ok(categories)), http.get("/api/admin/works", worksResolver));
}

describe("WorkList 조회", () => {
  it("성공 0건이면 빈 상태와 새 작업물 링크", async () => {
    useServer(() => ok(page([])));
    renderWithQuery(<WorkList page={1} />);

    expect(await screen.findByText("아직 작업물이 없습니다")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "새 작업물 만들기" })).toHaveAttribute("href", "/admin/works/new");
  });

  it("500이면 실패 안내와 다시 시도, 빈 상태로 표시하지 않음", async () => {
    useServer(() => fail(500, "INTERNAL_ERROR"));
    renderWithQuery(<WorkList page={1} />);

    expect(await screen.findByText(/작업물 목록을 불러오지 못했습니다/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
    expect(screen.queryByText("아직 작업물이 없습니다")).not.toBeInTheDocument();
  });

  it("미분류·대표 이미지 없음도 대체 표시로 렌더링", async () => {
    useServer(() => ok(page([listItem(1, { category: null, thumbnail: null })])));
    renderWithQuery(<WorkList page={1} />);

    const table = await screen.findByRole("table", { name: "작업물 목록" });
    expect(within(table).getAllByText("미분류").length).toBeGreaterThan(0);
    expect(within(table).getByText("이미지 없음")).toBeInTheDocument();
  });

  it("전체 건수와 페이지를 표시하고 다음 링크는 ?page=2, API는 0 기반 page·size=20", async () => {
    let query = "";
    useServer(({ request }) => {
      query = new URL(request.url).search;
      return ok(page(Array.from({ length: 20 }, (_, index) => listItem(index + 1)), 21));
    });
    renderWithQuery(<WorkList page={1} />);

    expect(await screen.findByText("1 / 2 페이지, 전체 21건")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "다음" })).toHaveAttribute("href", "/admin/works?page=2");
    expect(query).toBe("?page=0&size=20");
  });

  it("범위 밖 페이지는 마지막 유효 페이지로 replace", async () => {
    useServer(() => ok({ ...page([], 21, 4) }));
    renderWithQuery(<WorkList page={5} />);

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/admin/works?page=2"));
  });
});

describe("WorkList 공개 전환과 삭제", () => {
  it("공개 조건 네 조합을 텍스트로 구분", async () => {
    useServer(() =>
      ok(
        page([
          listItem(1, { visibility: "HIDDEN" }),
          listItem(2, { visibility: "VISIBLE", category: null }),
          listItem(3, { visibility: "VISIBLE", category: { id: 2, name: "Archive", slug: "archive" } }),
          listItem(4, { visibility: "VISIBLE" }),
        ]),
      ),
    );
    const { container } = renderWithQuery(<WorkList page={1} />);

    await waitFor(() => {
      const shown = [...container.querySelectorAll("td [data-exposure]")].map((node) => node.getAttribute("data-exposure"));
      expect(new Set(shown)).toEqual(new Set(["hidden", "uncategorized", "categoryHidden", "eligible"]));
    });
    expect(screen.queryByText(/게시됨/)).not.toBeInTheDocument();
  });

  it("빠른 공개 전환은 PATCH {visibility}, 성공 후 목록 재조회", async () => {
    let visibility: "HIDDEN" | "VISIBLE" = "HIDDEN";
    let body: unknown;
    useServer(() => ok(page([listItem(1, { visibility })])));
    server.use(
      http.patch("/api/admin/works/1/visibility", async ({ request }) => {
        body = await request.json();
        visibility = "VISIBLE";
        return ok({ id: 1, visibility });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<WorkList page={1} />);

    await user.click(await screen.findByRole("button", { name: "작업물 1 공개로 전환" }));

    expect(body).toEqual({ visibility: "VISIBLE" });
    expect(await screen.findByRole("button", { name: "작업물 1 비공개로 전환" })).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("공개로 바꿨습니다");
  });

  it("공개 전환 실패 시 기존 표시 유지, 성공 안내 없음", async () => {
    useServer(() => ok(page([listItem(1)])));
    server.use(http.patch("/api/admin/works/1/visibility", () => fail(500, "INTERNAL_ERROR")));
    const user = userEvent.setup();
    renderWithQuery(<WorkList page={1} />);

    await user.click(await screen.findByRole("button", { name: "작업물 1 공개로 전환" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("공개 설정을 바꾸지 못했습니다");
    expect(screen.getByRole("button", { name: "작업물 1 공개로 전환" })).toBeInTheDocument();
    expect(screen.queryByText(/바꿨습니다/)).not.toBeInTheDocument();
  });

  it("삭제 확인창에서 취소하면 DELETE를 보내지 않음", async () => {
    let deletes = 0;
    useServer(() => ok(page([listItem(1)])));
    server.use(http.delete("/api/admin/works/1", () => (deletes++, new Response(null, { status: 204 }))));
    const user = userEvent.setup();
    renderWithQuery(<WorkList page={1} />);

    await user.click(await screen.findByRole("button", { name: "작업물 1 삭제" }));
    await user.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "취소" }));

    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(deletes).toBe(0);
    await waitFor(() => expect(screen.getByRole("button", { name: "작업물 1 삭제" })).toHaveFocus());
  });

  it("확인 후 204면 목록에서 제거, 확인 문구에 이미지 유지 안내", async () => {
    let removed = false;
    useServer(() => ok(page(removed ? [listItem(2)] : [listItem(1), listItem(2)])));
    server.use(http.delete("/api/admin/works/1", () => ((removed = true), new Response(null, { status: 204 }))));
    const user = userEvent.setup();
    renderWithQuery(<WorkList page={1} />);

    await user.click(await screen.findByRole("button", { name: "작업물 1 삭제" }));
    const dialog = screen.getByRole("alertdialog");
    expect(dialog).toHaveTextContent("이미지는 미디어 라이브러리에 남습니다");
    expect(dialog).toHaveTextContent("복구할 수 없습니다");
    await user.click(within(dialog).getByRole("button", { name: "삭제" }));

    await waitFor(() => expect(screen.queryByRole("link", { name: "작업물 1" })).not.toBeInTheDocument());
    expect(screen.getByRole("link", { name: "작업물 2" })).toBeInTheDocument();
  });

  it("DELETE 500이면 항목 유지와 다이얼로그 안 실패 안내", async () => {
    useServer(() => ok(page([listItem(1)])));
    server.use(http.delete("/api/admin/works/1", () => fail(500, "INTERNAL_ERROR")));
    const user = userEvent.setup();
    renderWithQuery(<WorkList page={1} />);

    await user.click(await screen.findByRole("button", { name: "작업물 1 삭제" }));
    await user.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "삭제" }));

    expect(await within(screen.getByRole("alertdialog")).findByRole("alert")).toHaveTextContent("서버 오류");
    // 다이얼로그가 열린 동안 바깥 콘텐츠는 aria-hidden
    expect(screen.getByRole("link", { name: "작업물 1", hidden: true })).toBeInTheDocument();
  });

  it("마지막 페이지의 마지막 항목을 삭제하면 이전 페이지로 이동", async () => {
    let removed = false;
    useServer(() => ok(removed ? page([], 20, 1) : page([listItem(21)], 21, 1)));
    server.use(http.delete("/api/admin/works/21", () => ((removed = true), new Response(null, { status: 204 }))));
    const user = userEvent.setup();
    renderWithQuery(<WorkList page={2} />);

    await user.click(await screen.findByRole("button", { name: "작업물 21 삭제" }));
    await user.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "삭제" }));

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/admin/works"));
  });
});
