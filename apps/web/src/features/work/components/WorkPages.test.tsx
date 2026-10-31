import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { WORK_CATEGORIES } from "../content/categories";
import type { PortfolioProject } from "../content/projects";
import { CategoryList } from "./CategoryList";
import { ProjectDetail } from "./ProjectDetail";
import { ProjectImage } from "./ProjectImage";
import { ProjectList } from "./ProjectList";

// 템플릿 검증 전용 fixture, 배포 데이터 아님
const fixtureProject: PortfolioProject = {
  id: "fixture-project",
  title: "Fixture Project",
  categoryIds: ["WORK_CAT_01_BRANDING", "WORK_CAT_02_PACKAGING_DESIGN"],
  status: "published",
  description: "Fixture description",
  media: [{ src: "/work/fixture-project/cover.jpg", width: 1600, height: 900, alt: "fixture cover", rightsCleared: true }],
};
const fixtureCategories = WORK_CATEGORIES.slice(0, 2);

describe("CategoryList", () => {
  it("6개 카테고리를 순서대로 정식 명칭 링크로 표시", () => {
    render(<CategoryList categories={WORK_CATEGORIES} />);

    const links = within(screen.getByRole("list")).getAllByRole("link");
    expect(links.map((link) => link.getAttribute("href"))).toEqual([
      "/work/branding",
      "/work/packaging-design",
      "/work/product-detail-page-design",
      "/work/presentation-design",
      "/work/marketing-design",
      "/work/editorial-design",
    ]);
    WORK_CATEGORIES.forEach((category, index) => expect(links[index]).toHaveTextContent(category.name));
  });
});

describe("ProjectList", () => {
  it("공개 프로젝트가 없으면 빈 상태와 WORK 복귀 링크만 표시", () => {
    render(<ProjectList projects={[]} />);

    expect(screen.getByText("아직 공개된 작업이 없습니다.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "WORK 목록으로" })).toHaveAttribute("href", "/work");
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("여러 카테고리에 속한 프로젝트는 어느 목록에서든 같은 상세 경로", () => {
    const { unmount } = render(<ProjectList projects={[fixtureProject]} />);
    const fromBranding = screen.getByRole("link", { name: /Fixture Project/ }).getAttribute("href");
    unmount();

    render(<ProjectList projects={[fixtureProject]} />);
    expect(screen.getByRole("link", { name: /Fixture Project/ })).toHaveAttribute("href", fromBranding!);
    expect(fromBranding).toBe("/work/projects/fixture-project");
  });
});

describe("ProjectDetail", () => {
  it("제목, 소속 카테고리 링크, 대체 텍스트 있는 이미지, 설명, WORK 복귀 링크 표시", () => {
    render(<ProjectDetail project={fixtureProject} categories={fixtureCategories} />);

    expect(screen.getByRole("heading", { level: 1, name: "Fixture Project" })).toBeInTheDocument();
    const categoryLinks = within(screen.getByRole("list", { name: "카테고리" })).getAllByRole("link");
    expect(categoryLinks.map((link) => link.getAttribute("href"))).toEqual(["/work/branding", "/work/packaging-design"]);
    expect(screen.getByRole("img", { name: "fixture cover" })).toBeInTheDocument();
    expect(screen.getByText("Fixture description")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "WORK 목록으로" })).toHaveAttribute("href", "/work");
  });

  it("설명이 없으면 설명 영역 없음", () => {
    const { container } = render(
      <ProjectDetail project={{ ...fixtureProject, description: null }} categories={fixtureCategories} />,
    );

    expect(screen.queryByText("Fixture description")).not.toBeInTheDocument();
    expect(container.querySelector("[data-slot=description]")).toBeNull();
  });
});

describe("ProjectImage", () => {
  it("이미지 로드 실패 시 같은 비율 박스에 대체 텍스트 표시", () => {
    render(<ProjectImage media={fixtureProject.media[0]} />);

    fireEvent.error(screen.getByRole("img", { name: "fixture cover" }));

    const fallback = screen.getByRole("img", { name: "fixture cover" });
    expect(fallback.tagName).toBe("DIV");
    expect(fallback).toHaveTextContent("fixture cover");
    expect(fallback).toHaveStyle({ aspectRatio: "1600 / 900" });
  });
});
