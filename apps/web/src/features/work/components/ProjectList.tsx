import Link from "next/link";

import { cn } from "@/shared/lib/utils";

import { projectHref } from "../content/routes";
import type { PublicWorkSummary } from "../model/types";
import { ProjectImage } from "./ProjectImage";
import { FOCUS_RING } from "./styles";

/** 카테고리의 공개 작업물 목록, 없으면 빈 상태 */
export function ProjectList({ projects }: { projects: readonly PublicWorkSummary[] }) {
  if (projects.length === 0) {
    return (
      <div className="flex flex-col items-start gap-4 py-16">
        <p className="text-muted-foreground">아직 공개된 작업이 없습니다.</p>
        <Link href="/work" className={cn(FOCUS_RING, "underline underline-offset-4")}>
          WORK 목록으로
        </Link>
      </div>
    );
  }

  return (
    <ul className="grid gap-10 sm:grid-cols-2">
      {projects.map((project) => (
        <li key={project.slug}>
          <Link href={projectHref(project.slug)} className={cn(FOCUS_RING, "flex flex-col gap-3 hover:opacity-80")}>
            {project.thumbnail && <ProjectImage image={project.thumbnail} />}
            <span className="text-lg break-words">{project.title}</span>
            {project.summary && <span className="text-sm break-words text-muted-foreground">{project.summary}</span>}
          </Link>
        </li>
      ))}
    </ul>
  );
}
