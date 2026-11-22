import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import type { PublicWorkDetail, PublicWorkSummary, WorkCategoryView } from "../model/types";
import { CategoryList } from "./CategoryList";
import { ProjectDetail } from "./ProjectDetail";
import { ProjectImage } from "./ProjectImage";
import { ProjectList } from "./ProjectList";

// 템플릿 검증 전용 fixture, 배포 데이터 아님
const categories: WorkCategoryView[] = [
  { slug: "branding", name: "Branding", summary: null },
  { slug: "packaging-design", name: "Packaging Design", summary: null },
  { slug: "editorial-design", name: "Editorial Design", summary: null },
];
const cover = { src: "/work/fixture-project/cover.jpg", width: 1600, height: 900, alt: "fixture cover" };
const fixtureSummary: PublicWorkSummary = {
  slug: "fixture-project",
  title: "Fixture Project",
  summary: "Fixture summary",
  category: { slug: "branding", name: "Branding" },
  thumbnail: cover,
};
const fixtureDetail: PublicWorkDetail = {
  slug: "fixture-project",
  title: "Fixture Project",
  summary: null,
  category: { slug: "branding", name: "Branding" },
  description: "Fixture description",
  images: [cover, { src: "/api/media/7/file", width: null, height: null, alt: "Fixture Project 이미지 2" }],
};

describe("CategoryList", () => {
  it("카테고리를 배열 순서대로 번호와 정식 명칭 링크로 표시", () => {
    render(<CategoryList categories={categories} />);

    const links = within(screen.getByRole("list")).getAllByRole("link");
    expect(links.map((link) => link.getAttribute("href"))).toEqual([
      "/work/branding",
      "/work/packaging-design",
      "/work/editorial-design",
    ]);
    expect(links[2]).toHaveTextContent("03Editorial Design");
  });
});

describe("ProjectList", () => {
  it("공개 작업물이 없으면 빈 상태와 WORK 복귀 링크만 표시", () => {
    render(<ProjectList projects={[]} />);

    expect(screen.getByText("아직 공개된 작업이 없습니다.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "WORK 목록으로" })).toHaveAttribute("href", "/work");
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("카드는 대표 이미지·제목·요약과 정본 상세 경로를 가짐", () => {
    render(<ProjectList projects={[fixtureSummary]} />);

    const card = screen.getByRole("link", { name: /Fixture Project/ });
    expect(card).toHaveAttribute("href", "/work/projects/fixture-project");
    expect(within(card).getByRole("img", { name: "fixture cover" })).toBeInTheDocument();
    expect(within(card).getByText("Fixture summary")).toBeInTheDocument();
  });

  it("대표 이미지가 없으면 이미지 없이 제목만 표시", () => {
    render(<ProjectList projects={[{ ...fixtureSummary, thumbnail: null, summary: null }]} />);

    const card = screen.getByRole("link", { name: "Fixture Project" });
    expect(within(card).queryByRole("img")).not.toBeInTheDocument();
  });

  it("HTML처럼 보이는 긴 제목도 텍스트 그대로 표시", () => {
    const title = `<b>${"Long".repeat(60)}</b>`;
    const { container } = render(<ProjectList projects={[{ ...fixtureSummary, title }]} />);

    expect(screen.getByText(title)).toBeInTheDocument();
    expect(container.querySelector("b")).toBeNull();
  });
});

describe("ProjectDetail", () => {
  it("제목, 소속 카테고리 1개 링크, 이미지 순서, 설명, WORK 복귀 링크 표시", () => {
    render(<ProjectDetail project={fixtureDetail} />);

    expect(screen.getByRole("heading", { level: 1, name: "Fixture Project" })).toBeInTheDocument();
    const categoryLinks = within(screen.getByRole("list", { name: "카테고리" })).getAllByRole("link");
    expect(categoryLinks.map((link) => link.getAttribute("href"))).toEqual(["/work/branding"]);
    expect(screen.getAllByRole("img").map((image) => image.getAttribute("alt"))).toEqual([
      "fixture cover",
      "Fixture Project 이미지 2",
    ]);
    expect(screen.getByText("Fixture description")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "WORK 목록으로" })).toHaveAttribute("href", "/work");
  });

  it("설명이 없으면 설명 영역 없음", () => {
    const { container } = render(<ProjectDetail project={{ ...fixtureDetail, description: null }} />);

    expect(screen.queryByText("Fixture description")).not.toBeInTheDocument();
    expect(container.querySelector("[data-slot=description]")).toBeNull();
  });

  it("HTML처럼 보이는 설명은 해석하지 않고 줄바꿈 포함 텍스트로 표시", () => {
    const description = '<script>alert("x")</script>\n둘째 줄';
    const { container } = render(<ProjectDetail project={{ ...fixtureDetail, description }} />);

    expect(container.querySelector("[data-slot=description]")?.textContent).toBe(description);
    expect(container.querySelector("script")).toBeNull();
  });
});

describe("ProjectImage", () => {
  it("이미지 로드 실패 시 같은 비율 박스에 대체 텍스트 표시", () => {
    render(<ProjectImage image={cover} />);

    fireEvent.error(screen.getByRole("img", { name: "fixture cover" }));

    const fallback = screen.getByRole("img", { name: "fixture cover" });
    expect(fallback.tagName).toBe("DIV");
    expect(fallback).toHaveTextContent("fixture cover");
    expect(fallback).toHaveStyle({ aspectRatio: "1600 / 900" });
  });

  it("크기 정보 없는 이미지는 원본 그대로 표시하고 실패 시 4:3 박스", () => {
    render(<ProjectImage image={fixtureDetail.images[1]} />);

    const image = screen.getByRole("img", { name: "Fixture Project 이미지 2" });
    // 이미지 최적화 경로(`/_next/image`)를 거치지 않고 원본 URL 사용
    expect(image.getAttribute("src")).toMatch(/^(https?:\/\/[^/]+)?\/api\/media\/7\/file$/);
    fireEvent.error(image);

    expect(screen.getByRole("img", { name: "Fixture Project 이미지 2" })).toHaveStyle({ aspectRatio: "4 / 3" });
  });
});
