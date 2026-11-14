/** 답변 실패 원인, 화면 문구는 `describeFailure`로만 생성(서버 message 미사용) */
export type ChatFailure = {
  kind: "http" | "network" | "stream" | "protocol" | "timeout" | "interrupted";
  code: string | null;
  retryAfter?: number;
};

const DEFAULT = { message: "답변을 처리하지 못했어요", retryable: true };

/** 실패 → 안내 문구·재시도 가능 여부 (contracts/chat-ui.md §3) */
export function describeFailure({ kind, code }: ChatFailure): { message: string; retryable: boolean } {
  switch (kind) {
    case "network":
      return { message: "연결이 끊겼어요", retryable: true };
    case "timeout":
      return { message: "응답 시간이 초과됐어요", retryable: true };
    case "interrupted":
      return { message: "새로고침으로 답변이 중단됐어요", retryable: true };
    case "stream":
      if (code === "STREAM_TIMEOUT") return { message: "응답 시간이 초과됐어요", retryable: true };
      if (code === "STREAM_LIMIT_EXCEEDED") return { message: "답변이 너무 길어 중단됐어요. 질문을 나눠 보내 주세요", retryable: false };
      return DEFAULT;
    case "http":
      switch (code) {
        case "INVALID_INPUT":
          return { message: "질문을 확인해 주세요", retryable: false };
        case "PAYLOAD_TOO_LARGE":
          return { message: "대화가 너무 길어요. 새 대화를 시작해 주세요", retryable: false };
        case "AI_RATE_LIMITED":
        case "AI_BUSY":
          return { message: "요청이 많아요. 잠시 후 다시 시도해 주세요", retryable: true };
        case "AI_UNAVAILABLE":
          return { message: "지금은 AI 상담을 이용할 수 없어요", retryable: true };
        case "AI_TIMEOUT":
          return { message: "응답 시간이 초과됐어요", retryable: true };
        default:
          return DEFAULT;
      }
    default:
      return DEFAULT;
  }
}
