import { vi } from "vitest";

/** 테스트용 HTMLVideoElement 대역, seek·rVFC·이벤트를 수동으로 진행 */
export class FakeVideo extends EventTarget {
  readyState = 0;
  networkState = 1;
  error: { code: number } | null = null;
  preload = "metadata";
  src = "";
  duration = Number.NaN;
  seeks: number[] = [];
  load = vi.fn(() => {
    this.readyState = 0;
  });
  removeAttribute = vi.fn((name: string) => {
    if (name === "src") this.src = "";
  });
  play = vi.fn(() => Promise.resolve());
  pause = vi.fn();
  cancelVideoFrameCallback = vi.fn((handle: number) => {
    this.frameCallbacks.delete(handle);
  });

  private frameCallbacks = new Map<number, VideoFrameRequestCallback>();
  private nextHandle = 1;
  private time = 0;

  get currentTime() {
    return this.time;
  }

  set currentTime(value: number) {
    this.time = value;
    this.seeks.push(value);
  }

  requestVideoFrameCallback(callback: VideoFrameRequestCallback) {
    const handle = this.nextHandle++;
    this.frameCallbacks.set(handle, callback);
    return handle;
  }

  get pendingFrameCallbacks() {
    return this.frameCallbacks.size;
  }

  loadMetadata() {
    this.readyState = 1;
    this.dispatchEvent(new Event("loadedmetadata"));
  }

  /** 현재 대기 중인 rVFC 콜백에 표시 프레임 전달 */
  presentFrame(mediaTime = this.time) {
    const callbacks = [...this.frameCallbacks.values()];
    this.frameCallbacks.clear();
    callbacks.forEach((callback) => callback(0, { mediaTime } as VideoFrameCallbackMetadata));
  }

  fire(type: string) {
    this.dispatchEvent(new Event(type));
  }

  failWith(code: number, networkState = 1) {
    this.error = { code };
    this.networkState = networkState;
    this.fire("error");
  }

  asVideo() {
    return this as unknown as HTMLVideoElement;
  }
}

/** rVFC를 지원하지 않는 브라우저 대역 */
export class FakeVideoWithoutFrameCallback extends FakeVideo {
  declare requestVideoFrameCallback: never;

  constructor() {
    super();
    Object.defineProperty(this, "requestVideoFrameCallback", { value: undefined });
  }
}

/** seek 요청 시각, 프레임 가운데 */
export const secondsOf = (webFrame: number) => (webFrame * 512 + 256) / 15360;

/** 브라우저가 rVFC mediaTime으로 보고하는 프레임 시작 시각 */
export const frameTimeOf = (webFrame: number) => (webFrame * 512) / 15360;
