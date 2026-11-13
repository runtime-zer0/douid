import { describe, expect, it } from "vitest";

import type { Category } from "../api/types";
import { publicExposure } from "./exposure";

const categories: Category[] = [
  { id: 1, name: "Open", slug: "open", displayOrder: 0, visible: true },
  { id: 2, name: "Closed", slug: "closed", displayOrder: 1, visible: false },
];

describe("publicExposure", () => {
  it("HIDDEN은 카테고리와 무관하게 hidden", () => {
    expect(publicExposure({ visibility: "HIDDEN", categoryId: 1 }, categories)).toBe("hidden");
    expect(publicExposure({ visibility: "HIDDEN", categoryId: null }, undefined)).toBe("hidden");
  });

  it("VISIBLE + 미분류는 uncategorized", () => {
    expect(publicExposure({ visibility: "VISIBLE", categoryId: null }, categories)).toBe("uncategorized");
  });

  it("VISIBLE + 비공개 카테고리는 categoryHidden", () => {
    expect(publicExposure({ visibility: "VISIBLE", categoryId: 2 }, categories)).toBe("categoryHidden");
  });

  it("VISIBLE + 공개 카테고리는 eligible", () => {
    expect(publicExposure({ visibility: "VISIBLE", categoryId: 1 }, categories)).toBe("eligible");
  });

  it("카테고리 조회 실패나 ID 불일치는 unknown", () => {
    expect(publicExposure({ visibility: "VISIBLE", categoryId: 1 }, undefined)).toBe("unknown");
    expect(publicExposure({ visibility: "VISIBLE", categoryId: 99 }, categories)).toBe("unknown");
  });
});
