import { cn } from "@/shared/lib/utils";

import type { Exposure } from "../model/exposure";

const LABELS: Record<Exposure, { setting: string; detail: string | null }> = {
  hidden: { setting: "비공개", detail: null },
  uncategorized: { setting: "공개 설정", detail: "카테고리가 없어 공개 조회에서 제외" },
  categoryHidden: { setting: "공개 설정", detail: "비공개 카테고리라 공개 조회에서 제외" },
  eligible: { setting: "공개 설정", detail: "서버 공개 조건 충족" },
  unknown: { setting: "공개 설정", detail: "공개 가능 여부를 확인하지 못함" },
};

/**
 * 작업물 공개 설정과 서버 공개 조건을 함께 표시
 * 현재 공개 사이트는 정적 콘텐츠라 "게시됨"으로 표시하지 않음, 의미는 색이 아니라 텍스트로 전달
 */
export function ExposureBadge({ exposure, className }: { exposure: Exposure; className?: string }) {
  const { setting, detail } = LABELS[exposure];
  return (
    <span data-exposure={exposure} className={cn("inline-flex flex-col gap-0.5 text-sm", className)}>
      <span className="inline-flex items-center gap-2 font-medium">
        <span
          aria-hidden="true"
          className={cn(
            "size-2 shrink-0 border border-foreground",
            exposure === "eligible" && "bg-foreground",
            exposure === "hidden" && "border-muted-foreground",
          )}
        />
        {setting}
      </span>
      {detail ? (
        <span className={cn("pl-4 text-xs", exposure === "eligible" ? "text-foreground" : "text-muted-foreground")}>{detail}</span>
      ) : null}
    </span>
  );
}
