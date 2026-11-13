import { ApiError } from "@/shared/api/client";

/** 관리자 조회 공통 옵션, 항상 서버 기준이며 네트워크 오류만 1회 재시도(401·403·404 재시도 없음) */
export const adminQueryOptions = {
  staleTime: 0,
  retry: (failureCount: number, error: Error) =>
    failureCount < 1 && error instanceof ApiError && error.failure.kind === "network",
};

/** 관리자 변경 공통 옵션, 실패한 변경은 자동 재전송하지 않음 */
export const adminMutationOptions = { retry: 0 } as const;
