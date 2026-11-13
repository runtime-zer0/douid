export type Admin = { id: number; email: string; role: "ADMIN" };

export type Category = { id: number; name: string; slug: string; displayOrder: number; visible: boolean };
export type CategoryBody = Omit<Category, "id">;

/** `createdAt`은 오프셋 없는 LocalDateTime 문자열 */
export type Media = {
  id: number;
  url: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
};

export type WorkVisibility = "VISIBLE" | "HIDDEN";
export type WorkMediaRole = "THUMBNAIL" | "DETAIL_IMAGE" | "ATTACHMENT";
export type WorkCategoryRef = { id: number; name: string; slug: string };
export type WorkMedia = {
  mediaId: number;
  role: WorkMediaRole;
  sortOrder: number;
  altText: string | null;
  fileUrl: string;
};

export type AdminWorkListItem = {
  id: number;
  title: string;
  slug: string;
  visibility: WorkVisibility;
  category: WorkCategoryRef | null;
  thumbnail: WorkMedia | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminWorkDetail = Omit<AdminWorkListItem, "thumbnail"> & {
  summary: string | null;
  description: string | null;
  mediaItems: WorkMedia[];
};

/** 생성·수정 공통 본문, PUT은 `mediaItems` 전체 교체 */
export type WorkBody = {
  title: string;
  slug: string;
  summary: string | null;
  description: string | null;
  categoryId: number | null;
  visibility: WorkVisibility;
  mediaItems: { mediaId: number; role: WorkMediaRole; sortOrder: number; altText: string | null }[];
};

export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; hasNext: boolean };
