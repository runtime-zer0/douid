import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";

import { CHAT_ENDPOINT } from "../content";
import { resetChatForTest } from "../model/chatStore";
import type { PublicProject } from "../model/types";
import { EOF_NO_TERMINAL, HTTP_503, jsonFailure, NORMAL, NORMAL_ANSWER, sseResponse, WITH_RECOMMENDATIONS } from "../test/fixtures";

import { ChatWidget } from "./ChatWidget";

const content = vi.hoisted(() => ({ kakao: null as string | null }));
vi.mock("../content", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../content")>()),
  get KAKAO_CHANNEL_URL() {
    return content.kakao;
  },
}));

const encode = (text: string) => new TextEncoder().encode(text);
const delta = (text: string) => encode(`event: message_delta\ndata: ${JSON.stringify({ text })}\n\n`);
const COMPLETED = encode('event: completed\ndata: {"status":"success"}\n\n');

let requestCount = 0;

function respond(response: () => Response | Promise<Response>) {
  server.use(
    http.post(CHAT_ENDPOINT, () => {
      requestCount += 1;
      return response();
    }),
  );
}

async function openChat(projects: PublicProject[] = []) {
  const user = userEvent.setup();
  render(<ChatWidget projects={projects} />);
  await user.click(screen.getByRole("button", { name: "AI 상담 열기" }));
  return { user, dialog: screen.getByRole("dialog", { name: "DOUID AI 상담" }) };
}

async function ask(user: ReturnType<typeof userEvent.setup>, question = "질문") {
  await user.type(screen.getByRole("textbox", { name: "질문 입력" }), `${question}{Enter}`);
}

beforeEach(() => {
  resetChatForTest();
  requestCount = 0;
  content.kakao = null;
});

describe("기본 흐름", () => {
  it("처음에는 런처만 있고 열면 입력창에 포커스", async () => {
    render(<ChatWidget projects={[]} />);

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getAllByRole("button")).toHaveLength(1);

    await userEvent.click(screen.getByRole("button", { name: "AI 상담 열기" }));

    expect(screen.getByRole("dialog", { name: "DOUID AI 상담" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "질문 입력" })).toHaveFocus();
    expect(screen.getByRole("button", { name: "AI 상담 닫기" })).toHaveAttribute("aria-expanded", "true");
  });

  it("Enter 전송, 델타 점진 표시 후 완료 알림", async () => {
    respond(() => sseResponse([delta("패키지"), delta(" 디자인"), COMPLETED], { delayMs: 30 }));
    const { user } = await openChat();

    await ask(user, "패키지 작업 보여줘");

    expect(screen.getByText("패키지 작업 보여줘")).toBeInTheDocument();
    expect(await screen.findByText("패키지")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("답변을 준비하고 있어요");
    expect(await screen.findByText("패키지 디자인")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("답변이 완료됐어요"));
    expect(requestCount).toBe(1);
  });

  it("Shift+Enter는 줄바꿈, 전송 없음", async () => {
    respond(() => sseResponse([NORMAL]));
    const { user } = await openChat();
    const input = screen.getByRole("textbox", { name: "질문 입력" });

    await user.type(input, "첫 줄{Shift>}{Enter}{/Shift}둘째 줄");

    expect(input).toHaveValue("첫 줄\n둘째 줄");
    expect(requestCount).toBe(0);
  });

  it("공백만 보내면 안내, 요청 없음", async () => {
    respond(() => sseResponse([NORMAL]));
    const { user } = await openChat();

    await user.type(screen.getByRole("textbox", { name: "질문 입력" }), "   ");
    await user.click(screen.getByRole("button", { name: "보내기" }));

    expect(screen.getByText("질문을 입력해 주세요")).toBeInTheDocument();
    expect(requestCount).toBe(0);
  });

  it("2001자면 안내와 보내기 비활성", async () => {
    await openChat();

    fireEvent.change(screen.getByRole("textbox", { name: "질문 입력" }), { target: { value: "가".repeat(2001) } });

    expect(screen.getByText("2000자 이하로 입력해 주세요")).toBeInTheDocument();
    expect(screen.getByText("2001 / 2000")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "보내기" })).toBeDisabled();
  });

  it("스크립트가 담긴 답변은 텍스트로 표시", async () => {
    respond(() => sseResponse([delta("<script>alert(1)</script><b>굵게</b>"), COMPLETED]));
    const { user, dialog } = await openChat();

    await ask(user);

    expect(await screen.findByText("<script>alert(1)</script><b>굵게</b>")).toBeInTheDocument();
    expect(dialog.querySelector("script, b")).toBeNull();
  });
});

describe("비정상 종료", () => {
  it("완료 없는 EOF는 미완료 배지와 오류 문구, 완료 알림 없음", async () => {
    respond(() => sseResponse([EOF_NO_TERMINAL]));
    const { user, dialog } = await openChat();

    await ask(user);

    expect(await within(dialog).findByText("완료되지 않은 답변")).toBeInTheDocument();
    expect(within(dialog).getByText("부분 답변")).toBeInTheDocument();
    expect(within(dialog).getByRole("list")).toHaveTextContent("답변을 처리하지 못했어요");
    expect(screen.getByRole("status")).toHaveTextContent("답변을 처리하지 못했어요");
    expect(screen.queryByText("답변이 완료됐어요")).toBeNull();
  });
});

describe("세션 유지", () => {
  it("닫았다 다시 열어도 대화 유지, 저장값도 유지", async () => {
    respond(() => sseResponse([NORMAL]));
    const { user } = await openChat();
    await ask(user);
    await screen.findByText(NORMAL_ANSWER);
    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("답변이 완료됐어요"));

    await user.click(screen.getByRole("button", { name: "닫기" }));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getByRole("button", { name: "AI 상담 열기" })).toHaveFocus();
    expect(window.sessionStorage.getItem("douid-ai-chat")).toContain(NORMAL_ANSWER);

    await user.click(screen.getByRole("button", { name: "AI 상담 열기" }));

    expect(screen.getByText("질문")).toBeInTheDocument();
    expect(screen.getByText(NORMAL_ANSWER)).toBeInTheDocument();
  });

  it("마운트 시 저장된 대화 복원", async () => {
    window.sessionStorage.setItem(
      "douid-ai-chat",
      JSON.stringify({
        state: { messages: [{ id: "u1", role: "user", text: "예전 질문", createdAt: 1 }] },
        version: 1,
      }),
    );

    await openChat();

    expect(await screen.findByText("예전 질문")).toBeInTheDocument();
  });
});

describe("중단·새 대화", () => {
  it("진행 중에는 생성 중단, 누르면 중단 배지와 알림", async () => {
    respond(() => sseResponse([delta("앞"), delta("뒤"), COMPLETED], { delayMs: 50 }));
    const { user, dialog } = await openChat();

    await ask(user);
    expect(screen.queryByRole("button", { name: "보내기" })).toBeNull();
    await within(dialog).findByText("앞");
    await user.click(screen.getByRole("button", { name: "생성 중단" }));

    expect(within(dialog).getByRole("list")).toHaveTextContent("생성을 중단했어요");
    expect(screen.getByRole("status")).toHaveTextContent("생성을 중단했어요");
    expect(screen.getByRole("button", { name: "보내기" })).toBeInTheDocument();
  });

  it("새 대화는 메시지를 지우고 입력에 포커스", async () => {
    respond(() => sseResponse([NORMAL]));
    const { user, dialog } = await openChat();
    await ask(user);
    await screen.findByText(NORMAL_ANSWER);

    await user.click(screen.getByRole("button", { name: "새 대화" }));

    expect(within(dialog).queryByRole("list")).toBeNull();
    expect(screen.getByRole("textbox", { name: "질문 입력" })).toHaveFocus();
    expect(screen.queryByRole("button", { name: "새 대화" })).toBeNull();
  });
});

describe("추천", () => {
  it("카탈로그에 있는 추천만 링크", async () => {
    respond(() => sseResponse([WITH_RECOMMENDATIONS]));
    const { user } = await openChat([{ id: "published-a", title: "공개 작업 A" }]);

    await ask(user);

    const list = await screen.findByRole("list", { name: "추천 작업물" });
    expect(within(list).getAllByRole("link")).toHaveLength(1);
    expect(within(list).getByRole("link", { name: "공개 작업 A" })).toHaveAttribute("href", "/work/projects/published-a");
  });

  it("답변 텍스트 속 경로는 링크가 아님, 빈 카탈로그면 목록 없음", async () => {
    respond(() =>
      sseResponse([delta("/work/projects/fake 참고"), encode('event: recommendations\ndata: {"portfolioIds":["fake"]}\n\n'), COMPLETED]),
    );
    const { user, dialog } = await openChat([]);

    await ask(user);

    expect(await screen.findByText("/work/projects/fake 참고")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("답변이 완료됐어요"));
    expect(within(dialog).queryByRole("link")).toBeNull();
    expect(screen.queryByRole("list", { name: "추천 작업물" })).toBeNull();
  });
});

describe("실패·재시도", () => {
  it("503은 안내와 다시 시도, 재시도는 요청 1건 추가하고 질문 수 유지", async () => {
    let call = 0;
    respond(() => (++call === 1 ? HTTP_503() : sseResponse([NORMAL])));
    const { user } = await openChat();

    await ask(user);
    expect(await screen.findByRole("button", { name: "다시 시도" })).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toHaveTextContent("지금은 AI 상담을 이용할 수 없어요");

    await user.click(screen.getByRole("button", { name: "다시 시도" }));

    expect(await screen.findByText(NORMAL_ANSWER)).toBeInTheDocument();
    expect(requestCount).toBe(2);
    expect(screen.getAllByText("질문")).toHaveLength(1);
  });

  it("INVALID_INPUT은 재시도 없음", async () => {
    respond(() => jsonFailure(400, "INVALID_INPUT"));
    const { user } = await openChat();

    await ask(user);

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("질문을 확인해 주세요"));
    expect(screen.queryByRole("button", { name: "다시 시도" })).toBeNull();
  });
});

describe("상담 링크", () => {
  it("승인 URL이 없으면 링크 없음", async () => {
    await openChat();

    expect(screen.queryByRole("link", { name: "카카오톡으로 상담하기" })).toBeNull();
  });

  it.each(["http://pf.kakao.com/x", "javascript:alert(1)"])("https가 아닌 %s는 미노출", async (url) => {
    content.kakao = url;
    await openChat();

    expect(screen.queryByRole("link", { name: "카카오톡으로 상담하기" })).toBeNull();
  });

  it("https URL이면 새 탭 링크와 대체 안내", async () => {
    content.kakao = "https://pf.kakao.com/_test";
    await openChat();

    const link = screen.getByRole("link", { name: "카카오톡으로 상담하기" });
    expect(link).toHaveAttribute("href", "https://pf.kakao.com/_test");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    expect(screen.getByRole("link", { name: "Contact 페이지" })).toHaveAttribute("href", "/contact");
  });
});

describe("접근성", () => {
  it("패널 안에서 Esc로 닫고 런처로 포커스", async () => {
    const { user } = await openChat();

    await user.keyboard("{Escape}");

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getByRole("button", { name: "AI 상담 열기" })).toHaveFocus();
  });

  it("과거 메시지를 읽는 중이면 위치 유지, 새 답변 보기로 하단 이동", async () => {
    respond(() => sseResponse([delta("첫"), delta(" 둘"), delta(" 셋"), COMPLETED], { delayMs: 40 }));
    const { user, dialog } = await openChat();
    await ask(user);
    await within(dialog).findByText("첫");

    const scroller = within(dialog).getByRole("list").parentElement!;
    Object.defineProperty(scroller, "scrollHeight", { configurable: true, value: 1000 });
    Object.defineProperty(scroller, "clientHeight", { configurable: true, value: 300 });
    scroller.scrollTop = 100;
    fireEvent.scroll(scroller);

    const showNew = await screen.findByRole("button", { name: "새 답변 보기" });
    expect(scroller.scrollTop).toBe(100);

    await user.click(showNew);

    expect(scroller.scrollTop).toBe(1000);
    expect(screen.queryByRole("button", { name: "새 답변 보기" })).toBeNull();
  });
});
