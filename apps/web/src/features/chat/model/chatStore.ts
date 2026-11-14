import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

import type { ChatStreamEvent } from "../api/events";
import { type ChatRequestBody, streamChat } from "../api/streamChat";

import type { ChatFailure } from "./failure";
import { buildRequestBody, validateQuestion } from "./history";
import { CLIENT_TIMEOUT_MS } from "./limits";
import { restoreMessages, safeSessionStorage } from "./storage";
import type { AssistantMessage, ChatMessage } from "./types";

type ChatState = {
  messages: ChatMessage[];
  isOpen: boolean;
  draft: string;
};

type ChatActions = {
  open(): void;
  close(): void;
  setDraft(text: string): void;
  /** 질문 전송, 진행 중이거나 검증 실패면 무시(FR-007) */
  send(text: string): void;
  /** 진행 중 요청 중단, 부분 답변은 `cancelled`로 유지 */
  cancel(): void;
  /** 마지막 실패·중단 답변을 지우고 직전 질문으로 1회 재요청 */
  retry(): void;
  /** 진행 중 요청 중단 후 대화와 저장값 삭제 */
  reset(): void;
};

export type ChatPhase = "idle" | "connecting" | "streaming" | "completed" | "error" | "cancelled";

// 진행 중 요청 핸들, 직렬화 대상이 아니라 store 밖에 둠(R7)
let current: { id: string; controller: AbortController; timer: ReturnType<typeof setTimeout> } | null = null;
let pageHiding = false;

const initialState: ChatState = { messages: [], isOpen: false, draft: "" };

const pendingAssistant = (): AssistantMessage => ({
  id: crypto.randomUUID(),
  role: "assistant",
  text: "",
  createdAt: Date.now(),
  status: "pending",
});

function stopCurrent() {
  if (!current) return;
  clearTimeout(current.timer);
  current.controller.abort();
  current = null;
}

export const useChatStore = create<ChatState & ChatActions>()(
  persist(
    (set, get) => {
      /** 마지막 assistant 메시지만 새 객체로 교체 */
      function updateLast(update: (message: AssistantMessage) => AssistantMessage | null) {
        set(({ messages }) => {
          const last = messages.at(-1);
          if (last?.role !== "assistant") return {};
          const next = update(last);
          return next ? { messages: [...messages.slice(0, -1), next] } : {};
        });
      }

      function applyEvent(event: Exclude<ChatStreamEvent, { type: "invalid" | "ignored" }>) {
        updateLast((message) => {
          if (message.status !== "pending" && message.status !== "streaming") return null;
          if (event.type === "delta") return { ...message, text: message.text + event.text, status: "streaming" };
          if (event.type === "completed") return { ...message, status: "done" };
          if (event.type === "recommendations") {
            return { ...message, recommendations: [...new Set([...(message.recommendations ?? []), ...event.ids])] };
          }
          return null;
        });
      }

      function fail(failure: ChatFailure) {
        updateLast((message) =>
          message.status === "pending" || message.status === "streaming" ? { ...message, status: "error", error: failure } : null,
        );
      }

      function startRequest(body: ChatRequestBody) {
        const id = crypto.randomUUID();
        const controller = new AbortController();
        const timeout = new DOMException("Client timeout", "TimeoutError");
        const timer = setTimeout(() => controller.abort(timeout), CLIENT_TIMEOUT_MS);
        current = { id, controller, timer };
        // 취소·새 대화 이후 남은 이벤트와 Promise 후속 처리는 ID 비교로 버림
        const isCurrent = () => current?.id === id;
        const finish = () => {
          clearTimeout(timer);
          current = null;
        };

        streamChat(body, {
          signal: controller.signal,
          onEvent: (event) => {
            if (isCurrent()) applyEvent(event);
          },
        }).then(
          (outcome) => {
            if (!isCurrent() || pageHiding) return;
            finish();
            if (!outcome.ok) fail(outcome.failure);
          },
          () => {
            if (!isCurrent() || pageHiding) return;
            finish();
            fail({ kind: controller.signal.reason === timeout ? "timeout" : "network", code: null });
          },
        );
      }

      return {
        ...initialState,
        open: () => set({ isOpen: true }),
        close: () => set({ isOpen: false }),
        setDraft: (draft) => set({ draft }),
        send(text) {
          if (current || validateQuestion(text)) return;
          const body = buildRequestBody(get().messages, text);
          const question: ChatMessage = { id: crypto.randomUUID(), role: "user", text, createdAt: Date.now() };
          set(({ messages }) => ({ messages: [...messages, question, pendingAssistant()], draft: "" }));
          startRequest(body);
        },
        cancel() {
          if (!current) return;
          stopCurrent();
          updateLast((message) =>
            message.status === "pending" || message.status === "streaming" ? { ...message, status: "cancelled" } : null,
          );
        },
        retry() {
          const { messages } = get();
          const [question, failed] = messages.slice(-2);
          if (current || question?.role !== "user" || failed?.role !== "assistant") return;
          if (failed.status !== "error" && failed.status !== "cancelled") return;
          const body = buildRequestBody(messages.slice(0, -2), question.text);
          set({ messages: [...messages.slice(0, -1), pendingAssistant()] });
          startRequest(body);
        },
        reset() {
          stopCurrent();
          set({ messages: [], draft: "" });
          useChatStore.persist.clearStorage();
        },
      };
    },
    {
      name: "douid-ai-chat",
      version: 1,
      storage: createJSONStorage(() => safeSessionStorage),
      partialize: (state) => ({ messages: state.messages }),
      skipHydration: true,
      // 이전 버전 형식이 없어 변환 없이 빈 대화로 시작
      migrate: () => ({ messages: [] }),
      merge: (persisted, state) => ({
        ...state,
        messages: restoreMessages((persisted as { messages?: unknown } | undefined)?.messages),
      }),
    },
  ),
);

/** 마지막 메시지 상태로 계산한 응답 단계(FR-005) */
export function selectPhase({ messages }: ChatState): ChatPhase {
  const last = messages.at(-1);
  if (!last) return "idle";
  if (last.role === "user") return "connecting";
  switch (last.status) {
    case "pending":
      return "connecting";
    case "streaming":
      return "streaming";
    case "done":
      return "completed";
    default:
      return last.status;
  }
}

/** 진행 중 요청 존재 여부, 테스트·UI 보조 */
export const hasActiveRequest = () => current !== null;

let hydrationRequested = false;

/**
 * 현재 탭 대화 복원, 페이지 로드당 1회
 * 공개 레이아웃이 다시 마운트돼도 진행 중 답변을 저장값으로 덮어쓰지 않도록 재실행 안 함
 */
export function hydrateChatOnce() {
  if (hydrationRequested) return;
  hydrationRequested = true;
  // 새로고침·이탈로 브라우저가 요청을 끊으면 network 실패 대신 진행 중 상태로 남겨 복원 시 interrupted 표시(FR-027)
  // 요청 취소가 pagehide보다 먼저 올 수 있어 beforeunload도 함께 사용
  const markHiding = () => {
    pageHiding = true;
  };
  window.addEventListener("beforeunload", markHiding);
  window.addEventListener("pagehide", markHiding);
  window.addEventListener("pageshow", () => {
    pageHiding = false;
  });
  void useChatStore.persist.rehydrate();
}

/** 테스트 격리용 초기화, 진행 중 요청 중단·store·sessionStorage·복원 여부 비움 */
export function resetChatForTest() {
  hydrationRequested = false;
  pageHiding = false;
  stopCurrent();
  useChatStore.setState(initialState);
  window.sessionStorage.clear();
}
