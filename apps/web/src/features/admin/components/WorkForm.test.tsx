import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { delay, http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/renderWithQuery";

import { adminKeys } from "../api/queryKeys";
import type { AdminWorkDetail } from "../api/types";
import { categories, csrfHandler, fail, media, ok, page, workDetail } from "../test/fixtures";
import { WorkForm } from "./WorkForm";

const router = vi.hoisted(() => ({ replace: vi.fn(), push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

beforeEach(() => router.replace.mockClear());

const originalBody = {
  title: workDetail.title,
  slug: workDetail.slug,
  summary: null,
  description: workDetail.description,
  categoryId: 1,
  visibility: "VISIBLE",
  mediaItems: [
    { mediaId: 10, role: "THUMBNAIL", sortOrder: 5, altText: "로고" },
    { mediaId: 11, role: "DETAIL_IMAGE", sortOrder: 9, altText: null },
  ],
};

function editServer(detail: () => AdminWorkDetail | Response = () => workDetail) {
  server.use(
    csrfHandler,
    http.get("/api/admin/categories", () => ok(categories)),
    http.get("/api/admin/works/7", () => {
      const value = detail();
      return value instanceof Response ? value : ok(value);
    }),
  );
}

const titleInput = () => screen.getByLabelText(/^제목/);
const save = () => screen.getByRole("button", { name: /변경 저장|작업물 만들기|저장 중/ });

describe("WorkForm 편집", () => {
  it("상세 응답 전에는 폼을 렌더링하지 않음", async () => {
    server.use(
      http.get("/api/admin/categories", () => ok(categories)),
      http.get("/api/admin/works/7", async () => {
        await delay("infinite");
        return ok(workDetail);
      }),
    );
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    expect(screen.getByRole("status")).toHaveTextContent("작업물을 불러오는 중");
    expect(screen.queryByLabelText(/^제목/)).not.toBeInTheDocument();
  });

  it("상세 응답으로 모든 필드와 이미지 항목 채움", async () => {
    editServer();
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    expect(await screen.findByDisplayValue(workDetail.title)).toBeInTheDocument();
    expect(screen.getByLabelText(/^슬러그/)).toHaveValue("seoul-coffee");
    expect(screen.getByLabelText("상세 설명")).toHaveValue("첫 줄\n둘째 줄");
    expect(screen.getByLabelText("공개")).toBeChecked();
    await waitFor(() => expect(screen.getByLabelText("카테고리")).toHaveValue("1"));
    expect(screen.getAllByLabelText("대체 텍스트").map((input) => (input as HTMLInputElement).value)).toEqual(["로고", ""]);
    expect(screen.getByText("대표")).toBeInTheDocument();
  });

  it("제목만 바꿔 저장하면 PUT 본문은 원본과 title만 다름", async () => {
    let body: unknown;
    editServer();
    server.use(
      http.put("/api/admin/works/7", async ({ request }) => {
        body = await request.json();
        return ok({ id: 7 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    await user.clear(await screen.findByDisplayValue(workDetail.title));
    await user.type(titleInput(), "새 제목");
    await user.click(save());

    await waitFor(() => expect(body).toEqual({ ...originalBody, title: "새 제목" }));
  });

  it("저장 연타에도 PUT은 1회", async () => {
    let puts = 0;
    editServer();
    server.use(
      http.put("/api/admin/works/7", async () => {
        puts += 1;
        await delay(50);
        return ok({ id: 7 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    await user.type(await screen.findByDisplayValue(workDetail.title), "!");
    await user.dblClick(save());

    expect(await screen.findByText("저장했습니다.")).toBeInTheDocument();
    expect(puts).toBe(1);
  });

  it("슬러그 중복이면 슬러그 필드에 오류 연결, 다른 입력 유지", async () => {
    editServer();
    server.use(http.put("/api/admin/works/7", () => fail(409, "WORK_SLUG_DUPLICATE")));
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    await user.type(await screen.findByDisplayValue(workDetail.title), " 2");
    await user.click(save());

    const slug = screen.getByLabelText(/^슬러그/);
    await waitFor(() => expect(slug).toHaveAttribute("aria-invalid", "true"));
    expect(slug).toHaveAccessibleDescription(/이미 사용 중인 슬러그입니다/);
    expect(titleInput()).toHaveValue(`${workDetail.title} 2`);
    expect(screen.getAllByLabelText("대체 텍스트")).toHaveLength(2);
  });

  it.each([
    ["500", () => fail(500, "INTERNAL_ERROR"), "서버 오류"],
    ["네트워크 오류", () => HttpResponse.error(), "반영 여부를 확인"],
  ])("%s 후 입력 유지와 안내", async (_, resolver, text) => {
    editServer();
    server.use(http.put("/api/admin/works/7", resolver));
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    await user.type(await screen.findByDisplayValue(workDetail.title), " 수정");
    await user.click(save());

    expect(await screen.findByRole("alert")).toHaveTextContent(text);
    expect(titleInput()).toHaveValue(`${workDetail.title} 수정`);
    expect(screen.getByText("저장하지 않은 변경이 있습니다.")).toBeInTheDocument();
  });

  it("상세 404면 대상 없음 안내와 목록 링크, PUT 없음", async () => {
    let puts = 0;
    editServer(() => fail(404, "WORK_NOT_FOUND"));
    server.use(http.put("/api/admin/works/7", () => (puts++, ok({ id: 7 }))));
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    expect(await screen.findByText(/작업물을 찾을 수 없습니다/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "작업물 목록으로" })).toHaveAttribute("href", "/admin/works");
    expect(puts).toBe(0);
  });

  it("저장 성공 후 상세를 다시 받아 기준값 갱신, 미저장 표시 사라짐", async () => {
    let current = workDetail;
    editServer(() => current);
    server.use(
      http.put("/api/admin/works/7", async ({ request }) => {
        const body = (await request.json()) as { title: string };
        current = { ...workDetail, title: body.title, updatedAt: "2026-11-13T12:00:00" };
        return ok({ id: 7 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    await user.type(await screen.findByDisplayValue(workDetail.title), "!");
    expect(screen.getByText("저장하지 않은 변경이 있습니다.")).toBeInTheDocument();
    await user.click(save());

    expect(await screen.findByText("저장했습니다.")).toBeInTheDocument();
    expect(screen.queryByText("저장하지 않은 변경이 있습니다.")).not.toBeInTheDocument();
    expect(titleInput()).toHaveValue(`${workDetail.title}!`);
  });

  it("백그라운드 재조회가 편집 중 입력을 덮어쓰지 않음", async () => {
    let current = workDetail;
    editServer(() => current);
    const user = userEvent.setup();
    const { queryClient } = renderWithQuery(<WorkForm mode="edit" id={7} />);

    await user.type(await screen.findByDisplayValue(workDetail.title), " 작성 중");
    current = { ...workDetail, title: "다른 관리자가 바꾼 제목" };
    await queryClient.refetchQueries({ queryKey: adminKeys.work(7) });

    expect(titleInput()).toHaveValue(`${workDetail.title} 작성 중`);
  });

  it("슬러그를 바꾸면 기존 주소가 바뀐다는 안내", async () => {
    editServer();
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="edit" id={7} />);

    await user.type(await screen.findByDisplayValue("seoul-coffee"), "-2");

    expect(screen.getByText(/기존 주소가 바뀌고/)).toBeInTheDocument();
  });

  it("생성 직후 진입이면 공개 조건 미충족 안내", async () => {
    editServer(() => ({ ...workDetail, visibility: "HIDDEN", category: null, mediaItems: [] }));
    renderWithQuery(<WorkForm mode="edit" id={7} created />);

    const notice = (await screen.findByText(/작업물을 만들었습니다/)).closest('[role="status"]');
    expect(notice).toHaveTextContent("서버 공개 조건을 충족하지 않습니다");
  });
});

describe("WorkForm 생성", () => {
  function createServer() {
    let mediaGets = 0;
    server.use(
      csrfHandler,
      http.get("/api/admin/categories", () => ok(categories)),
      http.get("/api/media", () => {
        mediaGets += 1;
        return ok(page([media(30)]));
      }),
    );
    return { mediaGets: () => mediaGets };
  }

  async function fillRequired(user: ReturnType<typeof userEvent.setup>) {
    await user.type(titleInput(), "새 작업물");
    await user.type(screen.getByLabelText(/^슬러그/), "new-work");
  }

  it("초기값은 비공개·미분류·이미지 없음", () => {
    createServer();
    renderWithQuery(<WorkForm mode="create" />);

    expect(screen.getByLabelText("비공개")).toBeChecked();
    expect(screen.getByLabelText("카테고리")).toHaveValue("");
    expect(screen.getByText(/연결한 이미지가 없습니다/)).toBeInTheDocument();
  });

  it("이미지·카테고리 없이 저장하면 POST 본문은 categoryId null·mediaItems []이고 편집 화면으로 이동", async () => {
    let body: unknown;
    createServer();
    server.use(
      http.post("/api/admin/works", async ({ request }) => {
        body = await request.json();
        return ok({ id: 9 }, 201);
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="create" />);

    await fillRequired(user);
    await user.click(save());

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/admin/works/9/edit?created=1"));
    expect(body).toEqual({
      title: "새 작업물",
      slug: "new-work",
      summary: null,
      description: null,
      categoryId: null,
      visibility: "HIDDEN",
      mediaItems: [],
    });
  });

  it("공백만 있는 제목이면 요청하지 않고 필드 오류", async () => {
    let posts = 0;
    createServer();
    server.use(http.post("/api/admin/works", () => (posts++, ok({ id: 9 }, 201))));
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="create" />);

    await user.type(titleInput(), "   ");
    await user.click(save());

    expect(titleInput()).toHaveAccessibleDescription("제목을 입력해 주세요.");
    expect(posts).toBe(0);
  });

  it("업로드 성공 후 저장이 500이면 폼과 연결 항목 유지, 미디어 삭제 요청 없음", async () => {
    let mediaDeletes = 0;
    createServer();
    server.use(
      http.post("/api/admin/media", () => ok(media(55, { originalFilename: "new.png" }), 201)),
      http.delete("/api/admin/media/:id", () => (mediaDeletes++, ok(null))),
      http.post("/api/admin/works", () => fail(500, "INTERNAL_ERROR")),
    );
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="create" />);

    await fillRequired(user);
    await user.click(screen.getByRole("button", { name: "이미지 추가" }));
    const dialog = await screen.findByRole("dialog", { name: "이미지 추가" });
    await user.upload(within(dialog).getByLabelText("이미지 업로드"), new File(["png"], "new.png", { type: "image/png" }));
    await user.click(within(dialog).getByRole("button", { name: "업로드" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.getByText("이미지 ID 55")).toBeInTheDocument();

    await user.click(save());

    expect(await screen.findByRole("alert")).toHaveTextContent("서버 오류");
    expect(screen.getByText("이미지 ID 55")).toBeInTheDocument();
    expect(titleInput()).toHaveValue("새 작업물");
    expect(save()).toBeEnabled();
    expect(mediaDeletes).toBe(0);
  });

  it("POST 네트워크 오류면 성공 여부 불명확 안내, 재전송 없음", async () => {
    let posts = 0;
    createServer();
    server.use(http.post("/api/admin/works", () => (posts++, HttpResponse.error())));
    const user = userEvent.setup();
    renderWithQuery(<WorkForm mode="create" />);

    await fillRequired(user);
    await user.click(save());

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("만들어졌는지 확인하지 못했습니다");
    expect(within(alert).getByRole("link", { name: "작업물 목록" })).toBeInTheDocument();
    expect(posts).toBe(1);
  });

  it.each([
    ["WORK_THUMBNAIL_DUPLICATE", 400, "대표 이미지는 하나만"],
    ["MEDIA_NOT_FOUND", 404, "이미지 일부를 찾을 수 없습니다"],
  ])("%s면 이미지 구성 영역에 안내하고 라이브러리 재조회", async (code, status, text) => {
    const counter = createServer();
    server.use(http.post("/api/admin/works", () => fail(status, code)));
    const user = userEvent.setup();
    const { queryClient } = renderWithQuery(<WorkForm mode="create" />);
    queryClient.setQueryData(adminKeys.media(0), page([media(30)]));
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await fillRequired(user);
    await user.click(save());

    expect(await screen.findAllByText(new RegExp(text))).not.toHaveLength(0);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: adminKeys.mediaAll });
    expect(counter.mediaGets()).toBe(0);
  });
});
