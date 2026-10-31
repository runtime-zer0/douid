import Link from "next/link";

import { cn } from "@/shared/lib/utils";

import type { WorkCategory } from "../content/categories";
import type { PortfolioProject } from "../content/projects";
import { categoryHref } from "../content/routes";
import { ProjectImage } from "./ProjectImage";
import { FOCUS_RING } from "./styles";

type ProjectDetailProps = {
  project: PortfolioProject;
  /** 소속 카테고리, 표시 순서 */
  categories: readonly WorkCategory[];
};

/** 프로젝트 상세, 승인 필드만 표시하고 고객사·연도·성과 영역은 두지 않음 */
export function ProjectDetail({ project, categories }: ProjectDetailProps) {
  const primary = categories[0];

  return (
    <article className="flex flex-col gap-10">
      <nav aria-label="현재 위치" className="text-sm text-muted-foreground">
        <Link href="/work" className={FOCUS_RING}>
          WORK
        </Link>
        {primary && (
          <>
            {" / "}
            <Link href={categoryHref(primary.id)} className={FOCUS_RING}>
              {primary.name}
            </Link>
          </>
        )}
      </nav>
      <header className="flex flex-col gap-4">
        <h1 className="font-heading text-4xl tracking-tight break-words md:text-5xl">{project.title}</h1>
        <ul aria-label="카테고리" className="flex flex-wrap gap-3 text-sm">
          {categories.map((category) => (
            <li key={category.id}>
              <Link href={categoryHref(category.id)} className={cn(FOCUS_RING, "underline underline-offset-4")}>
                {category.name}
              </Link>
            </li>
          ))}
        </ul>
      </header>
      {project.description && (
        <p data-slot="description" className="max-w-2xl leading-relaxed whitespace-pre-line">
          {project.description}
        </p>
      )}
      <div className="flex flex-col gap-8">
        {project.media.map((media) => (
          <ProjectImage key={media.src} media={media} />
        ))}
      </div>
      <Link href="/work" className={cn(FOCUS_RING, "w-fit underline underline-offset-4")}>
        WORK 목록으로
      </Link>
    </article>
  );
}
