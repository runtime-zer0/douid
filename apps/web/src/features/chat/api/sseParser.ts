/** 빈 줄로 확정된 SSE 이벤트, `event` 필드가 없으면 `message` */
export type SseEvent = { type: string; data: string };

/**
 * 바이트 청크를 SSE 이벤트로 바꾸는 증분 파서, 청크 경계와 이벤트 경계를 별개로 처리
 * BOM 1회 제거·CR/LF/CRLF 줄 끝·주석 무시·복수 data 결합, `id`·`retry`는 무시(재접속 없음)
 * `end()` 시 빈 줄로 끝나지 않은 마지막 이벤트는 버림
 */
export function createSseParser(onEvent: (event: SseEvent) => void) {
  // 기본 TextDecoder가 스트림 첫 BOM만 제거
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  let skipLeadingLf = false;
  let eventType = "";
  let data: string[] = [];

  function processLine(line: string) {
    if (line === "") {
      if (data.length > 0) onEvent({ type: eventType || "message", data: data.join("\n") });
      eventType = "";
      data = [];
      return;
    }
    if (line.startsWith(":")) return;
    const colon = line.indexOf(":");
    const field = colon === -1 ? line : line.slice(0, colon);
    let value = colon === -1 ? "" : line.slice(colon + 1);
    if (value.startsWith(" ")) value = value.slice(1);
    if (field === "event") eventType = value;
    else if (field === "data") data.push(value);
  }

  function feed(text: string) {
    // 앞 청크가 CR로 끝났으면 이번 청크 첫 LF는 같은 줄 끝
    if (skipLeadingLf && text.startsWith("\n")) text = text.slice(1);
    if (text.length > 0) skipLeadingLf = false;
    buffer += text;

    let start = 0;
    for (let index = 0; index < buffer.length; index += 1) {
      const char = buffer[index];
      if (char !== "\r" && char !== "\n") continue;
      processLine(buffer.slice(start, index));
      if (char === "\r") {
        if (index + 1 === buffer.length) skipLeadingLf = true;
        else if (buffer[index + 1] === "\n") index += 1;
      }
      start = index + 1;
    }
    buffer = buffer.slice(start);
  }

  return {
    push(chunk: Uint8Array) {
      feed(decoder.decode(chunk, { stream: true }));
    },
    end() {
      feed(decoder.decode());
      buffer = "";
      eventType = "";
      data = [];
    },
  };
}
