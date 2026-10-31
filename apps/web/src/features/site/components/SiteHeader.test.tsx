import { act, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { SiteHeader } from "./SiteHeader";

const navigation = vi.hoisted(() => ({ pathname: "/" }));
vi.mock("next/navigation", () => ({ usePathname: () => navigation.pathname }));

let frameQueue: FrameRequestCallback[] = [];
const flushFrames = () =>
  act(() => {
    const queued = frameQueue;
    frameQueue = [];
    queued.forEach((callback) => callback(0));
  });

beforeEach(() => {
  frameQueue = [];
  navigation.pathname = "/";
  vi.stubGlobal("requestAnimationFrame", (callback: FrameRequestCallback) => frameQueue.push(callback));
  vi.stubGlobal("cancelAnimationFrame", () => {});
});

afterEach(() => {
  vi.unstubAllGlobals();
  window.scrollY = 0;
});

describe("SiteHeader", () => {
  it("주요 메뉴는 ABOUT, WORK, CONTACT 순서로 실제 경로에 연결", () => {
    render(<SiteHeader />);

    const links = within(screen.getByRole("navigation", { name: "주요 메뉴" })).getAllByRole("link");
    expect(links.map((link) => [link.textContent, link.getAttribute("href")])).toEqual([
      ["ABOUT", "/about"],
      ["WORK", "/work"],
      ["CONTACT", "/contact"],
    ]);
  });

  it("로고 링크는 홈으로 이동", () => {
    render(<SiteHeader />);

    expect(screen.getByRole("link", { name: "DOUID 홈" })).toHaveAttribute("href", "/");
  });

  it("하위 경로에서도 해당 메뉴를 현재 페이지로 표시", () => {
    navigation.pathname = "/work/branding";
    render(<SiteHeader />);

    expect(screen.getByRole("link", { name: "WORK" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "ABOUT" })).not.toHaveAttribute("aria-current");
  });

  it("intro면 맨 위에서 중앙, 스크롤하면 같은 로고가 좌상단으로 이동하고 다시 맨 위면 복귀", () => {
    const { container } = render(<SiteHeader intro />);
    const header = screen.getByRole("banner");
    expect(header).toHaveAttribute("data-intro", "center");

    window.scrollY = 120;
    window.dispatchEvent(new Event("scroll"));
    window.dispatchEvent(new Event("scroll"));
    flushFrames();
    expect(header).toHaveAttribute("data-intro", "compact");
    expect(container.querySelectorAll("img")).toHaveLength(1);

    window.scrollY = 0;
    window.dispatchEvent(new Event("scroll"));
    flushFrames();
    expect(header).toHaveAttribute("data-intro", "center");
    expect(container.querySelectorAll("img")).toHaveLength(1);
  });

  it("intro가 아니면 처음부터 좌상단", () => {
    render(<SiteHeader tone="light" />);

    expect(screen.getByRole("banner")).not.toHaveAttribute("data-intro");
  });
});
