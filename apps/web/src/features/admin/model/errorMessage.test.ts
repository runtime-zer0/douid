import { describe, expect, it } from "vitest";

import { ApiError } from "@/shared/api/client";

import { describeError } from "./errorMessage";

const http = (status: number, code: string | null, fields: { field: string; message: string }[] = []) =>
  new ApiError({ kind: "http", status, code, message: null, fields });

describe("describeError", () => {
  it("로그인 401은 계정 오류 안내", () => {
    expect(describeError(http(401, "AUTHENTICATION_FAILED"), "login")).toMatchObject({
      scope: "form",
      message: "이메일 또는 비밀번호가 올바르지 않습니다.",
    });
  });

  it("로그인 외 401은 재인증", () => {
    expect(describeError(http(401, "UNAUTHORIZED")).scope).toBe("auth");
  });

  it("403은 로그아웃 없이 다시 시도 안내", () => {
    const described = describeError(http(403, "FORBIDDEN"));
    expect(described.scope).toBe("forbidden");
    expect(described.message).toContain("보안 토큰");
  });

  it("INVALID_INPUT + fields는 서버 필드 이름 그대로 연결", () => {
    expect(describeError(http(400, "INVALID_INPUT", [{ field: "title", message: "필수입니다" }]))).toMatchObject({
      scope: "field",
      fieldErrors: { title: "필수입니다" },
    });
  });

  it("INVALID_INPUT(fields 없음)은 요청 값·파일 크기 안내", () => {
    expect(describeError(http(400, "INVALID_INPUT")).message).toContain("파일 크기");
  });

  it.each([
    ["WORK_SLUG_DUPLICATE", "slug"],
    ["CATEGORY_SLUG_DUPLICATE", "slug"],
    ["WORK_THUMBNAIL_DUPLICATE", "mediaItems"],
    ["CATEGORY_NOT_FOUND", "categoryId"],
    ["MEDIA_NOT_FOUND", "mediaItems"],
    ["EMPTY_FILE", "file"],
    ["INVALID_FILE_TYPE", "file"],
  ])("%s는 %s 필드 안내", (code, field) => {
    const described = describeError(http(400, code));
    expect(described.scope).toBe("field");
    expect(described.fieldErrors?.[field]).toBeTruthy();
  });

  it.each([
    ["WORK_NOT_FOUND", "삭제되었을 수"],
    ["CATEGORY_HAS_WORKS", "비공개 포함"],
    ["MEDIA_IN_USE", "연결을 해제하고 저장"],
    ["CONFLICT", "충돌"],
  ])("%s는 일반 안내", (code, text) => {
    const described = describeError(http(409, code));
    expect(described.scope).toBe("form");
    expect(described.message).toContain(text);
  });

  it("네트워크 실패는 반영 여부 불명확", () => {
    expect(describeError(new ApiError({ kind: "network" }))).toMatchObject({ scope: "form", uncertain: true });
  });

  it("모르는 코드와 비JSON 응답은 서버 오류", () => {
    expect(describeError(http(500, "INTERNAL_ERROR")).message).toContain("서버 오류");
    expect(describeError(http(502, null)).message).toContain("서버 오류");
    expect(describeError(new Error("x")).message).toContain("서버 오류");
  });
});
