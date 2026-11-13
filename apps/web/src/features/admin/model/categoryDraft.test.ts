import { describe, expect, it } from "vitest";

import {
  categoryDraftFrom,
  emptyCategoryDraft,
  needsHideConfirm,
  toCategoryBody,
  validateCategoryDraft,
} from "./categoryDraft";

describe("categoryDraft", () => {
  it("생성 초기값은 순서 0, 공개", () => {
    expect(emptyCategoryDraft()).toEqual({ name: "", slug: "", displayOrder: "0", visible: true });
  });

  it("본문은 네 필드를 모두 포함하고 순서는 정수", () => {
    expect(toCategoryBody({ name: "A", slug: "a", displayOrder: "-3", visible: false })).toEqual({
      name: "A",
      slug: "a",
      displayOrder: -3,
      visible: false,
    });
  });

  it("목록 항목으로 편집 초기값 생성", () => {
    expect(categoryDraftFrom({ id: 1, name: "A", slug: "a", displayOrder: 2, visible: false })).toEqual({
      name: "A",
      slug: "a",
      displayOrder: "2",
      visible: false,
    });
  });

  it("공백만 있는 이름·슬러그, 100자 초과 거부", () => {
    expect(validateCategoryDraft({ name: " ", slug: "", displayOrder: "0", visible: true })).toMatchObject({
      name: expect.any(String),
      slug: expect.any(String),
    });
    const long = "a".repeat(101);
    expect(validateCategoryDraft({ name: long, slug: long, displayOrder: "0", visible: true })).toMatchObject({
      name: expect.any(String),
      slug: expect.any(String),
    });
  });

  it.each(["", "1.5", "abc", "2147483648", "-2147483649", " 1"])("순서 %s 거부", (displayOrder) => {
    expect(validateCategoryDraft({ name: "a", slug: "a", displayOrder, visible: true }).displayOrder).toBeTruthy();
  });

  it.each(["0", "-5", "2147483647", "-2147483648"])("순서 %s 허용", (displayOrder) => {
    expect(validateCategoryDraft({ name: "a", slug: "a", displayOrder, visible: true })).toEqual({});
  });

  it("공개 → 비공개 전환일 때만 확인 필요", () => {
    const open = { name: "a", slug: "a", displayOrder: "0", visible: true };
    const closed = { ...open, visible: false };
    expect(needsHideConfirm(open, closed)).toBe(true);
    expect(needsHideConfirm(closed, closed)).toBe(false);
    expect(needsHideConfirm(open, open)).toBe(false);
    expect(needsHideConfirm(null, closed)).toBe(false);
  });
});
