import { afterEach, describe, expect, it } from "vitest";

import { createQaLog, type QaEvent } from "./qaLog";

afterEach(() => {
  delete window.__DOUID_CINEMATIC_QA__;
});

describe("createQaLog", () => {
  it("20,000건을 넘으면 오래된 이벤트부터 버림", () => {
    const log = createQaLog(true);
    for (let i = 0; i < 20005; i++) log.push({ type: "superseded", requestId: i, group: "G1" });

    const events = JSON.parse(log.export()) as QaEvent[];
    expect(events).toHaveLength(20000);
    expect(events[0]).toMatchObject({ requestId: 5 });
    expect(events.at(-1)).toMatchObject({ requestId: 20004 });
  });

  it("export는 시각이 붙은 JSON 배열 문자열이고 전역 객체로도 접근 가능", () => {
    const log = createQaLog(true);
    log.push({ type: "target", progress: 0.5, masterFrame: 1500, group: "G3" });

    const events = JSON.parse(window.__DOUID_CINEMATIC_QA__!.export()) as QaEvent[];
    expect(events).toEqual([expect.objectContaining({ type: "target", masterFrame: 1500, t: expect.any(Number) })]);
  });

  it("미확인 표시 이벤트를 그대로 보존", () => {
    const log = createQaLog(true);
    log.push({ type: "presented", requestId: 1, group: "G2", webFrame: 0, mediaTime: null, verified: false, latencyMs: 40 });

    expect(JSON.parse(log.export())[0]).toMatchObject({ verified: false, mediaTime: null });
  });

  it("비활성이면 전역 객체를 만들지 않고 기록하지 않음", () => {
    const log = createQaLog(false);
    log.push({ type: "superseded", requestId: 1, group: "G1" });

    expect(window.__DOUID_CINEMATIC_QA__).toBeUndefined();
    expect(log.export()).toBe("[]");
  });

  it("dispose 시 전역 객체 제거", () => {
    const log = createQaLog(true);
    log.dispose();

    expect(window.__DOUID_CINEMATIC_QA__).toBeUndefined();
  });
});
