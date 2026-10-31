import { afterEach, describe, expect, it, vi } from "vitest";

import { resolveLoopPreloadProgress, resolveLoopSrc } from "./media";

const FILE = "DOUID_L01_4K_30FPS_WEB_H264.mp4";

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("resolveLoopSrc", () => {
  it.each([
    ["미설정", undefined, `/media/cinematic/4k/${FILE}`],
    ["빈 문자열", "", `/media/cinematic/4k/${FILE}`],
    ["공백", "   ", `/media/cinematic/4k/${FILE}`],
    ["운영 base", "https://cdn.douid.kr/cinematic", `https://cdn.douid.kr/cinematic/4k/${FILE}`],
    ["끝 슬래시 1개", "https://cdn.douid.kr/cinematic/", `https://cdn.douid.kr/cinematic/4k/${FILE}`],
    ["끝 슬래시 여러 개", "https://cdn.douid.kr/cinematic///", `https://cdn.douid.kr/cinematic/4k/${FILE}`],
  ])("%s이면 <base>/4k/<파일명>으로 조립하고 경로에 이중 슬래시가 없음", (_label, base, expected) => {
    vi.stubEnv("NEXT_PUBLIC_CINEMATIC_MEDIA_BASE_URL", base);

    const src = resolveLoopSrc();
    expect(src).toBe(expected);
    expect(src.replace(/^https?:\/\//, "")).not.toContain("//");
  });
});

describe("resolveLoopPreloadProgress", () => {
  it("진단 모드가 아니면 쿼리와 무관하게 기본값", () => {
    expect(resolveLoopPreloadProgress("?cinematic-loop-preload=0.85", false)).toBe(0.95);
  });

  it.each([
    ["0.85", 0.85],
    ["0.9", 0.9],
    ["0.95", 0.95],
  ])("진단 모드에서 후보값 %s 반영", (value, expected) => {
    expect(resolveLoopPreloadProgress(`?cinematic-debug=1&cinematic-loop-preload=${value}`, true)).toBe(expected);
  });

  it.each(["0.5", "abc", "", "0.90"])("진단 모드라도 후보 밖 값 '%s'이면 기본값", (value) => {
    expect(resolveLoopPreloadProgress(`?cinematic-loop-preload=${value}`, true)).toBe(0.95);
  });

  it("쿼리가 없으면 기본값", () => {
    expect(resolveLoopPreloadProgress("", true)).toBe(0.95);
  });
});
