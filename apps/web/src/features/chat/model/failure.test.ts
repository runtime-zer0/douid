import { describe, expect, it } from "vitest";

import { type ChatFailure, describeFailure } from "./failure";

describe("describeFailure", () => {
  it.each<[ChatFailure, string, boolean]>([
    [{ kind: "http", code: "INVALID_INPUT" }, "질문을 확인해 주세요", false],
    [{ kind: "http", code: "PAYLOAD_TOO_LARGE" }, "대화가 너무 길어요. 새 대화를 시작해 주세요", false],
    [{ kind: "http", code: "AI_RATE_LIMITED", retryAfter: 30 }, "요청이 많아요. 잠시 후 다시 시도해 주세요", true],
    [{ kind: "http", code: "AI_BUSY" }, "요청이 많아요. 잠시 후 다시 시도해 주세요", true],
    [{ kind: "http", code: "AI_UNAVAILABLE" }, "지금은 AI 상담을 이용할 수 없어요", true],
    [{ kind: "http", code: "AI_TIMEOUT" }, "응답 시간이 초과됐어요", true],
    [{ kind: "timeout", code: null }, "응답 시간이 초과됐어요", true],
    [{ kind: "stream", code: "STREAM_TIMEOUT" }, "응답 시간이 초과됐어요", true],
    [{ kind: "stream", code: "STREAM_LIMIT_EXCEEDED" }, "답변이 너무 길어 중단됐어요. 질문을 나눠 보내 주세요", false],
    [{ kind: "network", code: null }, "연결이 끊겼어요", true],
    [{ kind: "interrupted", code: null }, "새로고침으로 답변이 중단됐어요", true],
    [{ kind: "http", code: null }, "답변을 처리하지 못했어요", true],
    [{ kind: "http", code: "UNKNOWN" }, "답변을 처리하지 못했어요", true],
    [{ kind: "stream", code: "AI_PROVIDER_ERROR" }, "답변을 처리하지 못했어요", true],
    [{ kind: "protocol", code: "INCOMPLETE_STREAM" }, "답변을 처리하지 못했어요", true],
  ])("%o → 문구·재시도", (failure, message, retryable) => {
    expect(describeFailure(failure)).toEqual({ message, retryable });
  });
});
