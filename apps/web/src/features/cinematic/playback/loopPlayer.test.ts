import { afterEach, describe, expect, it, vi } from "vitest";

import { FakeVideo, FakeVideoWithoutFrameCallback } from "./fakeVideo.testutil";
import { createLoopPlayer } from "./loopPlayer";

const SRC = "/media/cinematic/4k/DOUID_L01_4K_30FPS_WEB_H264.mp4";
const BLOB = "blob:douid/l01";

/** 받기 완료·실패 시점을 테스트가 직접 결정하는 loadSource 대역 */
function deferredSource() {
  let resolve!: (url: string) => void;
  let reject!: (error: Error) => void;
  let signal: AbortSignal | null = null;
  const load = vi.fn((_src: string, abortSignal: AbortSignal) => {
    signal = abortSignal;
    return new Promise<string>((res, rej) => {
      resolve = res;
      reject = rej;
    });
  });
  return {
    load,
    signal: () => signal,
    succeed: async (url = BLOB) => {
      resolve(url);
      await Promise.resolve();
    },
    fail: async () => {
      reject(new Error("network"));
      await Promise.resolve();
    },
  };
}

function setup(video = new FakeVideo()) {
  const visible: boolean[] = [];
  const log = vi.fn();
  const source = deferredSource();
  const player = createLoopPlayer(video.asVideo(), SRC, {
    onVisibleChange: (next) => visible.push(next),
    log,
    loadSource: source.load,
  });
  return { video, player, visible, log, source };
}

/** 받기 완료 후 enter까지 마친 상태 */
async function playing(video = new FakeVideo()) {
  const context = setup(video);
  context.player.enter();
  await context.source.succeed();
  return context;
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("loopPlayer 받기", () => {
  it("preload 전에는 아무것도 받지 않고, preload 시 전체를 받아 메모리 사본을 재생 소스로 사용", async () => {
    const { video, player, source, log } = setup();
    expect(source.load).not.toHaveBeenCalled();

    player.preload();
    player.preload();
    expect(source.load).toHaveBeenCalledTimes(1);
    expect(source.load).toHaveBeenCalledWith(SRC, expect.any(AbortSignal));
    expect(video.src).toBe("");

    await source.succeed();
    expect(video.src).toBe(BLOB);
    expect(video.preload).toBe("auto");
    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "buffered" });
    expect(video.play).not.toHaveBeenCalled();
  });

  it("받기 전에 엔딩에 들어오면 G4 정지 화면을 유지하다가 받은 직후 처음부터 재생", async () => {
    const { video, player, source } = setup();

    player.enter();
    expect(source.load).toHaveBeenCalledTimes(1);
    expect(video.play).not.toHaveBeenCalled();
    expect(player.isVisible()).toBe(false);

    await source.succeed();
    expect(video.seeks).toEqual([0]);
    expect(video.play).toHaveBeenCalledTimes(1);
  });

  it("받기 실패 시 원본 주소로 직접 재생", async () => {
    const { video, player, source, log } = setup();
    player.enter();

    await source.fail();
    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "fallback", reason: "network", status: null });
    expect(video.src).toBe(SRC);
    expect(video.play).toHaveBeenCalledTimes(1);
  });

  it("받기 시작은 preload를 여러 번 불러도 한 번만 기록", () => {
    const { player, log } = setup();
    player.preload();
    player.preload();

    expect(log.mock.calls.filter(([event]) => event.detail === "preload_start")).toHaveLength(1);
  });

  it("엔딩 진입으로 시작된 받기도 시작을 기록", () => {
    const { player, log } = setup();
    player.enter();

    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "preload_start" });
  });

  describe("기본 받기 실패 사유", () => {
    function setupWithFetch(fetchImpl: () => Promise<Response>) {
      vi.stubGlobal("fetch", vi.fn(fetchImpl));
      const createObjectUrl = vi.spyOn(URL, "createObjectURL");
      const video = new FakeVideo();
      const log = vi.fn();
      const player = createLoopPlayer(video.asVideo(), SRC, { onVisibleChange: () => {}, log });
      return { video, player, log, createObjectUrl };
    }

    afterEach(() => {
      vi.unstubAllGlobals();
    });

    it("HTTP 오류 응답이면 상태 코드와 함께 http로 기록하고 원본 주소로 재생", async () => {
      const { video, player, log, createObjectUrl } = setupWithFetch(() =>
        Promise.resolve(new Response(null, { status: 404 })),
      );
      player.preload();

      await vi.waitFor(() =>
        expect(log).toHaveBeenCalledWith({ type: "loop", detail: "fallback", reason: "http", status: 404 }),
      );
      expect(createObjectUrl).not.toHaveBeenCalled();
      expect(video.src).toBe(SRC);
    });

    it("요청 자체가 실패하면 network로 기록", async () => {
      const { player, log } = setupWithFetch(() => Promise.reject(new TypeError("Failed to fetch")));
      player.preload();

      await vi.waitFor(() =>
        expect(log).toHaveBeenCalledWith({ type: "loop", detail: "fallback", reason: "network", status: null }),
      );
    });
  });

  it("받는 도중 정리되면 요청을 중단하고 늦게 도착한 사본은 해제", async () => {
    const revoke = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
    const { video, player, source } = setup();
    player.preload();

    player.dispose();
    expect(source.signal()?.aborted).toBe(true);

    await source.succeed();
    expect(revoke).toHaveBeenCalledWith(BLOB);
    expect(video.src).toBe("");
  });

  it("정리 시 재생 중인 메모리 사본 해제", async () => {
    const revoke = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
    const { video, player } = await playing();

    player.dispose();
    expect(video.removeAttribute).toHaveBeenCalledWith("src");
    expect(revoke).toHaveBeenCalledWith(BLOB);
  });
});

describe("loopPlayer 재생", () => {
  it("첫 프레임이 표시된 뒤에만 보임", async () => {
    const { video, player, visible, log } = await playing();
    expect(player.isVisible()).toBe(false);

    video.presentFrame();
    expect(player.isVisible()).toBe(true);
    expect(visible).toEqual([true]);
    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "presented" });
  });

  it("rVFC 미지원이면 playing 이벤트로 표시", async () => {
    const { video, player } = await playing(new FakeVideoWithoutFrameCallback());

    expect(player.isVisible()).toBe(false);
    video.fire("playing");
    expect(player.isVisible()).toBe(true);
  });

  it("exit 시 정지하고 숨김, 대기 중인 rVFC 취소, 이후 늦게 온 첫 프레임은 무시", async () => {
    const { video, player, visible } = await playing();
    player.exit();

    expect(video.pause).toHaveBeenCalled();
    expect(video.pendingFrameCallbacks).toBe(0);
    video.fire("playing");
    expect(player.isVisible()).toBe(false);
    expect(visible).toEqual([]);
  });

  it("다시 들어오면 받은 사본을 재사용해 처음부터 재생", async () => {
    const { video, player, source } = await playing();
    player.exit();

    player.enter();
    expect(source.load).toHaveBeenCalledTimes(1);
    expect(video.seeks).toEqual([0, 0]);
    expect(video.play).toHaveBeenCalledTimes(2);
  });

  it("오류 시 숨기고 기록, 다시 들어와도 재생하지 않음", async () => {
    const { video, player, log } = await playing();
    video.presentFrame();

    video.failWith(4);
    expect(player.isVisible()).toBe(false);
    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "error" });

    player.exit();
    player.enter();
    expect(video.play).toHaveBeenCalledTimes(1);
  });

  it("자동재생이 막히면 숨긴 채 기록", async () => {
    const video = new FakeVideo();
    video.play.mockReturnValueOnce(Promise.reject(new DOMException("blocked", "NotAllowedError")));
    const { player, log } = await playing(video);

    await vi.waitFor(() => expect(log).toHaveBeenCalledWith({ type: "loop", detail: "error" }));
    expect(player.isVisible()).toBe(false);
  });

  it("재생 시작 전 일시정지로 중단돼도 오류로 처리하지 않고 첫 프레임 표시 대기 유지", async () => {
    const video = new FakeVideo();
    video.play.mockReturnValueOnce(Promise.reject(new DOMException("aborted", "AbortError")));
    const { player, log } = await playing(video);

    player.pauseForHidden();
    await Promise.resolve();
    player.resume();
    video.presentFrame();

    expect(log).not.toHaveBeenCalledWith({ type: "loop", detail: "error" });
    expect(player.isVisible()).toBe(true);
  });

  it("탭 숨김 시 일시정지, 복귀 시 엔딩 안이면 다시 재생, 받기 전이면 재생하지 않음", async () => {
    const { video, player, source } = setup();
    player.resume();
    player.enter();
    player.resume();
    expect(video.play).not.toHaveBeenCalled();

    await source.succeed();
    player.pauseForHidden();
    expect(video.pause).toHaveBeenCalledTimes(1);
    player.resume();
    expect(video.play).toHaveBeenCalledTimes(2);
  });

  it("멈춘 동안 받기가 끝나면 재생하지 않고, 재개할 때 처음부터 재생", async () => {
    const { video, player, source } = setup();
    player.enter();
    player.pauseForHidden();

    await source.succeed();
    expect(video.play).not.toHaveBeenCalled();
    player.resume();
    expect(video.play).toHaveBeenCalledTimes(1);
    video.presentFrame();
    expect(player.isVisible()).toBe(true);
  });

  it("멈춘 뒤 늦게 온 ended에는 이동·재생하지 않음", async () => {
    const { video, player } = await playing();
    player.pauseForHidden();

    video.fire("ended");
    expect(video.seeks).toEqual([0]);
    expect(video.play).toHaveBeenCalledTimes(1);
  });

  it("G4 복귀로 멈춤 상태가 풀려 다시 진입하면 재생", async () => {
    const { video, player } = await playing();
    player.pauseForHidden();
    player.exit();

    player.enter();
    expect(video.play).toHaveBeenCalledTimes(2);
  });

  it("재생이 거절된 뒤 재개하면 첫 프레임 확인을 다시 걸어 표시", async () => {
    const video = new FakeVideo();
    video.play.mockReturnValueOnce(Promise.reject(new DOMException("blocked", "NotAllowedError")));
    const { player, log } = await playing(video);
    await vi.waitFor(() => expect(log).toHaveBeenCalledWith({ type: "loop", detail: "error" }));
    expect(video.pendingFrameCallbacks).toBe(0);

    player.resume();
    expect(video.pendingFrameCallbacks).toBe(1);
    video.presentFrame();
    expect(player.isVisible()).toBe(true);
  });

  it("이전 재생 요청의 늦은 거절은 새 요청의 표시 상태를 지우지 않음", async () => {
    const video = new FakeVideo();
    let rejectFirst!: (error: Error) => void;
    video.play.mockReturnValueOnce(new Promise((_, reject) => (rejectFirst = reject)));
    const { player, log } = await playing(video);

    player.resume();
    video.presentFrame();
    rejectFirst(new DOMException("blocked", "NotAllowedError"));
    await Promise.resolve();
    await Promise.resolve();

    expect(player.isVisible()).toBe(true);
    expect(log).not.toHaveBeenCalledWith({ type: "loop", detail: "error" });
  });

  it("dispose 후 이벤트에 반응하지 않음", async () => {
    vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
    const { video, player, log } = await playing();
    player.dispose();
    log.mockClear();

    video.failWith(4);
    video.fire("timeupdate");
    expect(log).not.toHaveBeenCalled();
    expect(video.pendingFrameCallbacks).toBe(0);
  });
});

describe("loopPlayer 반복", () => {
  const FRAME = 1 / 30;

  async function looping(video = new FakeVideo()) {
    video.duration = 230 * FRAME;
    const visible: boolean[] = [];
    const source = deferredSource();
    const player = createLoopPlayer(video.asVideo(), SRC, {
      onVisibleChange: (next) => visible.push(next),
      loadSource: source.load,
      restartAt: 15 * FRAME,
      frameSeconds: FRAME,
    });
    player.enter();
    await source.succeed();
    return { video, player };
  }

  it("처음 진입은 0부터, 마지막 프레임이 표시되면 재생을 멈추지 않고 재시작 위치로 이동", async () => {
    const { video } = await looping();
    expect(video.seeks).toEqual([0]);

    video.presentFrame(228 * FRAME);
    expect(video.seeks).toEqual([0]);
    video.presentFrame(229 * FRAME);
    expect(video.seeks).toEqual([0, 15 * FRAME]);
    expect(video.pause).not.toHaveBeenCalled();
    expect(video.pendingFrameCallbacks).toBe(1);
  });

  it("마지막 프레임 표시를 놓쳐 끝나면 재시작 위치에서 다시 재생", async () => {
    const { video } = await looping();

    video.fire("ended");
    expect(video.seeks).toEqual([0, 15 * FRAME]);
    expect(video.play).toHaveBeenCalledTimes(2);
  });

  it("rVFC 미지원이면 끝날 때 재시작 위치로 이동", async () => {
    const { video } = await looping(new FakeVideoWithoutFrameCallback());

    video.fire("ended");
    expect(video.seeks).toEqual([0, 15 * FRAME]);
  });

  it("엔딩 밖에서는 끝나도 다시 재생하지 않음", async () => {
    const { video, player } = await looping();
    player.exit();

    video.fire("ended");
    expect(video.seeks).toEqual([0]);
    expect(video.play).toHaveBeenCalledTimes(1);
  });
});

describe("loopPlayer 진단", () => {
  it("재생 위치가 처음으로 돌아가면 루프 경계로 기록", async () => {
    const { video, log } = await playing();

    video.currentTime = 7.5;
    video.fire("timeupdate");
    video.currentTime = 0.1;
    video.fire("timeupdate");

    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "wrap", droppedFrames: null, totalFrames: null });
  });

  it("루프 경계마다 누적 드롭 프레임을 함께 기록", async () => {
    const video = new FakeVideo();
    Object.assign(video, { getVideoPlaybackQuality: () => ({ droppedVideoFrames: 12, totalVideoFrames: 690 }) });
    const { log } = await playing(video);

    video.currentTime = 7.5;
    video.fire("timeupdate");
    video.currentTime = 0.1;
    video.fire("timeupdate");

    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "wrap", droppedFrames: 12, totalFrames: 690 });
  });

  it("stalled·waiting은 기록만 하고 표시 상태 유지", async () => {
    const { video, player, log } = await playing();
    video.presentFrame();

    video.fire("stalled");
    video.fire("waiting");
    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "stalled" });
    expect(log).toHaveBeenCalledWith({ type: "loop", detail: "waiting" });
    expect(player.isVisible()).toBe(true);
  });
});
