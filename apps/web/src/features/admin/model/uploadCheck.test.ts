import { describe, expect, it } from "vitest";

import { checkUpload, MAX_UPLOAD_BYTES } from "./uploadCheck";

const fileOf = (size: number, type = "image/png") => {
  const file = new File(["x"], "a.png", { type });
  Object.defineProperty(file, "size", { value: size });
  return file;
};

describe("checkUpload", () => {
  it("빈 파일 거부", () => {
    expect(checkUpload(fileOf(0))).toContain("빈 파일");
  });

  it("이미지가 아닌 파일 거부", () => {
    expect(checkUpload(fileOf(10, "application/pdf"))).toContain("이미지 파일만");
    expect(checkUpload(fileOf(10, ""))).toContain("이미지 파일만");
  });

  it("10MB-64KB는 허용, 1바이트 더 크면 거부", () => {
    expect(checkUpload(fileOf(MAX_UPLOAD_BYTES))).toBeNull();
    expect(checkUpload(fileOf(MAX_UPLOAD_BYTES + 1))).toContain("10MB");
  });
});
