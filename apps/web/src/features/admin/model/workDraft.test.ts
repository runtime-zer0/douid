import { describe, expect, it } from "vitest";

import type { AdminWorkDetail } from "../api/types";
import {
  addMedia,
  draftFromDetail,
  emptyDraft,
  isDirty,
  move,
  removeItem,
  setAltText,
  setRole,
  toWorkBody,
  validateWorkDraft,
} from "./workDraft";

const detail: AdminWorkDetail = {
  id: 7,
  title: "Brand Film",
  slug: "brand-film",
  summary: null,
  description: "",
  visibility: "VISIBLE",
  category: { id: 3, name: "Film", slug: "film" },
  mediaItems: [
    { mediaId: 10, role: "THUMBNAIL", sortOrder: 5, altText: "cover", fileUrl: "/api/media/10/file" },
    { mediaId: 11, role: "ATTACHMENT", sortOrder: 10, altText: null, fileUrl: "/api/media/11/file" },
    { mediaId: 10, role: "DETAIL_IMAGE", sortOrder: 20, altText: "", fileUrl: "/api/media/10/file" },
  ],
  createdAt: "2026-11-13T10:00:00",
  updatedAt: "2026-11-13T10:00:00",
};

const originalBody = {
  title: "Brand Film",
  slug: "brand-film",
  summary: null,
  description: "",
  categoryId: 3,
  visibility: "VISIBLE",
  mediaItems: [
    { mediaId: 10, role: "THUMBNAIL", sortOrder: 5, altText: "cover" },
    { mediaId: 11, role: "ATTACHMENT", sortOrder: 10, altText: null },
    { mediaId: 10, role: "DETAIL_IMAGE", sortOrder: 20, altText: "" },
  ],
};

describe("workDraft 편집 초기화와 보존", () => {
  it("상세 응답 순서를 유지하고 같은 mediaId도 항목별 고유 key", () => {
    const draft = draftFromDetail(detail);
    expect(draft.mediaItems.map((item) => item.mediaId)).toEqual([10, 11, 10]);
    expect(new Set(draft.mediaItems.map((item) => item.key)).size).toBe(3);
  });

  it("제목만 바꾸면 본문은 원본과 title만 다름(null·빈 문자열·sortOrder 보존)", () => {
    const draft = { ...draftFromDetail(detail), title: "Renamed" };
    expect(toWorkBody(draft)).toEqual({ ...originalBody, title: "Renamed" });
  });

  it("전송 본문에 key·fileUrl 없음", () => {
    for (const item of toWorkBody(draftFromDetail(detail)).mediaItems) {
      expect(Object.keys(item).sort()).toEqual(["altText", "mediaId", "role", "sortOrder"]);
    }
  });

  it("변경이 없으면 dirty 아님, 제목을 바꾸면 dirty", () => {
    const baseline = draftFromDetail(detail);
    expect(isDirty(baseline, baseline)).toBe(false);
    expect(isDirty({ ...baseline, title: "x" }, baseline)).toBe(true);
  });
});

describe("validateWorkDraft", () => {
  const base = { ...emptyDraft(), title: "t", slug: "s" };

  it("공백만 있는 제목·슬러그 거부", () => {
    expect(validateWorkDraft({ ...base, title: "   ", slug: "\t" })).toMatchObject({
      title: expect.any(String),
      slug: expect.any(String),
    });
  });

  it("255자 초과 제목·슬러그·요약·대체 텍스트 거부", () => {
    const long = "a".repeat(256);
    const draft = addMedia({ ...base, title: long, slug: long, summary: long }, { id: 1, url: "/u" });
    const errors = validateWorkDraft(setAltText(draft, draft.mediaItems[0].key, long));
    expect(errors.title && errors.slug && errors.summary).toBeTruthy();
    expect(errors.altText?.[draft.mediaItems[0].key]).toBeTruthy();
  });

  it("255자와 앞뒤 공백은 허용하고 값을 바꾸지 않음", () => {
    const draft = { ...base, title: " 제목 ", slug: "a".repeat(255) };
    expect(validateWorkDraft(draft)).toEqual({});
    expect(toWorkBody(draft).title).toBe(" 제목 ");
  });
});

describe("workDraft 미디어 연산", () => {
  it("생성 초기값은 비공개·미분류·이미지 없음", () => {
    expect(emptyDraft()).toEqual({
      title: "",
      slug: "",
      summary: null,
      description: null,
      categoryId: null,
      visibility: "HIDDEN",
      mediaItems: [],
      orderChanged: false,
    });
  });

  it("추가는 끝에 상세 이미지로, sortOrder는 max+1(빈 목록은 0), 같은 이미지 재추가 허용", () => {
    const first = addMedia(emptyDraft(), { id: 1, url: "/a" });
    expect(first.mediaItems[0]).toMatchObject({ mediaId: 1, role: "DETAIL_IMAGE", sortOrder: 0, altText: null });

    const fromDetail = addMedia(draftFromDetail(detail), { id: 10, url: "/b" });
    expect(fromDetail.mediaItems.at(-1)).toMatchObject({ mediaId: 10, sortOrder: 21 });
    expect(fromDetail.mediaItems).toHaveLength(4);
  });

  it("새 대표를 지정하면 기존 대표는 상세 이미지, 대표는 최대 1개", () => {
    const draft = draftFromDetail(detail);
    const next = setRole(draft, draft.mediaItems[2].key, "THUMBNAIL");
    expect(next.mediaItems.map((item) => item.role)).toEqual(["DETAIL_IMAGE", "ATTACHMENT", "THUMBNAIL"]);
  });

  it("대표 0개 구성도 유효", () => {
    const draft = draftFromDetail(detail);
    const next = setRole(draft, draft.mediaItems[0].key, "DETAIL_IMAGE");
    expect(next.mediaItems.some((item) => item.role === "THUMBNAIL")).toBe(false);
    expect(validateWorkDraft(next)).toEqual({});
  });

  it("이동하면 표시 순서대로 0..n-1, 이동하지 않으면 원래 sortOrder 유지", () => {
    const draft = draftFromDetail(detail);
    expect(toWorkBody(draft).mediaItems.map((item) => item.sortOrder)).toEqual([5, 10, 20]);

    const moved = move(draft, draft.mediaItems[0].key, 1);
    expect(toWorkBody(moved).mediaItems.map((item) => [item.mediaId, item.sortOrder])).toEqual([
      [11, 0],
      [10, 1],
      [10, 2],
    ]);
  });

  it("양 끝 이동은 무시", () => {
    const draft = draftFromDetail(detail);
    expect(move(draft, draft.mediaItems[0].key, -1)).toBe(draft);
    expect(move(draft, draft.mediaItems[2].key, 1)).toBe(draft);
  });

  it("연결 해제는 해당 key만 제거", () => {
    const draft = draftFromDetail(detail);
    const next = removeItem(draft, draft.mediaItems[0].key);
    expect(next.mediaItems.map((item) => item.key)).toEqual([draft.mediaItems[1].key, draft.mediaItems[2].key]);
  });

  it("ATTACHMENT 역할 보존", () => {
    expect(toWorkBody(draftFromDetail(detail)).mediaItems[1].role).toBe("ATTACHMENT");
  });
});
