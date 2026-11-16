import type { ChatRequestBody } from "../api/streamChat";

import { MAX_CONTEXT_CODE_POINTS, MAX_HISTORY_MESSAGES, MAX_MESSAGE_CODE_POINTS, MAX_REQUEST_BYTES } from "./limits";
import type { ChatMessage } from "./types";

type HistoryItem = ChatRequestBody["history"][number];

// 답변까지 그대로 보내는 최근 대화 쌍 수, 그 이전은 방문자 메시지만 보냄
const RECENT_PAIRS = 3;

const codePoints = (text: string) => [...text].length;
const encoder = new TextEncoder();

/** 전송 전 질문 검증, 공백만이면 `empty`, 2000 code point 초과면 `too_long` */
export function validateQuestion(text: string): "empty" | "too_long" | null {
  if (text.trim() === "") return "empty";
  if (codePoints(text) > MAX_MESSAGE_CODE_POINTS) return "too_long";
  return null;
}

/**
 * 요청 본문 구성, 질문은 원문 그대로
 * 이력은 user와 완료된 assistant 중 내용 있는 것만 쓰고, 서버의 byte·개수·code point 제한 안에서 고름
 * 최근 3쌍은 질문과 답변을 쌍 단위로 넣고, 그 이전은 방문자 메시지만 최신부터 채운 뒤 원래 순서로 보냄
 */
export function buildRequestBody(messages: ChatMessage[], question: string): ChatRequestBody {
  // 서버가 개별 2000 code point 초과 이력을 거부하므로 긴 답변은 앞부분만 남김
  const all: HistoryItem[] = messages
    .filter((message) => message.text !== "" && (message.role === "user" || message.status === "done"))
    .map((message) => ({ role: message.role, content: [...message.text].slice(0, MAX_MESSAGE_CODE_POINTS).join("") }));
  const questionCodePoints = codePoints(question);
  const fits = (history: HistoryItem[]) =>
    history.length <= MAX_HISTORY_MESSAGES &&
    history.reduce((sum, item) => sum + codePoints(item.content), questionCodePoints) <= MAX_CONTEXT_CODE_POINTS &&
    encoder.encode(JSON.stringify({ message: question, history })).length <= MAX_REQUEST_BYTES;

  let history: HistoryItem[] = [];
  let end = all.length;
  for (let pairs = 0; end > 0 && pairs < RECENT_PAIRS; pairs++) {
    const start = end >= 2 && all[end - 1].role === "assistant" && all[end - 2].role === "user" ? end - 2 : end - 1;
    const candidate = [...all.slice(start, end), ...history];
    if (!fits(candidate)) return { message: question, history };
    history = candidate;
    end = start;
  }
  for (let index = end - 1; index >= 0; index--) {
    if (all[index].role !== "user") continue;
    const candidate = [all[index], ...history];
    if (!fits(candidate)) break;
    history = candidate;
  }
  return { message: question, history };
}
