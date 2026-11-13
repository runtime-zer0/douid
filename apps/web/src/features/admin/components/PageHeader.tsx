import type { ReactNode } from "react";

/** 관리 화면 제목 영역, 화면당 주 동작 하나를 오른쪽에 배치 */
export function PageHeader({ title, description, action }: { title: string; description?: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-x-8 gap-y-4 border-b pb-6">
      <div className="min-w-0 max-w-2xl">
        <h1 className="font-heading text-3xl leading-tight sm:text-4xl">{title}</h1>
        {description ? <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}
