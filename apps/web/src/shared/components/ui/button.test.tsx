import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { Button } from "./button";

describe("Button", () => {
  it("기본 variant와 size로 렌더링된다", () => {
    render(<Button>저장</Button>);

    const button = screen.getByRole("button", { name: "저장" });
    expect(button).toHaveAttribute("data-variant", "default");
    expect(button).toHaveAttribute("data-size", "default");
  });

  it("클릭 시 onClick이 호출된다", async () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>저장</Button>);

    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(onClick).toHaveBeenCalledOnce();
  });

  it("asChild면 자식 요소에 버튼 스타일을 입힌다", () => {
    render(
      <Button asChild variant="link">
        <a href="/works">작업물 보기</a>
      </Button>,
    );

    const link = screen.getByRole("link", { name: "작업물 보기" });
    expect(link).toHaveAttribute("data-slot", "button");
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
