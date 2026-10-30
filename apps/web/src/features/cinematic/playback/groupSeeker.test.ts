import { afterEach, describe, expect, it, vi } from "vitest";

import type { SeekerSnapshot } from "./groupSeeker";
import { createGroupSeeker } from "./groupSeeker";
import { FakeVideo, FakeVideoWithoutFrameCallback, frameTimeOf, secondsOf } from "./fakeVideo.testutil";

function setup(video = new FakeVideo()) {
  const changes: SeekerSnapshot[] = [];
  const log = vi.fn();
  const seeker = createGroupSeeker(video.asVideo(), "/media/g1.mp4", {
    groupId: "G1",
    onChange: (snapshot) => changes.push(snapshot),
    log,
  });
  const target = (webFrame: number) => seeker.setTarget(webFrame, webFrame * 512);
  return { video, seeker, changes, log, target };
}

afterEach(() => {
  vi.useRealTimers();
});

describe("groupSeeker 기본 동작", () => {
  it("metadata 전에는 seek하지 않고 loadedmetadata 후 목표 프레임 가운데 시각으로 seek", () => {
    const { video, seeker, target } = setup();
    expect(video.src).toBe("/media/g1.mp4");

    target(300);
    expect(video.seeks).toEqual([]);
    expect(seeker.getSnapshot().status).toBe("waiting_metadata");

    video.loadMetadata();
    expect(video.seeks).toEqual([(300 * 512 + 256) / 15360]);
    expect(seeker.getSnapshot().status).toBe("seeking");
  });

  it("seek 시각이 목표 프레임 구간 안쪽이라 시간 단위 내림에도 직전 프레임으로 떨어지지 않음", () => {
    const { video, target } = setup();
    video.loadMetadata();

    for (const frame of [1, 2, 32, 41, 362, 1019]) {
      target(frame);
      const seconds = video.seeks.at(-1)!;
      // 마이크로초·밀리초 단위 내림 후에도 [frame 시작, 다음 frame 시작) 안
      expect(Math.floor(seconds * 1e6) / 1e6).toBeGreaterThanOrEqual(frameTimeOf(frame));
      expect(Math.floor(seconds * 1e3) / 1e3).toBeGreaterThanOrEqual(frameTimeOf(frame));
      expect(seconds).toBeLessThan(frameTimeOf(frame + 1));
      video.presentFrame(frameTimeOf(frame));
    }
  });

  it("rVFC mediaTime이 목표 프레임이면 presented", () => {
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(300);

    video.presentFrame(frameTimeOf(300));

    expect(seeker.getSnapshot()).toMatchObject({ status: "presented", presentedFrame: 300 });
  });

  it("seek 전 합성 대기 중이던 이전 프레임의 rVFC는 표시 확인으로 쓰지 않음", () => {
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(300);

    video.presentFrame(frameTimeOf(10));
    expect(seeker.getSnapshot().status).toBe("seeking");

    video.presentFrame(frameTimeOf(300));
    expect(seeker.getSnapshot()).toMatchObject({ status: "presented", presentedFrame: 300 });
  });

  it("rVFC를 지원하지 않으면 seeked만으로 presented_unverified", () => {
    const { video, seeker, target } = setup(new FakeVideoWithoutFrameCallback());
    video.loadMetadata();
    target(300);

    video.fire("seeked");

    expect(seeker.getSnapshot().status).toBe("presented_unverified");
  });

  it("seeked 후 rVFC가 오지 않으면 대기 상한 뒤 presented_unverified", () => {
    vi.useFakeTimers();
    const { video, seeker, target, log } = setup();
    video.loadMetadata();
    target(300);

    video.fire("seeked");
    expect(seeker.getSnapshot().status).toBe("seeking");

    vi.advanceTimersByTime(200);
    expect(seeker.getSnapshot().status).toBe("presented_unverified");
    expect(log).toHaveBeenCalledWith(expect.objectContaining({ type: "presented", verified: false, mediaTime: null }));
  });
});

describe("groupSeeker latest-wins", () => {
  it("seek 진행 중 목표가 10번 바뀌면 완료 후 마지막 목표만 한 번 더 seek", () => {
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(100);
    for (let frame = 101; frame <= 110; frame++) target(frame);

    expect(video.seeks).toEqual([secondsOf(100)]);

    video.presentFrame(frameTimeOf(100));
    expect(video.seeks).toEqual([secondsOf(100), secondsOf(110)]);

    video.presentFrame(frameTimeOf(110));
    expect(seeker.getSnapshot()).toMatchObject({ status: "presented", presentedFrame: 110, targetWebFrame: 110 });
  });

  it("교체된 이전 요청의 늦은 seeked는 상태를 바꾸지 않음", () => {
    vi.useFakeTimers();
    const { video, seeker, target, log } = setup();
    video.loadMetadata();
    target(100);
    target(200);
    video.presentFrame(frameTimeOf(100));
    expect(log).toHaveBeenCalledWith(expect.objectContaining({ type: "superseded", requestId: 1 }));

    // 100 요청은 이미 완료, 200 요청이 진행 중
    vi.advanceTimersByTime(1000);
    expect(seeker.getSnapshot().status).toBe("seeking");

    video.presentFrame(frameTimeOf(200));
    expect(seeker.getSnapshot()).toMatchObject({ status: "presented", presentedFrame: 200 });
  });

  it("마지막 목표가 현재 표시 프레임과 같으면 추가 seek 없음", () => {
    const { video, target } = setup();
    video.loadMetadata();
    target(100);
    target(200);
    target(100);
    video.presentFrame(frameTimeOf(100));

    expect(video.seeks).toEqual([secondsOf(100)]);

    target(100);
    expect(video.seeks).toEqual([secondsOf(100)]);
  });
});

describe("groupSeeker 오류와 정리", () => {
  it("error 이벤트 → error 상태와 원인", () => {
    const { video, seeker, target } = setup();
    target(10);

    video.failWith(4, 3);

    expect(seeker.getSnapshot()).toMatchObject({ status: "error", error: { kind: "no_source", code: 4 } });
  });

  it("retry → load 후 metadata를 기다려 마지막 목표 재요청", () => {
    const { video, seeker, target } = setup();
    target(10);
    video.failWith(2);
    target(20);

    seeker.retry();
    expect(video.load).toHaveBeenCalledTimes(1);
    expect(seeker.getSnapshot()).toMatchObject({ status: "waiting_metadata", error: null });

    video.loadMetadata();
    expect(video.seeks).toEqual([secondsOf(20)]);
  });

  it("resume → 완료 신호가 끊긴 seek를 버리고 최신 목표로 다시 seek", () => {
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(100);
    target(200);
    // 숨김 동안 100 요청의 seeked·rVFC가 오지 않은 상태

    seeker.resume();
    expect(video.seeks).toEqual([secondsOf(100), secondsOf(200)]);

    video.presentFrame(frameTimeOf(200));
    expect(seeker.getSnapshot()).toMatchObject({ status: "presented", presentedFrame: 200 });
  });

  it("resume → 이미 표시한 프레임이어도 다시 seek해 화면 복구", () => {
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(100);
    video.presentFrame(frameTimeOf(100));

    seeker.resume();

    expect(video.seeks).toEqual([secondsOf(100), secondsOf(100)]);
    expect(seeker.getSnapshot().status).toBe("seeking");
  });

  it("resume → media가 비워졌으면 다시 load 후 metadata를 기다려 seek", () => {
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(100);
    video.presentFrame(frameTimeOf(100));
    video.readyState = 0;
    video.networkState = 1;

    seeker.resume();
    expect(video.load).toHaveBeenCalledTimes(1);
    expect(seeker.getSnapshot().status).toBe("waiting_metadata");

    video.loadMetadata();
    expect(video.seeks).toEqual([secondsOf(100), secondsOf(100)]);
  });

  it("seek가 1.5초 안에 끝나지 않으면 입력이 없어도 media를 다시 불러와 최신 목표로 seek", () => {
    vi.useFakeTimers();
    const { video, seeker, target, log } = setup();
    video.loadMetadata();
    target(100);
    target(150);

    vi.advanceTimersByTime(1400);
    expect(video.load).not.toHaveBeenCalled();

    vi.advanceTimersByTime(200);
    expect(video.load).toHaveBeenCalledTimes(1);
    expect(seeker.getSnapshot()).toMatchObject({ status: "waiting_metadata", presentedFrame: null });
    expect(log).toHaveBeenCalledWith(
      expect.objectContaining({ type: "stalled", detail: expect.stringContaining("seek_timeout#1") }),
    );

    video.loadMetadata();
    expect(video.seeks).toEqual([secondsOf(100), secondsOf(150)]);
    video.presentFrame(frameTimeOf(150));
    expect(seeker.getSnapshot()).toMatchObject({ status: "presented", presentedFrame: 150 });
  });

  it("다시 불러와도 연속 3번 멈추면 오류로 넘기고 retry로 다시 시작", () => {
    vi.useFakeTimers();
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(100);

    for (let attempt = 1; attempt < 3; attempt++) {
      vi.advanceTimersByTime(1500);
      video.loadMetadata();
    }
    vi.advanceTimersByTime(1500);

    expect(video.load).toHaveBeenCalledTimes(2);
    expect(seeker.getSnapshot()).toMatchObject({ status: "error", error: { kind: "seek_timeout" } });

    seeker.retry();
    video.loadMetadata();
    video.presentFrame(frameTimeOf(100));
    expect(seeker.getSnapshot()).toMatchObject({ status: "presented", error: null });
  });

  it("탭 복귀 후 첫 seek는 0.4초 안에 응답이 없으면 바로 다시 불러옴", () => {
    vi.useFakeTimers();
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(100);
    video.presentFrame(frameTimeOf(100));

    seeker.resume();
    vi.advanceTimersByTime(399);
    expect(video.load).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(video.load).toHaveBeenCalledTimes(1);

    // 다시 불러온 뒤 seek는 일반 기준(1.5초)
    video.loadMetadata();
    vi.advanceTimersByTime(1000);
    expect(video.load).toHaveBeenCalledTimes(1);
  });

  it("탭 복귀 후 화면 표시 확인 없이 seeked만 오면 바로 다시 불러옴", () => {
    vi.useFakeTimers();
    const { video, seeker, target, log } = setup();
    video.loadMetadata();
    target(100);
    video.presentFrame(frameTimeOf(100));

    seeker.resume();
    video.fire("seeked");
    vi.advanceTimersByTime(120);

    expect(video.load).toHaveBeenCalledTimes(1);
    expect(seeker.getSnapshot()).toMatchObject({ status: "waiting_metadata", presentedFrame: null });
    expect(log).toHaveBeenCalledWith(
      expect.objectContaining({ type: "stalled", detail: expect.stringContaining("unverified_after_resume#1") }),
    );

    // 다시 불러온 뒤에는 표시 확인이 없어도 반복해서 다시 불러오지 않음
    video.loadMetadata();
    video.fire("seeked");
    vi.advanceTimersByTime(120);
    expect(video.load).toHaveBeenCalledTimes(1);
    expect(seeker.getSnapshot().status).toBe("presented_unverified");
  });

  it("멈춤 복구로 다시 불러오기 직전에 onBeforeReload 호출", () => {
    vi.useFakeTimers();
    const video = new FakeVideo();
    const order: string[] = [];
    video.load.mockImplementation(() => order.push("load"));
    const seeker = createGroupSeeker(video.asVideo(), "/g.mp4", {
      groupId: "G1",
      onBeforeReload: () => order.push("before"),
    });
    video.loadMetadata();
    seeker.setTarget(100, 100 * 512);

    vi.advanceTimersByTime(1500);

    expect(order).toEqual(["before", "load"]);
  });

  it("탭 복귀 후 화면 표시가 확인되면 짧은 기준을 해제", () => {
    vi.useFakeTimers();
    const { video, seeker, target } = setup();
    video.loadMetadata();
    target(100);
    video.presentFrame(frameTimeOf(100));

    seeker.resume();
    video.presentFrame(frameTimeOf(100));
    target(200);
    vi.advanceTimersByTime(1000);

    expect(video.load).not.toHaveBeenCalled();
  });

  it("탭 복귀 당시 목표가 없던 그룹도 다음 첫 seek에 짧은 기준 적용", () => {
    vi.useFakeTimers();
    const { video, seeker, target } = setup();
    video.loadMetadata();

    seeker.resume();
    vi.advanceTimersByTime(10_000);
    target(100);
    vi.advanceTimersByTime(400);

    expect(video.load).toHaveBeenCalledTimes(1);
  });

  it("정상 완료된 seek는 멈춤 타이머를 남기지 않음", () => {
    vi.useFakeTimers();
    const { video, target } = setup();
    video.loadMetadata();
    target(100);
    video.presentFrame(frameTimeOf(100));

    vi.advanceTimersByTime(5000);
    expect(video.load).not.toHaveBeenCalled();
  });

  it("dispose 후에는 콜백 없음, rVFC 취소, src 제거 후 load", () => {
    const { video, seeker, changes, target } = setup();
    video.loadMetadata();
    target(10);
    const changeCount = changes.length;

    seeker.dispose();
    video.presentFrame();
    video.fire("seeked");
    video.failWith(2);

    expect(changes).toHaveLength(changeCount);
    expect(video.cancelVideoFrameCallback).toHaveBeenCalled();
    expect(video.removeAttribute).toHaveBeenCalledWith("src");
    expect(video.load).toHaveBeenCalled();
  });
});
