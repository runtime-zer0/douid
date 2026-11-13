import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/renderWithQuery";

import { adminKeys } from "../api/queryKeys";
import { admin, csrfHandler, fail, ok } from "../test/fixtures";
import { AdminShell } from "./AdminShell";

const navigation = vi.hoisted(() => ({ pathname: "/admin/works/3/edit", replace: vi.fn() }));
vi.mock("next/navigation", () => ({
  usePathname: () => navigation.pathname,
  useRouter: () => ({ replace: navigation.replace, push: vi.fn() }),
}));

beforeEach(() => navigation.replace.mockClear());

function renderShell() {
  server.use(http.get("/api/auth/me", () => ok(admin)));
  return renderWithQuery(
    <AdminShell>
      <p>본문</p>
    </AdminShell>,
  );
}

describe("AdminShell", () => {
  it("현재 경로 메뉴를 aria-current로 표시하고 관리자 이메일 노출", async () => {
    renderShell();

    expect(screen.getByRole("link", { name: "작업물" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "카테고리" })).not.toHaveAttribute("aria-current");
    expect(await screen.findByText(admin.email)).toBeInTheDocument();
  });

  it("로그아웃 성공 시 /admin/login으로 1회만 이동하고 me 외 관리자 캐시 제거", async () => {
    server.use(csrfHandler, http.post("/api/auth/logout", () => ok(null)));
    const user = userEvent.setup();
    const { queryClient } = renderShell();
    queryClient.setQueryData(adminKeys.works(0), { content: [] });

    await user.click(screen.getByRole("button", { name: "로그아웃" }));

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledTimes(1));
    expect(navigation.replace).toHaveBeenCalledWith("/admin/login");
    expect(queryClient.getQueryData(adminKeys.works(0))).toBeUndefined();
  });

  it("로그아웃 실패 시 이동하지 않고 안내", async () => {
    server.use(csrfHandler, http.post("/api/auth/logout", () => fail(500, "INTERNAL_ERROR")));
    const user = userEvent.setup();
    renderShell();

    await user.click(screen.getByRole("button", { name: "로그아웃" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("로그아웃하지 못했습니다");
    expect(navigation.replace).not.toHaveBeenCalled();
  });
});
