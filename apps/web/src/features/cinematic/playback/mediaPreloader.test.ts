import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { createMediaPreloader, MediaFetchError, type PreloadKey } from "./mediaPreloader";

const ITEMS: { key: PreloadKey; src: string }[] = [
  { key: "G1", src: "/1440p/g1.mp4" },
  { key: "G2", src: "/1440p/g2.mp4" },
  { key: "G3", src: "/1440p/g3.mp4" },
  { key: "G4", src: "/1440p/g4.mp4" },
  { key: "L01", src: "/1440p/l01.mp4" },
];

type Pending = { src: string; signal: AbortSignal; resolve: (blob: Blob) => void; reject: (error: unknown) => void };

function setup() {
  const requests: Pending[] = [];
  const load = vi.fn(
    (src: string, signal: AbortSignal) => new Promise<Blob>((resolve, reject) => requests.push({ src, signal, resolve, reject })),
  );
  const log = vi.fn();
  const onBuffered = vi.fn();
  const preloader = createMediaPreloader(ITEMS, { load, log, onBuffered });
  // 받기 완료 처리 후 다음 받기가 시작될 때까지 마이크로태스크 비우기
  const finish = async (index: number, bytes = 10) => {
    requests[index].resolve(new Blob([new Uint8Array(bytes)]));
    await Promise.resolve();
    await Promise.resolve();
  };
  const fail = async (index: number, error: unknown) => {
    requests[index].reject(error);
    await Promise.resolve();
    await Promise.resolve();
  };
  return { preloader, requests, log, onBuffered, finish, fail };
}

let objectUrls = 0;
beforeEach(() => {
  objectUrls = 0;
  vi.spyOn(URL, "createObjectURL").mockImplementation(() => `blob:test/${++objectUrls}`);
  vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("mediaPreloader 순서와 동시 받기", () => {
  it("한 번에 하나씩 G1→G2→G3→G4→L01 순서로 받음", async () => {
    const { requests, finish, onBuffered } = setup();
    expect(requests.map((request) => request.src)).toEqual(["/1440p/g1.mp4"]);

    for (let index = 0; index < ITEMS.length; index += 1) {
      expect(requests).toHaveLength(index + 1);
      await finish(index);
    }

    expect(requests.map((request) => request.src)).toEqual(ITEMS.map((item) => item.src));
    expect(onBuffered.mock.calls.map(([key]) => key)).toEqual(["G1", "G2", "G3", "G4", "L01"]);
  });

  it("목표 그룹을 우선하면 진행 중 받기는 유지하고 그 그룹을 다음으로 받음", async () => {
    const { preloader, requests, finish } = setup();
    preloader.prioritize("G3");

    expect(requests).toHaveLength(1);
    expect(requests[0].signal.aborted).toBe(false);

    await finish(0);
    expect(requests[1].src).toBe("/1440p/g3.mp4");
    await finish(1);
    expect(requests[2].src).toBe("/1440p/g2.mp4");
  });

  it("이미 받았거나 받는 중인 그룹 우선 요청은 순서를 바꾸지 않음", async () => {
    const { preloader, requests, finish } = setup();
    preloader.prioritize("G1");
    await finish(0);
    preloader.prioritize("G1");

    expect(requests.map((request) => request.src)).toEqual(["/1440p/g1.mp4", "/1440p/g2.mp4"]);
  });

  it("받기 완료 시 Blob URL과 크기 기록, whenReady로 같은 URL 제공", async () => {
    const { preloader, finish, log, onBuffered } = setup();
    const ready = preloader.whenReady("G1");

    await finish(0, 42);

    await expect(ready).resolves.toBe("blob:test/1");
    await expect(preloader.whenReady("G1")).resolves.toBe("blob:test/1");
    expect(onBuffered).toHaveBeenCalledWith("G1", "blob:test/1");
    expect(log.mock.calls.map(([event]) => event)).toEqual([
      { type: "preload", group: "G1", detail: "start" },
      { type: "preload", group: "G1", detail: "buffered", bytes: 42 },
      { type: "preload", group: "G2", detail: "start" },
    ]);
  });
});

describe("mediaPreloader 중단 후 먼저 받기", () => {
  it("진행 중 받기를 중단하고 대상을 바로 받은 뒤, 중단한 그룹은 처음부터 다시 받음", async () => {
    const { preloader, requests, finish, log } = setup();
    await finish(0);
    expect(requests[1].src).toBe("/1440p/g2.mp4");

    preloader.preempt("L01");

    expect(requests[1].signal.aborted).toBe(true);
    expect(requests[2].src).toBe("/1440p/l01.mp4");
    expect(log).toHaveBeenCalledWith({ type: "preload", group: "G2", detail: "preempted", by: "L01" });

    await finish(2);
    expect(requests[3].src).toBe("/1440p/g2.mp4");
  });

  it("중단된 받기가 늦게 끝나도 사본을 만들지 않고 받기를 하나만 유지", async () => {
    const { preloader, requests, finish, onBuffered } = setup();
    preloader.preempt("L01");

    await finish(0);

    expect(onBuffered).not.toHaveBeenCalled();
    expect(requests).toHaveLength(2);
    await expect(Promise.race([preloader.whenReady("G1"), Promise.resolve("pending")])).resolves.toBe("pending");
  });

  it("대상이 이미 받는 중이거나 받았으면 아무것도 중단하지 않음", async () => {
    const { preloader, requests, finish } = setup();
    preloader.preempt("L01");
    preloader.preempt("L01");
    expect(requests).toHaveLength(2);
    expect(requests[1].signal.aborted).toBe(false);

    await finish(1);
    preloader.preempt("L01");
    expect(requests.filter((request) => request.signal.aborted)).toHaveLength(1);
  });

  it("이미 받은 항목을 대상으로 주면 진행 중 받기를 중단하지 않음", async () => {
    const { preloader, requests, finish } = setup();
    await finish(0);

    preloader.preempt("G1");

    expect(requests[1].signal.aborted).toBe(false);
    expect(requests).toHaveLength(2);
  });
});

describe("mediaPreloader 실패", () => {
  it("HTTP 오류면 상태 코드와 함께 fallback 기록, 원래 오류로 거절하고 다음 그룹 진행", async () => {
    const { preloader, requests, fail, log, onBuffered } = setup();
    const ready = preloader.whenReady("G1");
    const error = new MediaFetchError(403);

    await fail(0, error);

    await expect(ready).rejects.toBe(error);
    expect(log).toHaveBeenCalledWith({ type: "preload", group: "G1", detail: "fallback", reason: "http", status: 403 });
    expect(onBuffered).not.toHaveBeenCalled();
    expect(requests[1].src).toBe("/1440p/g2.mp4");
  });

  it("네트워크·CORS 실패면 network 사유와 status null 기록", async () => {
    const { preloader, fail, log } = setup();
    await fail(0, new TypeError("Failed to fetch"));

    expect(log).toHaveBeenCalledWith({ type: "preload", group: "G1", detail: "fallback", reason: "network", status: null });
    await expect(preloader.whenReady("G1")).rejects.toBeInstanceOf(TypeError);
  });
});

describe("mediaPreloader 정리", () => {
  it("dispose 시 진행 중 요청 중단, 받은 사본 revoke", async () => {
    const { preloader, requests, finish } = setup();
    await finish(0);
    await finish(1);

    preloader.dispose();

    expect(requests[2].signal.aborted).toBe(true);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:test/1");
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:test/2");
  });

  it("dispose 후 늦게 끝난 받기는 사본을 만들지 않고 다음 받기도 시작하지 않음", async () => {
    const { preloader, requests, finish, onBuffered } = setup();
    preloader.dispose();

    await finish(0);

    expect(URL.createObjectURL).not.toHaveBeenCalled();
    expect(onBuffered).not.toHaveBeenCalled();
    expect(requests).toHaveLength(1);
  });
});
