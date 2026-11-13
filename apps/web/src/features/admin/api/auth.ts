import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiRequest, clearCsrfToken } from "@/shared/api/client";

import { adminKeys } from "./queryKeys";
import { adminMutationOptions, adminQueryOptions } from "./queryOptions";
import type { Admin } from "./types";

/** 관리자 로그인, 성공 시 서버가 세션 쿠키 발급 */
export function login(email: string, password: string) {
  return apiRequest<Admin>("/api/auth/login", { method: "POST", json: { email, password } });
}

/** 현재 세션의 관리자 조회 */
export function fetchMe() {
  return apiRequest<Admin>("/api/auth/me");
}

/** 로그아웃, 성공 시 보관 CSRF 토큰 폐기 */
export async function logout() {
  await apiRequest<null>("/api/auth/logout", { method: "POST" });
  clearCsrfToken();
}

/** 서버 세션 기준 관리자 확인 */
export function useAdminMe() {
  return useQuery({ queryKey: adminKeys.me, queryFn: fetchMe, ...adminQueryOptions });
}

/** 로그인, 성공 응답을 `me` 캐시로 설정 */
export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    ...adminMutationOptions,
    mutationFn: ({ email, password }: { email: string; password: string }) => login(email, password),
    onSuccess: (admin) => queryClient.setQueryData(adminKeys.me, admin),
  });
}

/**
 * 로그아웃, 성공 시 `me` 외 관리자 캐시 제거
 * `me`는 로그인 화면 마운트 때 제거, 게이트가 먼저 `me`를 다시 받아 `next` 붙은 이동이 겹치는 것을 방지
 */
export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    ...adminMutationOptions,
    mutationFn: logout,
    onSuccess: () =>
      queryClient.removeQueries({ queryKey: adminKeys.all, predicate: (query) => query.queryKey[1] !== "me" }),
  });
}
