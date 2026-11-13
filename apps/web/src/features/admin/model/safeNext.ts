const DEFAULT_PATH = "/admin/works";

/** 로그인 후 복귀 경로 검증, 내부 `/admin/...`만 허용하고 그 외는 작업물 목록 */
export function safeNext(next: string | string[] | null | undefined) {
  if (typeof next !== "string") return DEFAULT_PATH;
  // `//host`, `/\host`는 브라우저가 외부 Origin으로 해석
  if (!next.startsWith("/admin/") || next.startsWith("//") || next.includes("\\")) return DEFAULT_PATH;
  if (next === "/admin/login" || next.startsWith("/admin/login?")) return DEFAULT_PATH;
  return next;
}
