import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import mappingJson from "../config/DOUID_SCROLL_MAPPING.json";
import manifestJson from "../config/DOUID_WEB_GROUP_MANIFEST.json";
import { buildCinematicTable, type ManifestSource, type MappingSource } from "../core/mapping.server";
import { resolveFrame } from "../core/resolveFrame";
import type { PlayerView } from "../playback/cinematicPlayer";
import { CinematicScroll } from "./CinematicScroll";

const mocks = vi.hoisted(() => ({
  onView: null as ((view: PlayerView) => void) | null,
  onBeforeReload: null as ((groupIndex: number) => void) | null,
  player: { setTarget: vi.fn(), resume: vi.fn(), retry: vi.fn(), getSnapshot: vi.fn(), dispose: vi.fn() },
  staticSeeker: { setTarget: vi.fn(), retry: vi.fn(), getSnapshot: vi.fn(), dispose: vi.fn() },
  loop: {
    preload: vi.fn(),
    enter: vi.fn(),
    exit: vi.fn(),
    pauseForHidden: vi.fn(),
    resume: vi.fn(),
    isVisible: vi.fn(() => false),
    dispose: vi.fn(),
  },
  onLoopVisible: null as ((visible: boolean) => void) | null,
}));

vi.mock("../playback/cinematicPlayer", () => ({
  createCinematicPlayer: vi.fn(
    (_videos, _groups, options: { onView: (view: PlayerView) => void; onBeforeReload?: (groupIndex: number) => void }) => {
      mocks.onView = options.onView;
      mocks.onBeforeReload = options.onBeforeReload ?? null;
      return mocks.player;
    },
  ),
}));

vi.mock("../playback/loopPlayer", () => ({
  createLoopPlayer: vi.fn((_video, _src, options: { onVisibleChange: (visible: boolean) => void }) => {
    mocks.onLoopVisible = options.onVisibleChange;
    return mocks.loop;
  }),
}));

vi.mock("../playback/groupSeeker", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../playback/groupSeeker")>()),
  createGroupSeeker: vi.fn(() => mocks.staticSeeker),
}));

const table = buildCinematicTable(mappingJson as MappingSource, manifestJson as ManifestSource, "/media/cinematic");
const MARK = <span>ending-logo</span>;
const VIEWPORT = 1000;
// 메인 15vh + 엔딩 4vh, 메인 진행률은 메인 구간 기준이라 기대 프레임 계산은 PHASE 01과 동일
const MAIN_HEIGHT = 15 * VIEWPORT;
const ENDING_START = MAIN_HEIGHT - VIEWPORT;
const ENDING_END = 19 * VIEWPORT - VIEWPORT;

let frameQueue: FrameRequestCallback[] = [];
const flushFrames = () =>
  act(() => {
    const queued = frameQueue;
    frameQueue = [];
    queued.forEach((callback) => callback(0));
  });

let staticMatches = false;
const mediaListeners = new Set<() => void>();
const setStaticMode = (matches: boolean) =>
  act(() => {
    staticMatches = matches;
    mediaListeners.forEach((listener) => listener());
  });

beforeEach(() => {
  frameQueue = [];
  staticMatches = false;
  mediaListeners.clear();
  vi.stubGlobal("requestAnimationFrame", (callback: FrameRequestCallback) => frameQueue.push(callback));
  vi.stubGlobal("cancelAnimationFrame", () => {});
  vi.stubGlobal("matchMedia", (query: string) => ({
    get matches() {
      return staticMatches;
    },
    media: query,
    addEventListener: (_: string, listener: () => void) => mediaListeners.add(listener),
    removeEventListener: (_: string, listener: () => void) => mediaListeners.delete(listener),
  }));
  window.innerHeight = VIEWPORT;
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  vi.clearAllMocks();
  window.scrollY = 0;
});

/** 스크롤 위치 설정, 섹션 top은 문서 0 기준이고 높이는 19vh 배수 */
function scrollTo(scrollY: number) {
  window.scrollY = scrollY;
  const section = screen.getByRole("region", { name: "DOUID 시네마틱" });
  vi.spyOn(section, "getBoundingClientRect").mockImplementation(
    () => ({ top: -window.scrollY, height: 19 * window.innerHeight }) as DOMRect,
  );
}

const expectedTarget = (scrollY: number) => resolveFrame(table, scrollY / (MAIN_HEIGHT - VIEWPORT));
const scrollAndFlush = (scrollY: number) => {
  scrollTo(scrollY);
  window.dispatchEvent(new Event("scroll"));
  flushFrames();
};
const endingScrollY = (e: number) => ENDING_START + e * (ENDING_END - ENDING_START);

describe("CinematicScroll 위치 복원 (US2)", () => {
  it("마운트 직후 현재 스크롤 위치의 목표를 player에 전달", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollTo(7000);
    flushFrames();

    expect(mocks.player.setTarget).toHaveBeenLastCalledWith(expectedTarget(7000));
  });

  it("같은 프레임에서 scroll만 반복되면 다시 전달하지 않음", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollTo(7000);
    flushFrames();
    window.dispatchEvent(new Event("scroll"));
    flushFrames();

    expect(mocks.player.setTarget).toHaveBeenCalledTimes(1);
  });

  it.each([
    ["resize", () => window.dispatchEvent(new Event("resize"))],
    ["visibilitychange", () => document.dispatchEvent(new Event("visibilitychange"))],
    ["pageshow", () => window.dispatchEvent(new Event("pageshow"))],
  ])("%s 후 같은 위치라도 목표를 다시 전달", (_, trigger) => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollTo(7000);
    flushFrames();

    trigger();
    flushFrames();

    expect(mocks.player.setTarget).toHaveBeenCalledTimes(2);
    expect(mocks.player.setTarget).toHaveBeenLastCalledWith(expectedTarget(7000));
  });

  it("스크롤 위치가 바뀌면 다음 rAF에서 새 목표 전달", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollTo(0);
    flushFrames();

    scrollTo(12000);
    window.dispatchEvent(new Event("scroll"));
    window.dispatchEvent(new Event("scroll"));
    flushFrames();

    expect(mocks.player.setTarget).toHaveBeenCalledTimes(2);
    expect(mocks.player.setTarget).toHaveBeenLastCalledWith(expectedTarget(12000));
  });

  it("창 높이가 바뀌면 직전 진행률 위치로 스크롤을 복원해 같은 프레임 유지", () => {
    const scrollToSpy = vi.fn((options: ScrollToOptions) => {
      window.scrollY = options.top!;
    });
    vi.stubGlobal("scrollTo", scrollToSpy);
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollTo(7000);
    flushFrames();
    const before = mocks.player.setTarget.mock.lastCall?.[0];

    // 1000 → 800, 메인 구간 15000 → 12000, scrollY는 그대로 7000
    window.innerHeight = 800;
    window.dispatchEvent(new Event("resize"));
    flushFrames();

    expect(scrollToSpy).toHaveBeenCalledWith({ top: 0.5 * (12000 - 800), behavior: "instant" });
    expect(mocks.player.setTarget.mock.lastCall?.[0]).toEqual(before);

    // 복원 후 같은 위치에서 발생한 scroll 이벤트도 같은 프레임 유지
    window.dispatchEvent(new Event("scroll"));
    flushFrames();
    expect(mocks.player.setTarget.mock.lastCall?.[0]).toEqual(before);
  });

  it("시네마틱 구간 밖이면 창 높이가 바뀌어도 스크롤을 옮기지 않음", () => {
    const scrollToSpy = vi.fn();
    vi.stubGlobal("scrollTo", scrollToSpy);
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollTo(20000);
    flushFrames();

    window.innerHeight = 800;
    window.dispatchEvent(new Event("resize"));
    flushFrames();

    expect(scrollToSpy).not.toHaveBeenCalled();
  });

  it("탭 복귀·bfcache 복원·창 focus 시 player의 끊긴 seek 복구", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    flushFrames();

    document.dispatchEvent(new Event("visibilitychange"));
    window.dispatchEvent(new Event("pageshow"));
    window.dispatchEvent(new Event("focus"));

    expect(mocks.player.resume).toHaveBeenCalledTimes(3);
  });

  it("예약된 rAF 콜백이 버려져도 이후 scroll이 계속 반영됨", () => {
    const now = vi.spyOn(performance, "now").mockReturnValue(0);
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollTo(3000);
    flushFrames();

    // 탭 전환 중 Safari가 예약 콜백을 실행하지 않고 버린 상황
    scrollTo(7000);
    window.dispatchEvent(new Event("scroll"));
    frameQueue = [];

    now.mockReturnValue(100);
    window.dispatchEvent(new Event("scroll"));
    expect(mocks.player.setTarget).toHaveBeenCalledTimes(1);

    now.mockReturnValue(400);
    act(() => {
      window.dispatchEvent(new Event("scroll"));
    });
    expect(mocks.player.setTarget).toHaveBeenLastCalledWith(expectedTarget(7000));
    now.mockRestore();
  });

  it("언마운트 시 player 정리", () => {
    const { unmount } = render(<CinematicScroll table={table} endingMark={MARK} />);
    unmount();

    expect(mocks.player.dispose).toHaveBeenCalled();
  });
});

describe("CinematicScroll 멈춤 복구 중 화면 유지 (US3·US4)", () => {
  const presentedView: PlayerView = { visibleGroupIndex: 0, phase: "presented", error: null };

  function setupCover() {
    const drawImage = vi.fn();
    vi.spyOn(HTMLCanvasElement.prototype, "getContext").mockReturnValue({ drawImage } as unknown as CanvasRenderingContext2D);
    const { container } = render(<CinematicScroll table={table} endingMark={MARK} />);
    container.querySelectorAll("video").forEach((video) => {
      Object.defineProperty(video, "videoWidth", { value: 3840 });
      Object.defineProperty(video, "videoHeight", { value: 2160 });
    });
    act(() => mocks.onView?.(presentedView));
    const canvas = container.querySelector("canvas")!;
    return { canvas, drawImage };
  }

  const setVisibility = (state: DocumentVisibilityState) =>
    Object.defineProperty(document, "visibilityState", { configurable: true, get: () => state });

  afterEach(() => setVisibility("visible"));

  it("화면 그룹을 다시 불러오기 직전 마지막 프레임으로 즉시 덮고, 새 프레임 표시 후 걷어냄", () => {
    mocks.player.getSnapshot.mockReturnValue({ view: presentedView });
    const { canvas, drawImage } = setupCover();
    expect(canvas.style.visibility).toBe("hidden");

    // React 렌더를 거치지 않고 같은 호출 안에서 반영
    mocks.onBeforeReload?.(0);
    expect(drawImage).toHaveBeenCalledTimes(1);
    expect(canvas).toHaveAttribute("width", "3840");
    expect(canvas.style.visibility).toBe("visible");

    act(() => mocks.onView?.({ ...presentedView, phase: "media_loading" }));
    expect(canvas.style.visibility).toBe("visible");

    act(() => mocks.onView?.(presentedView));
    expect(canvas.style.visibility).toBe("hidden");
  });

  it("화면에 없는 그룹을 다시 불러올 때는 덮지 않음", () => {
    mocks.player.getSnapshot.mockReturnValue({ view: presentedView });
    const { canvas, drawImage } = setupCover();

    mocks.onBeforeReload?.(2);

    expect(drawImage).not.toHaveBeenCalled();
    expect(canvas.style.visibility).toBe("hidden");
  });

  it("탭이 숨겨질 때 미리 덮고, 복귀 후 다시 불러오기 때는 이미 덮은 프레임을 덮어쓰지 않음", () => {
    mocks.player.getSnapshot.mockReturnValue({ view: presentedView });
    const { canvas, drawImage } = setupCover();

    setVisibility("hidden");
    document.dispatchEvent(new Event("visibilitychange"));
    expect(drawImage).toHaveBeenCalledTimes(1);
    expect(canvas.style.visibility).toBe("visible");

    setVisibility("visible");
    document.dispatchEvent(new Event("visibilitychange"));
    mocks.onBeforeReload?.(0);
    expect(drawImage).toHaveBeenCalledTimes(1);
    expect(canvas.style.visibility).toBe("visible");

    act(() => mocks.onView?.({ ...presentedView, phase: "seeking" }));
    act(() => mocks.onView?.(presentedView));
    expect(canvas.style.visibility).toBe("hidden");
  });
});

describe("CinematicScroll 로딩·오류 (US4)", () => {
  it("첫 프레임 표시 전에는 로딩 안내", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);

    expect(screen.getByText("시네마틱을 불러오는 중")).toHaveAttribute("aria-live", "polite");
  });

  it("오류 시 alert, 다시 시도, 건너뛰기 제공", async () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    act(() => mocks.onView?.({ visibleGroupIndex: 0, phase: "error", error: { kind: "media_error", groupIndex: 1 } }));

    const alert = screen.getByRole("alert");
    expect(alert).toHaveTextContent("시네마틱 영상을 불러오지 못했습니다.");

    await userEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(mocks.player.retry).toHaveBeenCalledTimes(1);

    const skipLinks = screen.getAllByRole("link", { name: "시네마틱 건너뛰기" });
    expect(skipLinks.length).toBeGreaterThanOrEqual(2);
    skipLinks.forEach((link) => expect(link).toHaveAttribute("href", "#site-footer"));
  });
});

describe("CinematicScroll 모드 (US5)", () => {
  it("기본은 스크롤 모드, 건너뛰기 링크는 푸터로 이동하고 자리표시 화면은 없음", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);

    const section = screen.getByRole("region", { name: "DOUID 시네마틱" });
    expect(section).toHaveAttribute("data-mode", "scroll");
    expect(section).toHaveClass("h-[1900vh]");
    expect(screen.getByRole("link", { name: "시네마틱 건너뛰기" })).toHaveAttribute("href", "#site-footer");
    expect(screen.queryByText("Coming soon")).not.toBeInTheDocument();
  });

  it("움직임 감소 또는 coarse pointer면 정적 모드, 쿼리 변경 시 다시 전환", () => {
    staticMatches = true;
    render(<CinematicScroll table={table} endingMark={MARK} />);
    const section = screen.getByRole("region", { name: "DOUID 시네마틱" });

    expect(section).toHaveAttribute("data-mode", "static");
    expect(section).toHaveClass("h-[200svh]");
    expect(screen.getByTestId("ending-mark")).toHaveTextContent("ending-logo");
    expect(screen.getByTestId("ending-mark")).not.toHaveStyle({ opacity: "0" });
    expect(screen.queryByTestId("ending-curtain")).not.toBeInTheDocument();
    expect(section.querySelectorAll("video")).toHaveLength(1);
    expect(mocks.staticSeeker.setTarget).toHaveBeenCalledWith(table.webFrame[3088], table.ptsTicks[3088]);
    expect(mocks.player.setTarget).not.toHaveBeenCalled();

    setStaticMode(false);
    expect(section).toHaveAttribute("data-mode", "scroll");
    expect(mocks.staticSeeker.dispose).toHaveBeenCalled();
  });
});

describe("CinematicScroll 엔딩 (US2·US3)", () => {
  it("엔딩 구간 어디서든 메인 목표는 마지막 프레임 3088 유지", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollAndFlush(ENDING_START);
    const last = mocks.player.setTarget.mock.lastCall?.[0];
    expect(last.masterFrame).toBe(3088);

    [0.3, 0.8, 1].forEach((e) => scrollAndFlush(endingScrollY(e)));
    expect(mocks.player.setTarget).toHaveBeenCalledTimes(1);
  });

  it("메인 구간 끝 직전에 L01 미리 받기", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollAndFlush(0.9 * ENDING_START);
    expect(mocks.loop.preload).not.toHaveBeenCalled();

    scrollAndFlush(0.97 * ENDING_START);
    expect(mocks.loop.preload).toHaveBeenCalledTimes(1);
  });

  it("엔딩 진입 시 L01 재생, G4로 돌아오면 정지, 메인 player에는 새 요청 없음", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollAndFlush(ENDING_START);
    expect(mocks.loop.exit).toHaveBeenCalledTimes(1);

    scrollAndFlush(endingScrollY(0.1));
    expect(mocks.loop.enter).toHaveBeenCalledTimes(1);

    scrollAndFlush(ENDING_START);
    expect(mocks.loop.exit).toHaveBeenCalledTimes(2);
    expect(mocks.player.setTarget).toHaveBeenCalledTimes(1);
  });

  it("L01 첫 프레임 확인 후에만 L01 video 표시", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    const loopVideo = screen.getByTestId("ending-loop");
    expect(loopVideo).not.toHaveAttribute("loop");
    expect(loopVideo).toHaveClass("invisible");
    expect(loopVideo).toHaveAttribute("aria-hidden", "true");
    expect(loopVideo).toHaveAttribute("tabindex", "-1");
    expect(loopVideo).toHaveProperty("muted", true);
    expect(loopVideo).toHaveAttribute("playsinline");

    act(() => mocks.onLoopVisible?.(true));
    expect(loopVideo).toHaveClass("visible");
  });

  it("로고는 정착 전 숨김, 공개 구간에서 점차 표시, 홀드 이후 완전 표시", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    const mark = screen.getByTestId("ending-mark");
    expect(mark).toHaveAttribute("aria-hidden", "true");
    expect(mark).toHaveTextContent("ending-logo");

    scrollAndFlush(endingScrollY(0.1));
    expect(mark.style.opacity).toBe("0");
    scrollAndFlush(endingScrollY(0.3));
    expect(Number(mark.style.opacity)).toBeCloseTo(0.5);
    scrollAndFlush(endingScrollY(0.5));
    expect(mark.style.opacity).toBe("1");

    // 역방향도 같은 진행률이면 같은 상태
    scrollAndFlush(endingScrollY(0.3));
    expect(Number(mark.style.opacity)).toBeCloseTo(0.5);
  });

  it("커튼은 텍스트 없는 장식 레이어, 저항 구간까지 화면 밖이고 끝에서 화면 전체를 덮음", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    const curtain = screen.getByTestId("ending-curtain");
    expect(curtain).toHaveAttribute("aria-hidden", "true");
    expect(curtain).toBeEmptyDOMElement();

    scrollAndFlush(endingScrollY(0.7));
    expect(curtain.style.translate).toBe("0 100%");
    scrollAndFlush(endingScrollY(0.875));
    expect(curtain.style.translate).toBe("0 50%");
    scrollAndFlush(endingScrollY(1));
    expect(curtain.style.translate).toBe("0 0%");
  });

  it("커튼이 완전히 덮으면 L01 일시정지, 되돌아오면 재개", () => {
    render(<CinematicScroll table={table} endingMark={MARK} />);
    scrollAndFlush(endingScrollY(0.9));
    scrollAndFlush(endingScrollY(1));
    expect(mocks.loop.pauseForHidden).toHaveBeenCalledTimes(1);

    scrollAndFlush(endingScrollY(0.9));
    expect(mocks.loop.resume).toHaveBeenCalledTimes(1);
  });

  it("언마운트 시 L01 정리", () => {
    const { unmount } = render(<CinematicScroll table={table} endingMark={MARK} />);
    unmount();

    expect(mocks.loop.dispose).toHaveBeenCalled();
  });
});
