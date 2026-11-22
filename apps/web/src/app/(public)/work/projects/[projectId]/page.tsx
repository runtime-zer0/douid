import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { ProjectDetail } from "@/features/work";
import { getWork } from "@/features/work/server";

// generateStaticParams 없음: 빈 목록이라도 두면 SSG로 분류돼 서버 모드의 요청 시점 조회(connection)가 실패

export async function generateMetadata({ params }: PageProps<"/work/projects/[projectId]">): Promise<Metadata> {
  const project = await getWork((await params).projectId);
  return { title: project?.title };
}

export default async function ProjectPage({ params }: PageProps<"/work/projects/[projectId]">) {
  const project = await getWork((await params).projectId);
  if (!project) notFound();

  return <ProjectDetail project={project} />;
}
