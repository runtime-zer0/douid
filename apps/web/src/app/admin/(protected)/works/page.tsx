import type { Metadata } from "next";

import { parseUiPage, WorkList } from "@/features/admin";

export const metadata: Metadata = { title: "작업물 관리" };

export default async function AdminWorksPage({ searchParams }: PageProps<"/admin/works">) {
  const { page } = await searchParams;
  return <WorkList page={parseUiPage(typeof page === "string" ? page : null)} />;
}
