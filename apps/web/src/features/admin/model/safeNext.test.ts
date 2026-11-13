import { describe, expect, it } from "vitest";

import { safeNext } from "./safeNext";

describe("safeNext", () => {
  it.each(["/admin/works?page=2", "/admin/works/3/edit", "/admin/media"])("내부 관리 경로 %s 허용", (path) => {
    expect(safeNext(path)).toBe(path);
  });

  it.each([
    "/admin",
    "//evil.com",
    "/\\evil",
    "/admin/\\evil",
    "https://x",
    "/about",
    "",
    "/admin/login",
    "/admin/login?next=/admin/works",
    undefined,
    null,
  ])("%s는 작업물 목록으로", (path) => {
    expect(safeNext(path)).toBe("/admin/works");
  });

  it("배열 값은 무시", () => {
    expect(safeNext(["/admin/media", "/admin/works"])).toBe("/admin/works");
  });
});
