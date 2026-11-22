import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import WorkError from "./error";

describe("WORK 일시적 오류 화면", () => {
  it("404와 다른 일시적 오류 안내와 다시 시도, WORK 복귀 링크 표시", () => {
    const reset = vi.fn();
    render(<WorkError error={new Error("down")} reset={reset} />);

    expect(screen.getByRole("heading", { name: "작업물을 잠시 불러올 수 없습니다" })).toBeInTheDocument();
    expect(screen.queryByText(/찾을 수 없습니다/)).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "WORK 목록으로" })).toHaveAttribute("href", "/work");

    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(reset).toHaveBeenCalledOnce();
  });
});
