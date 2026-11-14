import type { ChatFailure } from "./failure";

export type AssistantStatus = "pending" | "streaming" | "done" | "error" | "cancelled";

export type UserMessage = { id: string; role: "user"; text: string; createdAt: number };

export type AssistantMessage = {
  id: string;
  role: "assistant";
  text: string;
  createdAt: number;
  status: AssistantStatus;
  error?: ChatFailure;
  recommendations?: string[];
};

export type ChatMessage = UserMessage | AssistantMessage;

/** 추천 대조용 공개 작업물, `(public)` 레이아웃이 공개 카탈로그에서 생성 */
export type PublicProject = { id: string; title: string };
