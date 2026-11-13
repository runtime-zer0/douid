import type { ReactNode } from "react";

import { cn } from "@/shared/lib/utils";

type NoticeProps = {
  tone: "alert" | "status";
  children: ReactNode;
  className?: string;
  id?: string;
  /** 제출 실패 후 포커스를 옮길 대상이면 true */
  focusable?: boolean;
};

/** 변경 결과 안내, 실패는 `role="alert"`, 성공은 `role="status"` */
export function Notice({ tone, children, className, id, focusable }: NoticeProps) {
  return (
    <div
      id={id}
      role={tone}
      tabIndex={focusable ? -1 : undefined}
      className={cn(
        "border-l-2 px-4 py-3 text-sm leading-relaxed outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
        tone === "alert" ? "border-destructive bg-destructive/5 text-foreground" : "border-foreground bg-muted",
        className,
      )}
    >
      {children}
    </div>
  );
}
