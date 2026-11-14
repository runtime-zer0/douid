import type { Metadata } from "next";

import { CategoryList } from "@/features/work";
import { getCategories } from "@/features/work/server";

export const metadata: Metadata = { title: "Work" };

export default function WorkPage() {
  return (
    <div className="flex flex-col gap-12">
      <h1 className="font-heading text-4xl tracking-tight md:text-5xl">WORK</h1>
      <CategoryList categories={getCategories()} />
    </div>
  );
}
