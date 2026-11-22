import Link from "next/link";

import { cn } from "@/shared/lib/utils";

import { categoryHref } from "../content/routes";
import type { PublicWorkDetail } from "../model/types";
import { ProjectImage } from "./ProjectImage";
import { FOCUS_RING } from "./styles";

/** 작업물 상세, 승인 필드만 표시하고 고객사·연도·성과 영역은 두지 않음 */
export function ProjectDetail({ project }: { project: PublicWorkDetail }) {
  const { category } = project;

  return (
    <article className="flex flex-col gap-10">
      <nav aria-label="현재 위치" className="text-sm text-muted-foreground">
        <Link href="/work" className={FOCUS_RING}>
          WORK
        </Link>
        {" / "}
        <Link href={categoryHref(category.slug)} className={FOCUS_RING}>
          {category.name}
        </Link>
      </nav>
      <header className="flex flex-col gap-4">
        <h1 className="font-heading text-4xl tracking-tight break-words md:text-5xl">{project.title}</h1>
        <ul aria-label="카테고리" className="flex flex-wrap gap-3 text-sm">
          <li>
            <Link href={categoryHref(category.slug)} className={cn(FOCUS_RING, "underline underline-offset-4")}>
              {category.name}
            </Link>
          </li>
        </ul>
      </header>
      {project.description && (
        <p data-slot="description" className="max-w-2xl leading-relaxed break-words whitespace-pre-line">
          {project.description}
        </p>
      )}
      <div className="flex flex-col gap-8">
        {project.images.map((image) => (
          <ProjectImage key={image.src} image={image} />
        ))}
      </div>
      <Link href="/work" className={cn(FOCUS_RING, "w-fit underline underline-offset-4")}>
        WORK 목록으로
      </Link>
    </article>
  );
}
