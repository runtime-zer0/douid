import Link from "next/link";

import { buttonVariants } from "@/shared/components/ui/button-variants";
import { cn } from "@/shared/lib/utils";

import { totalPages } from "../model/pagination";

type PagerProps = {
  page: number;
  totalElements: number;
  size: number;
  /** URL 쿼리로 이동하는 목록 */
  hrefFor?: (page: number) => string;
  /** 내부 상태로 이동하는 목록(다이얼로그 등) */
  onPageChange?: (page: number) => void;
  label: string;
};

/** 페이지 이동과 "n / m 페이지, 전체 k건" 표시, 현재 페이지를 전체 목록으로 오인하지 않게 함 */
export function Pager({ page, totalElements, size, hrefFor, onPageChange, label }: PagerProps) {
  const last = totalPages(totalElements, size);
  const control = (target: number, text: string, disabled: boolean) => {
    const className = cn(buttonVariants({ variant: "outline", size: "sm" }), disabled && "pointer-events-none opacity-40");
    if (disabled) {
      return (
        <span aria-disabled="true" className={className}>
          {text}
        </span>
      );
    }
    if (hrefFor) {
      return (
        <Link href={hrefFor(target)} className={className}>
          {text}
        </Link>
      );
    }
    return (
      <button type="button" className={className} onClick={() => onPageChange?.(target)}>
        {text}
      </button>
    );
  };

  return (
    <nav aria-label={label} className="flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm text-muted-foreground tabular-nums">
        {page} / {last} 페이지, 전체 {totalElements}건
      </p>
      <div className="flex gap-2">
        {control(page - 1, "이전", page <= 1)}
        {control(page + 1, "다음", page >= last)}
      </div>
    </nav>
  );
}
