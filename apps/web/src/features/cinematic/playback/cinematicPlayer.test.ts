import { describe, expect, it, vi } from "vitest";

import type { CinematicTable, FrameTarget } from "../core/types";
import { createCinematicPlayer, type PlayerView } from "./cinematicPlayer";
import { FakeVideo, frameTimeOf, secondsOf } from "./fakeVideo.testutil";

const groups: CinematicTable["groups"] = [
  { id: "G1", src: "/g1.mp4", masterStart: 0, masterEndExclusive: 450 },
  { id: "G2", src: "/g2.mp4", masterStart: 450, masterEndExclusive: 1470 },
  { id: "G3", src: "/g3.mp4", masterStart: 1470, masterEndExclusive: 2410 },
  { id: "G4", src: "/g4.mp4", masterStart: 2410, masterEndExclusive: 3089 },
];

function frame(masterFrame: number): FrameTarget {
  const groupIndex = groups.findIndex((group) => masterFrame < group.masterEndExclusive);
  const webFrame = masterFrame - groups[groupIndex].masterStart;
  return { masterFrame, groupIndex, webFrame, ptsTicks: webFrame * 512 };
}

function setup({ prefetching = false } = {}) {
  const videos = groups.map(() => new FakeVideo());
  const views: PlayerView[] = [];
  const log = vi.fn();
  const player = createCinematicPlayer(
    videos.map((video) => video.asVideo()),
    groups,
    { onView: (view) => views.push(view), log, prefetching },
  );
  videos.forEach((video) => video.loadMetadata());
  const lastView = () => views.at(-1);
  return { videos, player, lastView, log };
}

describe("cinematicPlayer 그룹 전환", () => {
  it("G2 표시가 확인될 때까지 G1을 계속 보여줌", () => {
    const { videos, player, lastView } = setup();
    player.setTarget(frame(449));
    videos[0].presentFrame(frameTimeOf(449));
    expect(lastView()?.visibleGroupIndex).toBe(0);

    player.setTarget(frame(450));
    expect(lastView()).toMatchObject({ visibleGroupIndex: 0, phase: "seeking" });

    videos[1].presentFrame(frameTimeOf(0));
    expect(lastView()).toMatchObject({ visibleGroupIndex: 1, phase: "presented" });
  });

  it("전환 대기 중 목표가 G1로 돌아오면 늦게 도착한 G2 결과로 교체하지 않음", () => {
    const { videos, player, lastView } = setup();
    player.setTarget(frame(449));
    videos[0].presentFrame(frameTimeOf(449));
    player.setTarget(frame(450));
    player.setTarget(frame(449));

    videos[1].presentFrame(frameTimeOf(0));

    expect(lastView()?.visibleGroupIndex).toBe(0);
  });

  it("목표 그룹의 video에만 seek 요청", () => {
    const { videos, player } = setup();
    player.setTarget(frame(449));
    player.setTarget(frame(450));
    player.setTarget(frame(1469));
    player.setTarget(frame(1470));

    expect(videos[0].seeks).toEqual([secondsOf(449)]);
    expect(videos[1].seeks).toEqual([secondsOf(0)]);
    expect(videos[2].seeks).toEqual([secondsOf(0)]);
    expect(videos[3].seeks).toEqual([]);
  });

  it("떠난 그룹은 화면에 없으면 남은 목표로 seek를 이어가지 않음", () => {
    const { videos, player, lastView } = setup();
    player.setTarget(frame(2410));
    videos[3].presentFrame(frameTimeOf(0));
    player.setTarget(frame(2450));
    player.setTarget(frame(2460));
    // G4 seek 40 진행 중, 50 대기 중에 G3으로 되돌아감
    player.setTarget(frame(2400));
    videos[2].presentFrame(frameTimeOf(930));
    expect(lastView()?.visibleGroupIndex).toBe(2);

    videos[3].presentFrame(frameTimeOf(40));
    expect(videos[3].seeks).toEqual([secondsOf(0), secondsOf(40)]);
  });

  it("화면에 남아 있는 이전 그룹은 다음 그룹 표시 전까지 마지막 목표를 계속 따라감", () => {
    const { videos, player, lastView } = setup();
    player.setTarget(frame(430));
    videos[0].presentFrame(frameTimeOf(430));
    player.setTarget(frame(440));
    player.setTarget(frame(449));
    player.setTarget(frame(450));
    videos[0].presentFrame(frameTimeOf(440));

    expect(lastView()?.visibleGroupIndex).toBe(0);
    expect(videos[0].seeks).toEqual([secondsOf(430), secondsOf(440), secondsOf(449)]);
  });

  it("목표 ±1 그룹만 preload=auto", () => {
    const { videos, player } = setup();
    player.setTarget(frame(0));
    expect(videos.map((video) => video.preload)).toEqual(["auto", "auto", "metadata", "metadata"]);

    player.setTarget(frame(2000));
    expect(videos.map((video) => video.preload)).toEqual(["metadata", "auto", "auto", "auto"]);
  });

  it("목표 그룹 오류 시 마지막 표시 그룹을 유지하고 오류 그룹만 재시도", () => {
    const { videos, player, lastView } = setup();
    player.setTarget(frame(449));
    videos[0].presentFrame(frameTimeOf(449));
    player.setTarget(frame(450));

    videos[1].failWith(2);
    expect(lastView()).toMatchObject({ visibleGroupIndex: 0, phase: "error", error: { groupIndex: 1 } });

    player.retry();
    expect(videos[0].load).not.toHaveBeenCalled();
    expect(videos[1].load).toHaveBeenCalledTimes(1);
    expect(lastView()).toMatchObject({ visibleGroupIndex: 0, phase: "media_loading", error: null });
  });
});

describe("cinematicPlayer 미리 받기", () => {
  const preloads = (videos: FakeVideo[]) => videos.map((video) => video.preload);

  it("미리 받기 중이면 받기 전 그룹은 목표 그룹만 스트리밍", () => {
    const { videos, player } = setup({ prefetching: true });
    player.setTarget(frame(500));
    expect(preloads(videos)).toEqual(["metadata", "auto", "metadata", "metadata"]);
  });

  it("메모리 사본으로 바뀐 그룹은 목표가 아니어도 auto", () => {
    const { videos, player } = setup({ prefetching: true });
    player.setTarget(frame(500));
    player.replaceSource(0, "blob:g1");
    expect(preloads(videos)).toEqual(["auto", "auto", "metadata", "metadata"]);

    player.setTarget(frame(2500));
    expect(preloads(videos)).toEqual(["auto", "metadata", "metadata", "auto"]);
  });

  it("화면 그룹 교체는 visible로 기록하고 같은 프레임을 다시 표시 확인", () => {
    const { videos, player, lastView, log } = setup({ prefetching: true });
    player.setTarget(frame(100));
    videos[0].presentFrame(frameTimeOf(100));

    player.replaceSource(0, "blob:g1");

    expect(log).toHaveBeenCalledWith({ type: "source_swap", group: "G1", visible: true });
    expect(videos[0].src).toBe("blob:g1");
    expect(lastView()?.visibleGroupIndex).toBe(0);
    videos[0].loadMetadata();
    expect(videos[0].seeks.at(-1)).toBe(secondsOf(100));
    videos[0].presentFrame(frameTimeOf(100));
    expect(lastView()).toMatchObject({ visibleGroupIndex: 0, phase: "presented" });
  });

  it("화면에 없는 그룹 교체는 visible false로 기록", () => {
    const { videos, player, log } = setup({ prefetching: true });
    player.setTarget(frame(100));
    videos[0].presentFrame(frameTimeOf(100));

    player.replaceSource(2, "blob:g3");

    expect(log).toHaveBeenCalledWith({ type: "source_swap", group: "G3", visible: false });
    expect(videos[2].seeks).toEqual([]);
  });
});
