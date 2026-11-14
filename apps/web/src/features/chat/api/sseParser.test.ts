import { describe, expect, it } from "vitest";

import { MULTIBYTE_CRLF, NORMAL, splitAt, splitEveryByte } from "../test/fixtures";

import { createSseParser, type SseEvent } from "./sseParser";

const encode = (text: string) => new TextEncoder().encode(text);

function parse(chunks: Uint8Array[]): SseEvent[] {
  const events: SseEvent[] = [];
  const parser = createSseParser((event) => events.push(event));
  for (const chunk of chunks) parser.push(chunk);
  parser.end();
  return events;
}

const parseText = (...chunks: string[]) => parse(chunks.map(encode));

describe("createSseParser", () => {
  it("한 번에 넣은 NORMAL은 이벤트 4개를 순서대로 냄", () => {
    expect(parse([NORMAL]).map((event) => event.type)).toEqual(["started", "message_delta", "message_delta", "completed"]);
    expect(JSON.parse(parse([NORMAL])[1].data)).toEqual({ text: "패키지 디자인" });
  });

  it.each([
    ["NORMAL", NORMAL],
    ["MULTIBYTE_CRLF", MULTIBYTE_CRLF],
  ])("%s는 모든 단일 분할점과 1바이트 분할에서 결과 동일", (_, fixture) => {
    const expected = parse([fixture]);
    expect(expected.length).toBeGreaterThan(0);
    for (let offset = 1; offset < fixture.length; offset += 1) {
      expect(parse(splitAt(fixture, offset))).toEqual(expected);
    }
    expect(parse(splitEveryByte(fixture))).toEqual(expected);
  });

  it("BOM은 첫 1회만 제거", () => {
    expect(parseText("\uFEFFdata: a\n\n", "data: \uFEFFb\n\n")).toEqual([
      { type: "message", data: "a" },
      { type: "message", data: "\uFEFFb" },
    ]);
  });

  it("CRLF·LF·CR 모두 줄 끝, 청크 경계의 CR+LF는 줄 1개", () => {
    expect(parseText("data: a\r\n\r\ndata: b\n\ndata: c\r\rdata: d\r", "\n\r\n")).toEqual([
      { type: "message", data: "a" },
      { type: "message", data: "b" },
      { type: "message", data: "c" },
      { type: "message", data: "d" },
    ]);
  });

  it("주석 줄 무시", () => {
    expect(parseText(": ping\n\n:\ndata: a\n\n")).toEqual([{ type: "message", data: "a" }]);
  });

  it("data 여러 줄은 줄바꿈으로 결합", () => {
    expect(parseText("data: a\ndata: b\n\n")).toEqual([{ type: "message", data: "a\nb" }]);
  });

  it("콜론 뒤 공백은 1개만 제거", () => {
    expect(parseText("data:x\n\ndata: x\n\ndata:  x\n\n").map((event) => event.data)).toEqual(["x", "x", " x"]);
  });

  it("id·retry·모르는 필드 무시", () => {
    expect(parseText("id: 1\nretry: 10\nfoo: bar\ndata: a\n\n")).toEqual([{ type: "message", data: "a" }]);
  });

  it("event 없으면 type은 message, 있으면 그 이름", () => {
    expect(parseText("data: a\n\nevent: done\ndata: b\n\n")).toEqual([
      { type: "message", data: "a" },
      { type: "done", data: "b" },
    ]);
  });

  it("data 없이 빈 줄만 오면 이벤트 없음, event 이름도 초기화", () => {
    expect(parseText("event: x\n\n\n\ndata: a\n\n")).toEqual([{ type: "message", data: "a" }]);
  });

  it("end() 시 미완결 이벤트는 버림", () => {
    expect(parseText("data: a\n\ndata: b\n")).toEqual([{ type: "message", data: "a" }]);
    expect(parseText("data: a\n\ndata: b")).toEqual([{ type: "message", data: "a" }]);
  });

  it("잘린 멀티바이트 문자가 다음 청크와 합쳐짐", () => {
    const source = encode("data: 한👋\n\n");
    expect(parse(splitAt(source, 7, 8, 11))).toEqual([{ type: "message", data: "한👋" }]);
  });
});
