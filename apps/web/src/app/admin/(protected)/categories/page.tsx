import type { Metadata } from "next";

import { CategoryManager } from "@/features/admin";

export const metadata: Metadata = { title: "카테고리 관리" };

export default function AdminCategoriesPage() {
  return <CategoryManager />;
}
