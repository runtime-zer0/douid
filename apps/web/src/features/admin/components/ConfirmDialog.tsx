"use client";

import type { ReactNode } from "react";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/components/ui/alert-dialog";

import { Notice } from "./Notice";
import { useReturnFocus } from "./useReturnFocus";

type ConfirmDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  pendingLabel?: string;
  pending?: boolean;
  destructive?: boolean;
  /** 서버 거부 안내, 다이얼로그를 닫지 않고 표시 */
  error?: string | null;
  onConfirm: () => void;
};

/**
 * 삭제·공개 범위 변경 전 확인
 * 확인을 눌러도 바로 닫지 않고 호출자가 서버 성공 후 닫음, 닫히면 Radix가 열기 전 포커스로 복귀
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  pendingLabel = "처리 중…",
  pending = false,
  destructive = false,
  error,
  onConfirm,
}: ConfirmDialogProps) {
  const returnFocus = useReturnFocus();
  return (
    <AlertDialog open={open} onOpenChange={(next) => !pending && onOpenChange(next)}>
      <AlertDialogContent {...returnFocus}>
        <AlertDialogHeader>
          <AlertDialogTitle className="normal-case tracking-normal">{title}</AlertDialogTitle>
          <AlertDialogDescription asChild>
            <div className="space-y-2 text-left">{description}</div>
          </AlertDialogDescription>
        </AlertDialogHeader>
        {error ? <Notice tone="alert">{error}</Notice> : null}
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending}>취소</AlertDialogCancel>
          <AlertDialogAction
            variant={destructive ? "destructive" : "default"}
            disabled={pending}
            onClick={(event) => {
              event.preventDefault();
              onConfirm();
            }}
          >
            {pending ? pendingLabel : confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
