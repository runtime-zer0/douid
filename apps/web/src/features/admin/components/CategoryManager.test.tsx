import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { delay, http } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/renderWithQuery";

import type { Category } from "../api/types";
import { categories, csrfHandler, fail, ok } from "../test/fixtures";
import { CategoryManager } from "./CategoryManager";

function listServer(list: () => Category[] | Response = () => categories) {
  server.use(
    csrfHandler,
    http.get("/api/admin/categories", () => {
      const value = list();
      return value instanceof Response ? value : ok(value);
    }),
  );
}

describe("CategoryManager", () => {
  it("공개·비공개 전체를 서버 순서대로, 공개 여부를 텍스트로 표시", async () => {
    listServer();
    renderWithQuery(<CategoryManager />);

    const rows = within(await screen.findByRole("table", { name: "카테고리 목록" })).getAllByRole("row").slice(1);
    expect(rows.map((row) => within(row).getAllByRole("cell")[0].textContent)).toEqual([
      "Branding/branding",
      "Archive/archive",
    ]);
    expect(rows[0]).toHaveTextContent("공개");
    expect(rows[1]).toHaveTextContent("비공개");
  });

  it("조회 실패와 빈 상태를 구분", async () => {
    listServer(() => fail(500, "INTERNAL_ERROR"));
    const { unmount } = renderWithQuery(<CategoryManager />);
    expect(await screen.findByText(/카테고리 목록을 불러오지 못했습니다/)).toBeInTheDocument();
    expect(screen.queryByText("아직 카테고리가 없습니다")).not.toBeInTheDocument();
    unmount();

    listServer(() => []);
    renderWithQuery(<CategoryManager />);
    expect(await screen.findByText("아직 카테고리가 없습니다")).toBeInTheDocument();
  });

  it("생성 폼 초기값은 순서 0·공개, POST 본문에 네 필드", async () => {
    let body: unknown;
    listServer();
    server.use(
      http.post("/api/admin/categories", async ({ request }) => {
        body = await request.json();
        return ok({ id: 3, ...(body as object) }, 201);
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<CategoryManager />);

    await user.click(await screen.findByRole("button", { name: "새 카테고리" }));
    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByLabelText("순서")).toHaveValue("0");
    expect(within(dialog).getByLabelText("공개 카테고리")).toBeChecked();
    await user.type(within(dialog).getByLabelText("이름"), "Film");
    await user.type(within(dialog).getByLabelText("슬러그"), "film");
    await user.click(within(dialog).getByRole("button", { name: "카테고리 만들기" }));

    await waitFor(() => expect(body).toEqual({ name: "Film", slug: "film", displayOrder: 0, visible: true }));
    expect(await screen.findByRole("status")).toHaveTextContent("카테고리를 만들었습니다");
  });

  it("자기 슬러그 그대로 수정하면 성공, 슬러그 중복이면 필드 오류", async () => {
    const bodies: unknown[] = [];
    let duplicate = false;
    listServer();
    server.use(
      http.put("/api/admin/categories/1", async ({ request }) => {
        bodies.push(await request.json());
        return duplicate ? fail(409, "CATEGORY_SLUG_DUPLICATE") : ok({ ...categories[0] });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<CategoryManager />);

    await user.click(await screen.findByRole("button", { name: "Branding 편집" }));
    let dialog = screen.getByRole("dialog");
    await user.clear(within(dialog).getByLabelText("순서"));
    await user.type(within(dialog).getByLabelText("순서"), "-1");
    await user.click(within(dialog).getByRole("button", { name: "변경 저장" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(bodies[0]).toEqual({ name: "Branding", slug: "branding", displayOrder: -1, visible: true });

    duplicate = true;
    await user.click(screen.getByRole("button", { name: "Branding 편집" }));
    dialog = screen.getByRole("dialog");
    await user.clear(within(dialog).getByLabelText("슬러그"));
    await user.type(within(dialog).getByLabelText("슬러그"), "archive");
    await user.click(within(dialog).getByRole("button", { name: "변경 저장" }));

    const slug = within(dialog).getByLabelText("슬러그");
    await waitFor(() => expect(slug).toHaveAttribute("aria-invalid", "true"));
    expect(slug).toHaveAccessibleDescription("이미 사용 중인 슬러그입니다.");
  });

  it("비공개 전환은 확인을 먼저 묻고, 취소하면 PUT 없음", async () => {
    let puts = 0;
    listServer();
    server.use(http.put("/api/admin/categories/1", () => (puts++, ok(categories[0]))));
    const user = userEvent.setup();
    renderWithQuery(<CategoryManager />);

    await user.click(await screen.findByRole("button", { name: "Branding 편집" }));
    const dialog = screen.getByRole("dialog");
    await user.click(within(dialog).getByLabelText("공개 카테고리"));
    await user.click(within(dialog).getByRole("button", { name: "변경 저장" }));

    const confirm = await screen.findByRole("alertdialog");
    expect(confirm).toHaveTextContent("서버 공개 조회에서 제외");
    expect(confirm).toHaveTextContent("공개 설정은 그대로");
    await user.click(within(confirm).getByRole("button", { name: "취소" }));
    expect(puts).toBe(0);

    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "변경 저장" }));
    await user.click(within(await screen.findByRole("alertdialog")).getByRole("button", { name: "비공개로 저장" }));
    await waitFor(() => expect(puts).toBe(1));
  });

  it("연결 작업물이 있어 삭제가 거부되면 안내하고 항목 유지", async () => {
    listServer();
    server.use(http.delete("/api/admin/categories/2", () => fail(409, "CATEGORY_HAS_WORKS")));
    const user = userEvent.setup();
    renderWithQuery(<CategoryManager />);

    await user.click(await screen.findByRole("button", { name: "Archive 삭제" }));
    const dialog = screen.getByRole("alertdialog");
    await user.click(within(dialog).getByRole("button", { name: "삭제" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent("비공개 포함");
    expect(screen.getByRole("button", { name: "Archive 삭제", hidden: true })).toBeInTheDocument();
  });

  it("저장 연타에도 요청은 1회", async () => {
    let posts = 0;
    listServer();
    server.use(
      http.post("/api/admin/categories", async () => {
        posts += 1;
        await delay(50);
        return ok({ ...categories[0], id: 9 }, 201);
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<CategoryManager />);

    await user.click(await screen.findByRole("button", { name: "새 카테고리" }));
    const dialog = screen.getByRole("dialog");
    await user.type(within(dialog).getByLabelText("이름"), "A");
    await user.type(within(dialog).getByLabelText("슬러그"), "a");
    await user.dblClick(within(dialog).getByRole("button", { name: "카테고리 만들기" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(posts).toBe(1);
  });
});
