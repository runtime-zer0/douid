import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { ProjectList } from "@/features/work";
import { getCategories, getCategoryBySlug, getPublishedProjects } from "@/features/work/server";

export const dynamicParams = false;

export function generateStaticParams() {
  return getCategories().map((category) => ({ category: category.slug }));
}

export async function generateMetadata({ params }: PageProps<"/work/[category]">): Promise<Metadata> {
  const category = getCategoryBySlug((await params).category);
  return { title: category?.name };
}

export default async function WorkCategoryPage({ params }: PageProps<"/work/[category]">) {
  const category = getCategoryBySlug((await params).category);
  if (!category) notFound();

  return (
    <div className="flex flex-col gap-10">
      <nav aria-label="현재 위치" className="text-sm text-muted-foreground">
        <Link href="/work" className="rounded-sm outline-offset-4 focus-visible:outline-2 focus-visible:outline-current">
          WORK
        </Link>
        {" / "}
        <span aria-current="page">{category.name}</span>
      </nav>
      <header className="flex flex-col gap-4">
        <h1 className="font-heading text-4xl tracking-tight break-words md:text-5xl">{category.name}</h1>
        {category.summary && <p className="max-w-2xl text-muted-foreground">{category.summary}</p>}
      </header>
      <ProjectList projects={getPublishedProjects(category.id)} />
    </div>
  );
}
