"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/components/ui/dialog";

import { LoginForm } from "./LoginForm";
import { useReturnFocus } from "./useReturnFocus";

type ReauthDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
};

/** 세션 만료 시 화면 위 재로그인, 자동 저장은 하지 않음 */
export function ReauthDialog({ open, onOpenChange, onSuccess }: ReauthDialogProps) {
  const returnFocus = useReturnFocus();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent {...returnFocus}>
        <DialogHeader>
          <DialogTitle className="font-heading text-xl">다시 로그인</DialogTitle>
          <DialogDescription>
            세션이 만료되었습니다. 작성 중인 내용은 이 화면에 남아 있으니, 로그인한 뒤 저장을 다시 눌러 주세요.
          </DialogDescription>
        </DialogHeader>
        <LoginForm onSuccess={onSuccess} submitLabel="다시 로그인" />
      </DialogContent>
    </Dialog>
  );
}
