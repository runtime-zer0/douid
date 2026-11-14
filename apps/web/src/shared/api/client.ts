/** 정규화한 요청 실패, `http`는 서버 응답, `network`는 응답 유실 */
export type ApiFailure =
  | {
      kind: "http";
      status: number;
      code: string | null;
      message: string | null;
      fields: { field: string; message: string }[];
    }
  | { kind: "network" };

/** 요청 실패 오류, 화면은 `failure.code`로 분기 */
export class ApiError extends Error {
  constructor(readonly failure: ApiFailure) {
    super(failure.kind === "http" ? `HTTP ${failure.status} ${failure.code ?? ""}`.trim() : "Network error");
    this.name = "ApiError";
  }
}

type CsrfToken = { headerName: string; token: string };

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  json?: unknown;
  formData?: FormData;
  signal?: AbortSignal;
};

const CSRF_PATH = "/api/auth/csrf-token";

let csrfToken: Promise<CsrfToken> | null = null;

/** 보관한 CSRF 토큰 폐기, 다음 변경 요청 때 새로 취득 */
export function clearCsrfToken() {
  csrfToken = null;
}

function getCsrfToken() {
  // 동시에 들어온 첫 변경 요청이 같은 취득 요청을 공유
  csrfToken ??= send<CsrfToken>(CSRF_PATH, {}).catch((error) => {
    csrfToken = null;
    throw error;
  });
  return csrfToken;
}

function resolve(path: string) {
  // 브라우저는 상대 경로 그대로, jsdom·Node fetch는 절대 URL 필요
  return typeof window === "undefined" ? path : new URL(path, window.location.origin).toString();
}

async function readBody(response: Response): Promise<unknown> {
  if (response.status === 204 || !response.headers.get("content-type")?.includes("json")) return undefined;
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

async function send<T>(path: string, { method = "GET", json, formData, signal }: RequestOptions, csrf?: CsrfToken): Promise<T> {
  const headers: Record<string, string> = {};
  if (json !== undefined) headers["Content-Type"] = "application/json";
  if (csrf) headers[csrf.headerName] = csrf.token;

  let response: Response;
  try {
    response = await fetch(resolve(path), {
      method,
      headers,
      body: formData ?? (json !== undefined ? JSON.stringify(json) : undefined),
      credentials: "same-origin",
      cache: "no-store",
      signal,
    });
  } catch (error) {
    if (signal?.aborted) throw error;
    throw new ApiError({ kind: "network" });
  }

  if (response.ok) {
    const body = (await readBody(response)) as { data?: unknown } | undefined;
    return (body?.data ?? null) as T;
  }

  if (response.status === 401 || response.status === 403) clearCsrfToken();
  throw await toApiError(response);
}

/** 실패 응답의 봉투 해석, JSON이 아니거나 `data.code`가 없으면 `code: null` */
export async function toApiError(response: Response): Promise<ApiError> {
  const body = (await readBody(response)) as { data?: unknown } | undefined;
  const error = body?.data as { code?: unknown; message?: unknown; fields?: unknown } | undefined;
  return new ApiError({
    kind: "http",
    status: response.status,
    code: typeof error?.code === "string" ? error.code : null,
    message: typeof error?.message === "string" ? error.message : null,
    fields: Array.isArray(error?.fields) ? (error.fields as { field: string; message: string }[]) : [],
  });
}

/**
 * 관리자 API 공용 요청, 응답 봉투의 `data` 반환(204는 `null`)
 * 변경 요청에는 CSRF 헤더 부착, 401·403이면 토큰 폐기, 실패 요청은 재전송하지 않음
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? "GET";
  const csrf = method === "GET" ? undefined : await getCsrfToken();
  return send<T>(path, options, csrf);
}
