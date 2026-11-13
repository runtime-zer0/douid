import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiRequest } from "@/shared/api/client";

import { PAGE_SIZE } from "../model/pagination";
import { adminKeys } from "./queryKeys";
import { adminMutationOptions, adminQueryOptions } from "./queryOptions";
import type { Media, Page } from "./types";

/** 미디어 라이브러리 페이지, 최신 업로드 순 */
export function fetchMedia(page0: number) {
  return apiRequest<Page<Media>>(`/api/media?page=${page0}&size=${PAGE_SIZE}`);
}

/** 이미지 1개 업로드, multipart 파트명 `file` */
export function uploadMedia(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return apiRequest<Media>("/api/admin/media", { method: "POST", formData });
}

/** 미디어 자산 삭제, 작업물이 사용 중이면 `MEDIA_IN_USE` */
export function deleteMedia(id: number) {
  return apiRequest<null>(`/api/admin/media/${id}`, { method: "DELETE" });
}

/** 미디어 라이브러리 페이지 조회 */
export function useAdminMedia(page0: number) {
  return useQuery({
    queryKey: adminKeys.media(page0),
    queryFn: () => fetchMedia(page0),
    placeholderData: keepPreviousData,
    ...adminQueryOptions,
  });
}

function useInvalidateMedia() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: adminKeys.mediaAll });
}

/** 이미지 업로드 */
export function useUploadMedia() {
  const invalidate = useInvalidateMedia();
  return useMutation({ ...adminMutationOptions, mutationFn: uploadMedia, onSuccess: invalidate });
}

/** 미디어 삭제 */
export function useDeleteMedia() {
  const invalidate = useInvalidateMedia();
  return useMutation({ ...adminMutationOptions, mutationFn: deleteMedia, onSuccess: invalidate });
}
