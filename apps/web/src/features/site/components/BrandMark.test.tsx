import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { BrandMark } from "./BrandMark";

describe("BrandMark", () => {
  it("기본은 어두운 배경용 SVG, 접근성 이름 DOUID", () => {
    render(<BrandMark />);

    const logo = screen.getByRole("img", { name: "DOUID" });
    expect(logo.getAttribute("src")).toContain("douid-logo-dark.svg");
  });

  it("tone light면 밝은 배경용 SVG", () => {
    render(<BrandMark tone="light" />);

    expect(screen.getByRole("img", { name: "DOUID" }).getAttribute("src")).toContain("douid-logo-light.svg");
  });

  it("decorative면 보조 기술에서 숨김", () => {
    const { container } = render(<BrandMark decorative />);

    expect(screen.queryByRole("img")).not.toBeInTheDocument();
    expect(container.querySelector("img")).toHaveAttribute("aria-hidden", "true");
  });

  it("className으로 크기만 조정, 원본 비율 유지", () => {
    render(<BrandMark className="w-64" />);

    const logo = screen.getByRole("img", { name: "DOUID" });
    expect(logo).toHaveClass("w-64", "h-auto");
    expect(logo).toHaveAttribute("width", "4809");
    expect(logo).toHaveAttribute("height", "2304");
  });
});
