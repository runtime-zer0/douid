import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { delay, http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/renderWithQuery";

import type { Media, Page } from "../api/types";
import { csrfHandler, fail, media, ok, page } from "../test/fixtures";
import { MediaLibrary } from "./MediaLibrary";

const router = vi.hoisted(() => ({ replace: vi.fn(), push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

beforeEach(() => router.replace.mockClear());

function libraryServer(list: () => Page<Media> = () => page([media(1)])) {
  server.use(csrfHandler, http.get("/api/media", () => ok(list())));
}

const fileOf = (name: string, type: string, size?: number) => {
  const file = new File(["x"], name, { type });
  if (size !== undefined) Object.defineProperty(file, "size", { value: size });
  return file;
};

describe("MediaLibrary", () => {
  it("파일명·MIME·바이트 크기·업로드 시각·미리보기 표시, 이미지는 서버 상대 URL", async () => {
    libraryServer();
    renderWithQuery(<MediaLibrary page={1} />);

    const item = within(await screen.findByRole("list", { name: "미디어 목록" })).getByRole("listitem");
    expect(item).toHaveTextContent("image-1.png");
    expect(item).toHaveTextContent("image/png");
    expect(item).toHaveTextContent("12,345 bytes");
    expect(item).toHaveTextContent("2026-11-13 09:00");
    // 최적화 경로(/_next/image)를 거치지 않고 서버 상대 URL을 같은 Origin에서 그대로 요청
    expect(new URL((within(item).getByRole("img") as HTMLImageElement).src).pathname).toBe("/api/media/1/file");
  });

  it.each([
    ["빈 파일", fileOf("a.png", "image/png", 0), "빈 파일"],
    ["이미지가 아닌 파일", fileOf("a.pdf", "application/pdf"), "이미지 파일만"],
    ["한도 초과", fileOf("a.png", "image/png", 10 * 1024 * 1024), "10MB"],
  ])("%s은 요청 없이 원인 표시", async (_, file, text) => {
    let posts = 0;
    libraryServer();
    server.use(http.post("/api/admin/media", () => (posts++, ok(media(2), 201))));
    const user = userEvent.setup({ applyAccept: false });
    renderWithQuery(<MediaLibrary page={1} />);

    await user.upload(screen.getByLabelText("이미지 업로드"), file);

    expect(await screen.findByRole("alert")).toHaveTextContent(text);
    expect(screen.getByRole("button", { name: "업로드" })).toBeEnabled();
    await user.click(screen.getByRole("button", { name: "업로드" }));
    expect(posts).toBe(0);
  });

  it("서버가 이미지 형식을 거부하면 원인 표시, 목록 그대로", async () => {
    libraryServer();
    server.use(http.post("/api/admin/media", () => fail(400, "INVALID_FILE_TYPE")));
    const user = userEvent.setup();
    renderWithQuery(<MediaLibrary page={1} />);
    await screen.findByText("image-1.png");

    await user.upload(screen.getByLabelText("이미지 업로드"), fileOf("b.png", "image/png"));
    await user.click(screen.getByRole("button", { name: "업로드" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미지 파일만 업로드할 수 있습니다.");
    expect(within(screen.getByRole("list", { name: "미디어 목록" })).getAllByRole("listitem")).toHaveLength(1);
  });

  it("업로드 연타에도 POST는 1회", async () => {
    let posts = 0;
    libraryServer();
    server.use(
      http.post("/api/admin/media", async () => {
        posts += 1;
        await delay(50);
        return ok(media(2, { originalFilename: "b.png" }), 201);
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<MediaLibrary page={1} />);

    await user.upload(screen.getByLabelText("이미지 업로드"), fileOf("b.png", "image/png"));
    await user.dblClick(screen.getByRole("button", { name: "업로드" }));

    expect(await screen.findByRole("status")).toHaveTextContent("b.png을 업로드했습니다.");
    expect(posts).toBe(1);
  });

  it("사용 중이면 연결 해제 후 저장 안내, 항목 유지", async () => {
    libraryServer();
    server.use(http.delete("/api/admin/media/1", () => fail(409, "MEDIA_IN_USE")));
    const user = userEvent.setup();
    renderWithQuery(<MediaLibrary page={1} />);

    await user.click(await screen.findByRole("button", { name: "image-1.png 삭제" }));
    const dialog = screen.getByRole("alertdialog");
    await user.click(within(dialog).getByRole("button", { name: "삭제" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent("연결을 해제하고 저장");
    expect(screen.getByRole("button", { name: "image-1.png 삭제", hidden: true })).toBeInTheDocument();
  });

  it("삭제 확인 후 성공하면 제거, 확인 문구에 복구 불가", async () => {
    let removed = false;
    libraryServer(() => page(removed ? [media(2)] : [media(1), media(2)]));
    server.use(http.delete("/api/admin/media/1", () => ((removed = true), ok(null))));
    const user = userEvent.setup();
    renderWithQuery(<MediaLibrary page={1} />);

    await user.click(await screen.findByRole("button", { name: "image-1.png 삭제" }));
    const dialog = screen.getByRole("alertdialog");
    expect(dialog).toHaveTextContent("복구할 수 없습니다");
    await user.click(within(dialog).getByRole("button", { name: "삭제" }));

    await waitFor(() => expect(screen.queryByText("image-1.png", { selector: "p" })).not.toBeInTheDocument());
    expect(screen.getByText("image-2.png", { selector: "p" })).toBeInTheDocument();
  });

  it("미리보기 실패 시 대체 표시, 메타데이터 유지", async () => {
    libraryServer();
    renderWithQuery(<MediaLibrary page={1} />);

    fireEvent.error(await screen.findByRole("img"));

    expect(await screen.findByText("미리보기 실패")).toBeInTheDocument();
    expect(screen.getByText("image-1.png", { selector: "p" })).toBeInTheDocument();
  });

  it("마지막 페이지의 마지막 항목을 삭제하면 이전 페이지로 이동", async () => {
    let removed = false;
    libraryServer(() => (removed ? page([], 20, 1) : page([media(21)], 21, 1)));
    server.use(http.delete("/api/admin/media/21", () => ((removed = true), ok(null))));
    const user = userEvent.setup();
    renderWithQuery(<MediaLibrary page={2} />);

    await user.click(await screen.findByRole("button", { name: "image-21.png 삭제" }));
    await user.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "삭제" }));

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/admin/media"));
  });
});
