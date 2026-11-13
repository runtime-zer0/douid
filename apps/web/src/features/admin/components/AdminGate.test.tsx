import { focusManager } from "@tanstack/react-query";
import { act, screen, waitFor } from "@testing-library/react";
import { delay, http } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/renderWithQuery";

import { admin, fail, ok } from "../test/fixtures";
import { AdminGate } from "./AdminGate";
import { ReauthProvider } from "./ReauthProvider";

const router = vi.hoisted(() => ({ replace: vi.fn(), push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

const ui = (
  <ReauthProvider>
    <AdminGate>
      <p>보호된 목록</p>
    </AdminGate>
  </ReauthProvider>
);

beforeEach(() => {
  router.replace.mockClear();
  window.history.replaceState(null, "", "/admin/works?page=2");
});
afterEach(() => focusManager.setFocused(undefined));

describe("AdminGate", () => {
  it("me 응답 전에는 보호 콘텐츠 없이 확인 중 표시", async () => {
    server.use(
      http.get("/api/auth/me", async () => {
        await delay("infinite");
        return ok(admin);
      }),
    );
    renderWithQuery(ui);

    expect(screen.getByRole("status")).toHaveTextContent("관리자 확인 중");
    expect(screen.queryByText("보호된 목록")).not.toBeInTheDocument();
  });

  it("me 200이면 보호 콘텐츠 렌더링", async () => {
    server.use(http.get("/api/auth/me", () => ok(admin)));
    renderWithQuery(ui);

    expect(await screen.findByText("보호된 목록")).toBeInTheDocument();
  });

  it("첫 확인 401이면 현재 경로를 next로 로그인 화면 이동", async () => {
    server.use(http.get("/api/auth/me", () => fail(401, "UNAUTHORIZED")));
    renderWithQuery(ui);

    await waitFor(() =>
      expect(router.replace).toHaveBeenCalledWith(`/admin/login?next=${encodeURIComponent("/admin/works?page=2")}`),
    );
    expect(screen.queryByText("보호된 목록")).not.toBeInTheDocument();
  });

  it.each([
    ["500", () => fail(500, "INTERNAL_ERROR")],
    ["네트워크 오류", () => Response.error()],
  ])("%s면 다시 시도를 보여주고 로그인으로 보내지 않음", async (_, resolver) => {
    server.use(http.get("/api/auth/me", resolver));
    renderWithQuery(ui);

    // 네트워크 오류는 1회 재시도(기본 지연 1초) 후 실패 표시
    expect(await screen.findByText(/관리자 상태를 확인하지 못했습니다/, {}, { timeout: 3000 })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
    expect(router.replace).not.toHaveBeenCalled();
  });

  it("인증 이후 창 포커스 재조회가 401이면 화면 유지 + 재인증 다이얼로그, 로그인 이동 없음", async () => {
    let authenticated = true;
    server.use(http.get("/api/auth/me", () => (authenticated ? ok(admin) : fail(401, "UNAUTHORIZED"))));
    renderWithQuery(ui);
    await screen.findByText("보호된 목록");

    authenticated = false;
    act(() => {
      focusManager.setFocused(false);
      focusManager.setFocused(true);
    });

    expect(await screen.findByRole("dialog", { name: "다시 로그인" })).toBeInTheDocument();
    expect(screen.getAllByRole("dialog")).toHaveLength(1);
    expect(screen.getByText("보호된 목록")).toBeInTheDocument();
    expect(router.replace).not.toHaveBeenCalled();
  });
});
