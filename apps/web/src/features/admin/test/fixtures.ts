import { http, HttpResponse } from "msw";

import type { AdminWorkDetail, AdminWorkListItem, Category, Media, Page } from "../api/types";

export const ok = <T,>(data: T, status = 200) => HttpResponse.json({ status: "SUCCESS", data }, { status });
export const fail = (status: number, code: string, fields?: { field: string; message: string }[]) =>
  HttpResponse.json({ status: "FAILURE", data: { code, message: code, ...(fields ? { fields } : {}) } }, { status });

export const csrfHandler = http.get("/api/auth/csrf-token", () =>
  ok({ headerName: "X-XSRF-TOKEN", token: "test-token" }),
);

export const admin = { id: 1, email: "admin@douid.kr", role: "ADMIN" as const };

export const categories: Category[] = [
  { id: 1, name: "Branding", slug: "branding", displayOrder: 0, visible: true },
  { id: 2, name: "Archive", slug: "archive", displayOrder: 1, visible: false },
];

export function listItem(id: number, overrides: Partial<AdminWorkListItem> = {}): AdminWorkListItem {
  return {
    id,
    title: `작업물 ${id}`,
    slug: `work-${id}`,
    visibility: "HIDDEN",
    category: { id: 1, name: "Branding", slug: "branding" },
    thumbnail: null,
    createdAt: "2026-11-13T10:00:00.123456",
    updatedAt: "2026-11-13T11:30:00",
    ...overrides,
  };
}

export function page<T>(content: T[], totalElements = content.length, pageNo = 0): Page<T> {
  return { content, page: pageNo, size: 20, totalElements, hasNext: (pageNo + 1) * 20 < totalElements };
}

export const workDetail: AdminWorkDetail = {
  id: 7,
  title: "Seoul Coffee Identity",
  slug: "seoul-coffee",
  summary: null,
  description: "첫 줄\n둘째 줄",
  visibility: "VISIBLE",
  category: { id: 1, name: "Branding", slug: "branding" },
  mediaItems: [
    { mediaId: 10, role: "THUMBNAIL", sortOrder: 5, altText: "로고", fileUrl: "/api/media/10/file" },
    { mediaId: 11, role: "DETAIL_IMAGE", sortOrder: 9, altText: null, fileUrl: "/api/media/11/file" },
  ],
  createdAt: "2026-11-13T10:00:00",
  updatedAt: "2026-11-13T10:00:00",
};

export function media(id: number, overrides: Partial<Media> = {}): Media {
  return {
    id,
    url: `/api/media/${id}/file`,
    originalFilename: `image-${id}.png`,
    contentType: "image/png",
    fileSize: 12345,
    createdAt: "2026-11-13T09:00:00",
    ...overrides,
  };
}
