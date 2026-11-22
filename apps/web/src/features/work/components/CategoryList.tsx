import Link from "next/link";

import { cn } from "@/shared/lib/utils";

import { categoryHref } from "../content/routes";
import type { WorkCategoryView } from "../model/types";
import { FOCUS_RING } from "./styles";

/** WORK 카테고리 목록, 표시 순서 번호와 정식 명칭 */
export function CategoryList({ categories }: { categories: readonly WorkCategoryView[] }) {
  return (
    <ul className="divide-y divide-border border-y border-border">
      {categories.map((category, index) => (
        <li key={category.slug}>
          <Link
            href={categoryHref(category.slug)}
            className={cn(FOCUS_RING, "flex items-baseline gap-6 py-6 break-words hover:opacity-70 md:py-8")}
          >
            <span className="text-sm text-muted-foreground tabular-nums">{String(index + 1).padStart(2, "0")}</span>
            <span className="min-w-0 font-heading text-2xl tracking-tight md:text-4xl">{category.name}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
