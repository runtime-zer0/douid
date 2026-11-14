import type { ChatRequestBody } from "../api/streamChat";

import { MAX_CONTEXT_CODE_POINTS, MAX_HISTORY_MESSAGES, MAX_MESSAGE_CODE_POINTS } from "./limits";
import type { ChatMessage } from "./types";

const codePoints = (text: string) => [...text].length;

/** 전송 전 질문 검증, 공백만이면 `empty`, 2000 code point 초과면 `too_long` */
export function validateQuestion(text: string): "empty" | "too_long" | null {
  if (text.trim() === "") return "empty";
  if (codePoints(text) > MAX_MESSAGE_CODE_POINTS) return "too_long";
  return null;
}

/**
 * 요청 본문 구성(research R8), 질문은 원문 그대로
 * 이력은 user와 완료된 assistant 중 내용 있는 것만, 최근 12개, 질문 포함 12000 code point 이내
 */
export function buildRequestBody(messages: ChatMessage[], question: string): ChatRequestBody {
  const candidates = messages
    .filter((message) => message.text !== "" && (message.role === "user" || message.status === "done"))
    .slice(-MAX_HISTORY_MESSAGES)
    .map((message) => ({ role: message.role, content: message.text }));

  let total = codePoints(question) + candidates.reduce((sum, item) => sum + codePoints(item.content), 0);
  while (candidates.length > 0 && total > MAX_CONTEXT_CODE_POINTS) {
    total -= codePoints(candidates.shift()!.content);
  }
  return { message: question, history: candidates };
}
