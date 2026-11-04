import { afterEach, describe, expect, it, vi } from "vitest";

import { replaceMediaVariant, resolveDebugMediaVariant, resolveLoopPreloadProgress, resolveLoopSrc, resolveMediaSrc } from "./media";

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

describe("resolveMediaSrc", () => {
  const BASE = "https://cdn.douid.kr/cinematic";

  it("기본은 승인 파일명 그대로 4k 디렉터리", () => {
    expect(resolveMediaSrc(BASE, "4k/DOUID_G1_4K_30FPS_WEB_H264.mp4")).toBe(`${BASE}/4k/DOUID_G1_4K_30FPS_WEB_H264.mp4`);
  });

  it("변형을 주면 4k/만 변형 디렉터리로 바꾸고 파일명은 유지, 이중 슬래시 없음", () => {
    const src = resolveMediaSrc(BASE, "4k/DOUID_G1_4K_30FPS_WEB_H264.mp4", "1440p");
    expect(src).toBe(`${BASE}/1440p/DOUID_G1_4K_30FPS_WEB_H264.mp4`);
    expect(src.replace(/^https?:\/\//, "")).not.toContain("//");
  });

  it.each(["DOUID_G1_4K_30FPS_WEB_H264.mp4", "1440p/DOUID_G1.mp4", "/4k/DOUID_G1.mp4", "4K/DOUID_G1.mp4"])(
    "승인 파일명이 4k/로 시작하지 않으면 '%s' 오류",
    (filename) => {
      expect(() => resolveMediaSrc(BASE, filename)).toThrow();
    },
  );
});

describe("resolveDebugMediaVariant", () => {
  it.each(["4k", "1440p", "1440p_g3", "1440p_g5"])("진단 모드에서 '%s' 허용", (value) => {
    expect(resolveDebugMediaVariant(`?cinematic-debug=1&cinematic-media=${value}`, true)).toBe(value);
  });

  it.each(["..", "../4k", "1440p/../4k", "a/b", "", "1440P", "1440p%2F", "%2e%2e"])("경로 이동·허용 밖 값 '%s'은 무시", (value) => {
    expect(resolveDebugMediaVariant(`?cinematic-media=${value}`, true)).toBeNull();
  });

  it("진단 모드가 아니면 무시", () => {
    expect(resolveDebugMediaVariant("?cinematic-media=1440p_g3", false)).toBeNull();
  });

  it("쿼리가 없으면 null", () => {
    expect(resolveDebugMediaVariant("", true)).toBeNull();
  });
});

describe("replaceMediaVariant", () => {
  it.each([
    ["운영 CDN", "https://cdn.douid.kr/cinematic/4k/DOUID_G1_4K_30FPS_WEB_H264.mp4", "https://cdn.douid.kr/cinematic/1440p_g3/DOUID_G1_4K_30FPS_WEB_H264.mp4"],
    ["로컬", "/media/cinematic/4k/DOUID_L01_4K_30FPS_WEB_H264.mp4", "/media/cinematic/1440p_g3/DOUID_L01_4K_30FPS_WEB_H264.mp4"],
  ])("%s URL의 파일명 바로 위 디렉터리만 교체", (_label, src, expected) => {
    expect(replaceMediaVariant(src, "1440p_g3")).toBe(expected);
  });
});
