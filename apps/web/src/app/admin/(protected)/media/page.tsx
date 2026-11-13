import type { Metadata } from "next";

import { MediaLibrary, parseUiPage } from "@/features/admin";

export const metadata: Metadata = { title: "미디어 관리" };

export default async function AdminMediaPage({ searchParams }: PageProps<"/admin/media">) {
  const { page } = await searchParams;
  return <MediaLibrary page={parseUiPage(typeof page === "string" ? page : null)} />;
}
