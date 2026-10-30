import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

const APPROVED_SHA256 = {
  "DOUID_SCROLL_MAPPING.json": "80af1d6030b219c025ac9c290fc3a8aeebe4a02a6688f272b9e518546e991437",
  "DOUID_WEB_GROUP_MANIFEST.json": "d8377436d7b5c0acf2cf690e47241cf648060e68d1684bcb15a7abc90c664596",
};

describe("승인 원본 JSON", () => {
  it.each(Object.entries(APPROVED_SHA256))("%s 내용이 승인본과 같음", (filename, expected) => {
    const bytes = readFileSync(join(import.meta.dirname, filename));
    expect(createHash("sha256").update(bytes).digest("hex")).toBe(expected);
  });
});
