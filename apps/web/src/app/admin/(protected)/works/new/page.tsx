import type { Metadata } from "next";

import { WorkForm } from "@/features/admin";

export const metadata: Metadata = { title: "새 작업물" };

export default function NewWorkPage() {
  return <WorkForm mode="create" />;
}
