import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import type { SiteContent } from "../content";
import { SiteFooter } from "./SiteFooter";

const EMPTY: SiteContent = {
  about: { body: null },
  contact: { email: null, channels: [] },
  studio: null,
  copyright: null,
  policyLinks: [],
};

// 렌더링 검증 전용 fixture, 배포 데이터 아님
const FILLED: SiteContent = {
  about: { body: null },
  contact: { email: "hello@example.test", channels: [{ label: "Instagram", href: "https://example.test/douid" }] },
  studio: "Studio fixture",
  copyright: "© fixture",
  policyLinks: [{ label: "개인정보처리방침", href: "/privacy" }],
};

describe("SiteFooter", () => {
  it("건너뛰기 도착점이고 로고와 주요 메뉴를 항상 표시", () => {
    const { container } = render(<SiteFooter content={EMPTY} />);

    const footer = screen.getByRole("contentinfo");
    expect(footer).toHaveAttribute("id", "site-footer");
    expect(footer).toHaveAttribute("tabindex", "-1");
    expect(container.querySelectorAll("img")).toHaveLength(1);
    const links = within(screen.getByRole("navigation", { name: "푸터 메뉴" })).getAllByRole("link");
    expect(links.map((link) => [link.textContent, link.getAttribute("href")])).toEqual([
      ["ABOUT", "/about"],
      ["WORK", "/work"],
      ["CONTACT", "/contact"],
    ]);
  });

  it("승인 콘텐츠가 없으면 연락·스튜디오·저작권·정책 영역을 그리지 않음", () => {
    render(<SiteFooter content={EMPTY} />);

    expect(screen.getAllByRole("link")).toHaveLength(3);
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "정책" })).not.toBeInTheDocument();
  });

  it("값이 있으면 해당 영역 표시", () => {
    render(<SiteFooter content={FILLED} />);

    expect(screen.getByRole("link", { name: "hello@example.test" })).toHaveAttribute("href", "mailto:hello@example.test");
    expect(screen.getByRole("link", { name: "Instagram" })).toHaveAttribute("href", "https://example.test/douid");
    expect(screen.getByText("Studio fixture")).toBeInTheDocument();
    expect(screen.getByText("© fixture")).toBeInTheDocument();
    expect(within(screen.getByRole("navigation", { name: "정책" })).getByRole("link")).toHaveAttribute("href", "/privacy");
  });

  it.each([["빈 콘텐츠", EMPTY], ["채운 콘텐츠", FILLED]])("%s에서 빈 href나 # 링크 없음", (_, content) => {
    render(<SiteFooter content={content} />);

    screen.getAllByRole("link").forEach((link) => {
      const href = link.getAttribute("href");
      expect(href).toBeTruthy();
      expect(href).not.toBe("#");
    });
  });
});
