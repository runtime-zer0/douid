import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { delay, http } from "msw";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/renderWithQuery";

import { adminKeys } from "../api/queryKeys";
import { admin, csrfHandler, fail, ok } from "../test/fixtures";
import { LoginForm } from "./LoginForm";

async function fill(user: ReturnType<typeof userEvent.setup>, password = "secret-pass") {
  await user.type(screen.getByLabelText("이메일"), "admin@douid.kr");
  await user.type(screen.getByLabelText("비밀번호"), password);
}

describe("LoginForm", () => {
  it("성공하면 onSuccess 호출과 me 캐시 설정", async () => {
    server.use(csrfHandler, http.post("/api/auth/login", () => ok(admin)));
    const onSuccess = vi.fn();
    const user = userEvent.setup();
    const { queryClient } = renderWithQuery(<LoginForm onSuccess={onSuccess} />);

    await fill(user);
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledWith(admin));
    expect(queryClient.getQueryData(adminKeys.me)).toEqual(admin);
  });

  it("인증 실패면 같은 안내, 비밀번호만 비우고 이메일 유지", async () => {
    server.use(csrfHandler, http.post("/api/auth/login", () => fail(401, "AUTHENTICATION_FAILED")));
    const user = userEvent.setup();
    renderWithQuery(<LoginForm onSuccess={vi.fn()} />);

    await fill(user);
    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이메일 또는 비밀번호가 올바르지 않습니다.");
    expect(screen.getByLabelText("비밀번호")).toHaveValue("");
    expect(screen.getByLabelText("이메일")).toHaveValue("admin@douid.kr");
  });

  it("403이면 보안 토큰 만료 안내", async () => {
    server.use(csrfHandler, http.post("/api/auth/login", () => fail(403, "FORBIDDEN")));
    const user = userEvent.setup();
    renderWithQuery(<LoginForm onSuccess={vi.fn()} />);

    await fill(user);
    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("보안 토큰");
  });

  it("제출 중 연타해도 로그인 요청은 1회", async () => {
    let calls = 0;
    server.use(
      csrfHandler,
      http.post("/api/auth/login", async () => {
        calls += 1;
        await delay(50);
        return ok(admin);
      }),
    );
    const onSuccess = vi.fn();
    const user = userEvent.setup();
    renderWithQuery(<LoginForm onSuccess={onSuccess} />);

    await fill(user);
    const button = screen.getByRole("button", { name: "로그인" });
    await user.dblClick(button);
    await user.keyboard("{Enter}");

    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
    expect(calls).toBe(1);
  });

  it("비밀번호를 브라우저 저장소에 남기지 않음", async () => {
    server.use(csrfHandler, http.post("/api/auth/login", () => fail(401, "AUTHENTICATION_FAILED")));
    const user = userEvent.setup();
    renderWithQuery(<LoginForm onSuccess={vi.fn()} />);

    await fill(user, "very-secret-123");
    await user.click(screen.getByRole("button", { name: "로그인" }));
    await screen.findByRole("alert");

    const stored = [localStorage, sessionStorage].flatMap((storage) =>
      Object.keys(storage).map((key) => storage.getItem(key) ?? ""),
    );
    expect(stored.some((value) => value.includes("very-secret-123"))).toBe(false);
  });
});
