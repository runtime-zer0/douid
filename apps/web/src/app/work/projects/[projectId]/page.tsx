import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { ProjectDetail } from "@/features/work";
import { getProjectCategories, getPublishedProject, getPublishedProjects } from "@/features/work/server";

export const dynamicParams = false;

export function generateStaticParams() {
  return getPublishedProjects().map((project) => ({ projectId: project.id }));
}

export async function generateMetadata({ params }: PageProps<"/work/projects/[projectId]">): Promise<Metadata> {
  const project = getPublishedProject((await params).projectId);
  return { title: project?.title };
}

export default async function ProjectPage({ params }: PageProps<"/work/projects/[projectId]">) {
  const project = getPublishedProject((await params).projectId);
  if (!project) notFound();

  return <ProjectDetail project={project} categories={getProjectCategories(project)} />;
}
