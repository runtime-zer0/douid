import type { StateStorage } from "zustand/middleware";

import type { ChatFailure } from "./failure";
import type { AssistantMessage, ChatMessage } from "./types";

/** sessionStorage 접근 래퍼, 접근 거부·용량 초과는 무시하고 메모리 대화 유지 */
export const safeSessionStorage: StateStorage = {
  getItem(name) {
    try {
      return window.sessionStorage.getItem(name);
    } catch {
      return null;
    }
  },
  setItem(name, value) {
    try {
      window.sessionStorage.setItem(name, value);
    } catch {
      // 저장 실패는 화면에 영향 없음
    }
  },
  removeItem(name) {
    try {
      window.sessionStorage.removeItem(name);
    } catch {
      // 삭제 실패도 무시
    }
  },
};

const INTERRUPTED: ChatFailure = { kind: "interrupted", code: null };
const FAILURE_KINDS = new Set(["http", "network", "stream", "protocol", "timeout", "interrupted"]);
const SETTLED = new Set(["done", "error", "cancelled"]);

const isRecord = (value: unknown): value is Record<string, unknown> => typeof value === "object" && value !== null;

function restoreFailure(value: unknown): ChatFailure {
  if (!isRecord(value) || typeof value.kind !== "string" || !FAILURE_KINDS.has(value.kind)) return INTERRUPTED;
  return { kind: value.kind as ChatFailure["kind"], code: typeof value.code === "string" ? value.code : null };
}

function restoreMessage(value: unknown): ChatMessage | null {
  if (!isRecord(value)) return null;
  const { id, role, text, createdAt } = value;
  if (typeof id !== "string" || typeof text !== "string" || typeof createdAt !== "number") return null;
  if (role === "user") return { id, role, text, createdAt };
  if (role !== "assistant") return null;

  const message: AssistantMessage = { id, role, text, createdAt, status: "error", error: INTERRUPTED };
  // 진행 중이던 답변·알 수 없는 상태는 새로고침 중단으로 표시(FR-027)
  if (typeof value.status === "string" && SETTLED.has(value.status)) {
    message.status = value.status as AssistantMessage["status"];
    message.error = message.status === "error" ? restoreFailure(value.error) : undefined;
    if (message.error === undefined) delete message.error;
  }
  if (Array.isArray(value.recommendations) && value.recommendations.every((item) => typeof item === "string")) {
    message.recommendations = value.recommendations;
  }
  return message;
}

/** 저장된 대화 검증·복원, 형태가 하나라도 틀리면 빈 대화 */
export function restoreMessages(persisted: unknown): ChatMessage[] {
  if (!Array.isArray(persisted)) return [];
  const messages = persisted.map(restoreMessage);
  return messages.every((message) => message !== null) ? (messages as ChatMessage[]) : [];
}
