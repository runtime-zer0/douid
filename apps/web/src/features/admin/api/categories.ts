import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiRequest } from "@/shared/api/client";

import { adminKeys } from "./queryKeys";
import { adminMutationOptions, adminQueryOptions } from "./queryOptions";
import type { Category, CategoryBody } from "./types";

/** 공개·비공개 전체 카테고리, 서버 정렬 그대로 */
export function fetchCategories() {
  return apiRequest<Category[]>("/api/admin/categories");
}

/** 카테고리 생성, 수정과 같이 네 필드 모두 전송 */
export function createCategory(body: CategoryBody) {
  return apiRequest<Category>("/api/admin/categories", { method: "POST", json: body });
}

/** 카테고리 수정, `visible` 누락 시 서버가 false로 처리하므로 네 필드 모두 전송 */
export function updateCategory(id: number, body: CategoryBody) {
  return apiRequest<Category>(`/api/admin/categories/${id}`, { method: "PUT", json: body });
}

/** 카테고리 삭제, 연결 작업물이 있으면 `CATEGORY_HAS_WORKS` */
export function deleteCategory(id: number) {
  return apiRequest<null>(`/api/admin/categories/${id}`, { method: "DELETE" });
}

/** 관리자 카테고리 전체 목록 */
export function useAdminCategories() {
  return useQuery({ queryKey: adminKeys.categories, queryFn: fetchCategories, ...adminQueryOptions });
}

function useInvalidateCategories() {
  const queryClient = useQueryClient();
  // 작업물 목록의 카테고리명·공개 조건 표시도 함께 갱신
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: adminKeys.categories }),
      queryClient.invalidateQueries({ queryKey: adminKeys.worksAll }),
    ]);
}

/** 카테고리 생성 */
export function useCreateCategory() {
  const invalidate = useInvalidateCategories();
  return useMutation({ ...adminMutationOptions, mutationFn: createCategory, onSuccess: invalidate });
}

/** 카테고리 수정 */
export function useUpdateCategory() {
  const invalidate = useInvalidateCategories();
  return useMutation({
    ...adminMutationOptions,
    mutationFn: ({ id, body }: { id: number; body: CategoryBody }) => updateCategory(id, body),
    onSuccess: invalidate,
  });
}

/** 카테고리 삭제 */
export function useDeleteCategory() {
  const invalidate = useInvalidateCategories();
  return useMutation({ ...adminMutationOptions, mutationFn: deleteCategory, onSuccess: invalidate });
}
