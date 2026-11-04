import type { GroupId } from "../core/types";
import type { QaEventInput } from "../diagnostics/qaLog";

export type PreloadKey = GroupId | "L01";

/** 응답은 받았지만 성공 상태가 아님, fallback 사유를 네트워크·CORS 실패와 구분하기 위함 */
export class MediaFetchError extends Error {
  constructor(readonly status: number) {
    super(`media fetch failed: ${status}`);
  }
}

/** 영상 전체를 받아 Blob으로 반환, 성공 상태가 아니면 MediaFetchError */
export async function fetchMediaBlob(src: string, signal: AbortSignal): Promise<Blob> {
  const response = await fetch(src, { signal });
  if (!response.ok) throw new MediaFetchError(response.status);
  return response.blob();
}

/** 영상 전체를 받아 메모리 Blob URL 생성 */
export async function loadAsObjectUrl(src: string, signal: AbortSignal): Promise<string> {
  return URL.createObjectURL(await fetchMediaBlob(src, signal));
}

/** fallback 기록용 사유, 네트워크 끊김과 CORS 거부는 브라우저가 같은 TypeError로 주므로 network 하나로 묶음 */
export const fallbackReason = (error: unknown) =>
  error instanceof MediaFetchError
    ? { reason: "http" as const, status: error.status }
    : { reason: "network" as const, status: null };

export type MediaPreloader = {
  /** 아직 받지 않은 항목을 다음 순서로 올림, 진행 중 받기는 중단하지 않음 */
  prioritize: (key: PreloadKey) => void;
  /** 받기 완료 시 Blob URL, 실패 시 원래 오류로 거절 */
  whenReady: (key: PreloadKey) => Promise<string>;
  dispose: () => void;
};

type PreloaderOptions = {
  /** 받기 완료 직후 호출, 소스 교체 담당 */
  onBuffered?: (key: PreloadKey, url: string) => void;
  log?: (event: QaEventInput) => void;
  load?: (src: string, signal: AbortSignal) => Promise<Blob>;
};

type Entry = {
  key: PreloadKey;
  src: string;
  state: "queued" | "loading" | "buffered" | "failed";
  url: string | null;
  error: unknown;
  waiters: { resolve: (url: string) => void; reject: (error: unknown) => void }[];
};

/**
 * 그룹 영상을 주어진 순서로 한 번에 하나씩 통째로 받아 Blob URL로 보관, 탐색을 네트워크에서 분리
 * 받은 사본은 역방향 스크롤 대비 세션 동안 유지, 실패한 항목은 스트리밍 그대로 사용
 */
export function createMediaPreloader(
  items: { key: PreloadKey; src: string }[],
  { onBuffered, log, load = fetchMediaBlob }: PreloaderOptions = {},
): MediaPreloader {
  const entries = items.map<Entry>(({ key, src }) => ({ key, src, state: "queued", url: null, error: null, waiters: [] }));
  const abort = new AbortController();
  let priority: PreloadKey | null = null;
  let loading = false;
  let disposed = false;

  const find = (key: PreloadKey) => entries.find((entry) => entry.key === key);

  function settle(entry: Entry) {
    entry.waiters.forEach(({ resolve, reject }) => (entry.state === "buffered" ? resolve(entry.url!) : reject(entry.error)));
    entry.waiters = [];
  }

  function pump() {
    if (loading || disposed) return;
    const prioritized = priority === null ? undefined : find(priority);
    const entry = prioritized?.state === "queued" ? prioritized : entries.find((candidate) => candidate.state === "queued");
    priority = null;
    if (!entry) return;

    loading = true;
    entry.state = "loading";
    log?.({ type: "preload", group: entry.key, detail: "start" });
    load(entry.src, abort.signal).then(
      (blob) => {
        if (disposed) return;
        entry.state = "buffered";
        entry.url = URL.createObjectURL(blob);
        log?.({ type: "preload", group: entry.key, detail: "buffered", bytes: blob.size });
        onBuffered?.(entry.key, entry.url);
        settle(entry);
        loading = false;
        pump();
      },
      (error: unknown) => {
        if (disposed) return;
        entry.state = "failed";
        entry.error = error;
        log?.({ type: "preload", group: entry.key, detail: "fallback", ...fallbackReason(error) });
        settle(entry);
        loading = false;
        pump();
      },
    );
  }

  pump();

  return {
    prioritize(key) {
      if (disposed || find(key)?.state !== "queued") return;
      priority = key;
      pump();
    },
    whenReady(key) {
      const entry = find(key);
      if (!entry) return Promise.reject(new Error(`preload 대상 아님: ${key}`));
      if (entry.state === "buffered") return Promise.resolve(entry.url!);
      if (entry.state === "failed") return Promise.reject(entry.error);
      return new Promise((resolve, reject) => entry.waiters.push({ resolve, reject }));
    },
    dispose() {
      if (disposed) return;
      disposed = true;
      abort.abort();
      entries.forEach((entry) => {
        if (entry.url) URL.revokeObjectURL(entry.url);
      });
    },
  };
}
