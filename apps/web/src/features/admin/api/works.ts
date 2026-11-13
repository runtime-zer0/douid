import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiRequest } from "@/shared/api/client";

import { PAGE_SIZE } from "../model/pagination";
import { adminKeys } from "./queryKeys";
import { adminMutationOptions, adminQueryOptions } from "./queryOptions";
import type { AdminWorkDetail, AdminWorkListItem, Page, WorkBody, WorkVisibility } from "./types";

/** 관리자 작업물 목록, 정렬은 서버 고정이라 `sort`를 보내지 않음 */
export function fetchWorks(page0: number) {
  return apiRequest<Page<AdminWorkListItem>>(`/api/admin/works?page=${page0}&size=${PAGE_SIZE}`);
}

/** 관리자 작업물 상세 */
export function fetchWork(id: number) {
  return apiRequest<AdminWorkDetail>(`/api/admin/works/${id}`);
}

/** 작업물 생성 */
export function createWork(body: WorkBody) {
  return apiRequest<{ id: number }>("/api/admin/works", { method: "POST", json: body });
}

/** 작업물 전체 교체 저장, 빠진 미디어 항목은 연결 해제 */
export function updateWork(id: number, body: WorkBody) {
  return apiRequest<{ id: number }>(`/api/admin/works/${id}`, { method: "PUT", json: body });
}

/** 작업물 공개 설정만 변경 */
export function changeWorkVisibility(id: number, visibility: WorkVisibility) {
  return apiRequest<{ id: number; visibility: WorkVisibility }>(`/api/admin/works/${id}/visibility`, {
    method: "PATCH",
    json: { visibility },
  });
}

/** 작업물 삭제(204), 연결 이미지는 라이브러리에 유지 */
export function deleteWork(id: number) {
  return apiRequest<null>(`/api/admin/works/${id}`, { method: "DELETE" });
}

/** 작업물 목록 페이지, 페이지 이동 중 이전 데이터 유지 */
export function useAdminWorks(page0: number) {
  return useQuery({
    queryKey: adminKeys.works(page0),
    queryFn: () => fetchWorks(page0),
    placeholderData: keepPreviousData,
    ...adminQueryOptions,
  });
}

/** 작업물 상세 */
export function useAdminWork(id: number) {
  return useQuery({ queryKey: adminKeys.work(id), queryFn: () => fetchWork(id), ...adminQueryOptions });
}

function useInvalidateWorks() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: adminKeys.worksAll });
}

/** 작업물 생성 */
export function useCreateWork() {
  const invalidate = useInvalidateWorks();
  return useMutation({ ...adminMutationOptions, mutationFn: createWork, onSuccess: invalidate });
}

/** 작업물 저장, 성공 시 목록·상세 재조회 */
export function useUpdateWork(id: number) {
  const invalidate = useInvalidateWorks();
  return useMutation({
    ...adminMutationOptions,
    mutationFn: (body: WorkBody) => updateWork(id, body),
    onSuccess: invalidate,
  });
}

/** 빠른 공개 전환, 낙관적 갱신 없이 서버 응답 후 재조회 */
export function useChangeWorkVisibility() {
  const invalidate = useInvalidateWorks();
  return useMutation({
    ...adminMutationOptions,
    mutationFn: ({ id, visibility }: { id: number; visibility: WorkVisibility }) => changeWorkVisibility(id, visibility),
    onSuccess: invalidate,
  });
}

/** 작업물 삭제, 성공 후에만 목록에서 제거 */
export function useDeleteWork() {
  const queryClient = useQueryClient();
  return useMutation({
    ...adminMutationOptions,
    mutationFn: deleteWork,
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: adminKeys.work(id), exact: true });
      return queryClient.invalidateQueries({ queryKey: adminKeys.worksAll });
    },
  });
}
