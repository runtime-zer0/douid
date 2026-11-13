"use client";

import { ArrowDown, ArrowUp } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";

import type { WorkMediaRole } from "../api/types";
import type { DraftMediaItem } from "../model/workDraft";
import { MediaThumb } from "./MediaThumb";

const ROLE_LABELS: Record<WorkMediaRole, string> = {
  THUMBNAIL: "대표 이미지",
  DETAIL_IMAGE: "상세 이미지",
  ATTACHMENT: "첨부",
};

type WorkMediaEditorProps = {
  items: DraftMediaItem[];
  altTextErrors?: Record<string, string>;
  onRole: (key: string, role: WorkMediaRole) => void;
  onAltText: (key: string, value: string) => void;
  onMove: (key: string, direction: -1 | 1) => void;
  onRemove: (key: string) => void;
};

/**
 * 연결 이미지의 역할·순서·대체 텍스트 편집, 드래그 없이 버튼으로 순서 변경
 * 이동 후에도 같은 항목의 같은 버튼에 포커스를 유지해 키보드로 연속 이동 가능
 */
export function WorkMediaEditor({ items, altTextErrors, onRole, onAltText, onMove, onRemove }: WorkMediaEditorProps) {
  const listRef = useRef<HTMLOListElement>(null);
  const [focusTarget, setFocusTarget] = useState<{ key: string; direction: -1 | 1 } | null>(null);

  useEffect(() => {
    if (!focusTarget || !listRef.current) return;
    const { key, direction } = focusTarget;
    const button =
      listRef.current.querySelector<HTMLButtonElement>(`[data-move="${key}:${direction}"]:not(:disabled)`) ??
      listRef.current.querySelector<HTMLButtonElement>(`[data-move="${key}:${-direction}"]`);
    button?.focus();
    setFocusTarget(null);
  }, [focusTarget, items]);

  if (items.length === 0) {
    return <p className="border border-dashed px-4 py-6 text-sm text-muted-foreground">연결한 이미지가 없습니다. 이미지 없이도 저장할 수 있습니다.</p>;
  }

  return (
    <ol ref={listRef} className="grid gap-px border bg-border">
      {items.map((item, index) => {
        const number = index + 1;
        const altId = `alt-${item.key}`;
        const altError = altTextErrors?.[item.key];
        return (
          <li key={item.key} className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-4 bg-background p-4 sm:grid-cols-[auto_1fr_auto]">
            <div className="relative">
              <MediaThumb src={item.fileUrl} alt={item.altText ?? ""} size={96} className="size-20 sm:size-24" />
              <span className="absolute top-0 left-0 bg-background px-1.5 text-xs tabular-nums">{number}</span>
            </div>
            <div className="grid min-w-0 gap-4">
              <div className="flex flex-wrap items-center gap-3">
                <Label htmlFor={`role-${item.key}`} className="sr-only">
                  {number}번 이미지 역할
                </Label>
                <select
                  id={`role-${item.key}`}
                  value={item.role}
                  onChange={(event) => onRole(item.key, event.target.value as WorkMediaRole)}
                  className="h-9 border-b border-input bg-background pr-6 text-foreground text-sm outline-none focus-visible:border-ring"
                >
                  {(Object.keys(ROLE_LABELS) as WorkMediaRole[]).map((role) => (
                    <option key={role} value={role}>
                      {ROLE_LABELS[role]}
                    </option>
                  ))}
                </select>
                {item.role === "THUMBNAIL" ? <span className="text-xs font-semibold">대표</span> : null}
                <span translate="no" className="text-xs text-muted-foreground tabular-nums">이미지 ID {item.mediaId}</span>
              </div>
              <div className="grid gap-1">
                <Label htmlFor={altId} className="text-[0.6875rem]">
                  대체 텍스트
                </Label>
                <Input
                  id={altId}
                  value={item.altText ?? ""}
                  onChange={(event) => onAltText(item.key, event.target.value)}
                  aria-invalid={altError ? true : undefined}
                  aria-describedby={altError ? `${altId}-error` : undefined}
                  className="h-9"
                />
                {altError ? (
                  <p id={`${altId}-error`} className="text-xs text-destructive">
                    {altError}
                  </p>
                ) : null}
              </div>
            </div>
            <div className="col-span-2 flex flex-wrap gap-1 max-sm:[&_[data-size=xs]]:h-9 sm:col-span-1 sm:flex-col sm:items-stretch">
              <Button
                type="button"
                variant="outline"
                size="xs"
                data-move={`${item.key}:-1`}
                disabled={index === 0}
                aria-label={`${number}번 이미지 위로`}
                onClick={() => {
                  onMove(item.key, -1);
                  setFocusTarget({ key: item.key, direction: -1 });
                }}
              >
                <ArrowUp aria-hidden="true" />
                위로
              </Button>
              <Button
                type="button"
                variant="outline"
                size="xs"
                data-move={`${item.key}:1`}
                disabled={index === items.length - 1}
                aria-label={`${number}번 이미지 아래로`}
                onClick={() => {
                  onMove(item.key, 1);
                  setFocusTarget({ key: item.key, direction: 1 });
                }}
              >
                <ArrowDown aria-hidden="true" />
                아래로
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="xs"
                aria-label={`${number}번 이미지 연결 해제`}
                onClick={() => onRemove(item.key)}
              >
                연결 해제
              </Button>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
