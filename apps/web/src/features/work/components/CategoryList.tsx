import Link from "next/link";

import { cn } from "@/shared/lib/utils";

import type { WorkCategory } from "../content/categories";
import { categoryHref } from "../content/routes";
import { FOCUS_RING } from "./styles";

/** WORK 카테고리 목록, 번호와 정식 명칭 */
export function CategoryList({ categories }: { categories: readonly WorkCategory[] }) {
  return (
    <ul className="divide-y divide-border border-y border-border">
      {categories.map((category) => (
        <li key={category.id}>
          <Link
            href={categoryHref(category.id)}
            className={cn(FOCUS_RING, "flex items-baseline gap-6 py-6 break-words hover:opacity-70 md:py-8")}
          >
            <span className="text-sm text-muted-foreground tabular-nums">{String(category.order).padStart(2, "0")}</span>
            <span className="min-w-0 font-heading text-2xl tracking-tight md:text-4xl">{category.name}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
