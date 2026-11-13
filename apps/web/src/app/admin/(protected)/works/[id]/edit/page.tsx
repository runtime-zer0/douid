import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { WorkForm } from "@/features/admin";

export const metadata: Metadata = { title: "작업물 편집" };

export default async function EditWorkPage({ params, searchParams }: PageProps<"/admin/works/[id]/edit">) {
  const { id } = await params;
  const { created } = await searchParams;
  // 숫자가 아닌 ID는 서버가 500으로 응답하므로 요청 전에 차단
  if (!/^\d+$/.test(id) || !Number.isSafeInteger(Number(id))) notFound();

  return <WorkForm mode="edit" id={Number(id)} created={created === "1"} />;
}
