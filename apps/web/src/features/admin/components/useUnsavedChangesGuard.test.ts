import { renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { useUnsavedChangesGuard } from "./useUnsavedChangesGuard";

afterEach(() => vi.restoreAllMocks());

const beforeUnloadListeners = (spy: { mock: { calls: unknown[][] } }) =>
  spy.mock.calls.filter(([type]) => type === "beforeunload").length;

describe("useUnsavedChangesGuard", () => {
  it("dirty일 때만 beforeunload 등록", () => {
    const add = vi.spyOn(window, "addEventListener");
    const { rerender } = renderHook(({ dirty }) => useUnsavedChangesGuard(dirty), { initialProps: { dirty: false } });
    expect(beforeUnloadListeners(add)).toBe(0);

    rerender({ dirty: true });
    expect(beforeUnloadListeners(add)).toBe(1);

    const event = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
  });

  it("언마운트하면 리스너 제거", () => {
    const remove = vi.spyOn(window, "removeEventListener");
    const { unmount } = renderHook(() => useUnsavedChangesGuard(true));

    unmount();

    expect(beforeUnloadListeners(remove)).toBe(1);
  });

  it("confirm이 false면 이동하지 않음, dirty가 아니면 묻지 않음", () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    const { result, rerender } = renderHook(({ dirty }) => useUnsavedChangesGuard(dirty), { initialProps: { dirty: true } });
    expect(result.current()).toBe(false);
    expect(confirm).toHaveBeenCalledTimes(1);

    rerender({ dirty: false });
    expect(result.current()).toBe(true);
    expect(confirm).toHaveBeenCalledTimes(1);
  });
});
