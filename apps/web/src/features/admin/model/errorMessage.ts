import { ApiError } from "@/shared/api/client";

/**
 * 화면 안내 위치
 * field: 입력 옆, form: 상단 안내, auth: 재인증 다이얼로그, forbidden: 상단 안내(로그아웃하지 않음)
 */
export type ErrorScope = "field" | "form" | "auth" | "forbidden";

export type DescribedError = {
  scope: ErrorScope;
  message: string;
  /** scope가 field일 때 입력 이름별 안내, 서버 `fields`의 이름을 그대로 사용 */
  fieldErrors?: Record<string, string>;
  code: string | null;
  /** 응답 유실, 성공 여부를 알 수 없어 목록 확인 필요 */
  uncertain?: boolean;
};

const FIELD_MESSAGES: Record<string, { field: string; message: string }> = {
  WORK_SLUG_DUPLICATE: { field: "slug", message: "이미 사용 중인 슬러그입니다." },
  CATEGORY_SLUG_DUPLICATE: { field: "slug", message: "이미 사용 중인 슬러그입니다." },
  WORK_THUMBNAIL_DUPLICATE: { field: "mediaItems", message: "대표 이미지는 하나만 지정할 수 있습니다." },
  CATEGORY_NOT_FOUND: {
    field: "categoryId",
    message: "선택한 카테고리를 찾을 수 없습니다. 목록을 새로 불러왔으니 다시 선택해 주세요.",
  },
  MEDIA_NOT_FOUND: {
    field: "mediaItems",
    message: "연결한 이미지 일부를 찾을 수 없습니다. 라이브러리를 새로 불러왔으니 구성을 확인해 주세요.",
  },
  EMPTY_FILE: { field: "file", message: "빈 파일은 업로드할 수 없습니다." },
  INVALID_FILE_TYPE: { field: "file", message: "이미지 파일만 업로드할 수 있습니다." },
};

const FORM_MESSAGES: Record<string, string> = {
  WORK_NOT_FOUND: "작업물을 찾을 수 없습니다. 이미 삭제되었을 수 있습니다.",
  CATEGORY_HAS_WORKS:
    "이 카테고리를 쓰는 작업물(비공개 포함)이 있어 삭제할 수 없습니다. 연결된 작업물의 카테고리를 바꾸거나 작업물을 삭제한 뒤 다시 시도해 주세요.",
  MEDIA_IN_USE:
    "작업물(숨김 포함)에서 사용 중인 이미지라 삭제할 수 없습니다. 작업물에서 연결을 해제하고 저장한 뒤 다시 시도해 주세요.",
  CONFLICT: "입력 길이 제한을 넘었거나 다른 변경과 충돌했습니다. 확인 후 다시 시도해 주세요.",
  INVALID_INPUT: "요청 값 또는 파일 크기를 확인해 주세요.",
};

export const SERVER_ERROR_MESSAGE = "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
export const NETWORK_ERROR_MESSAGE = "응답을 받지 못해 반영 여부를 확인하지 못했습니다. 목록에서 결과를 확인해 주세요.";

/** 요청 실패를 화면 안내로 변환, 같은 401도 로그인 요청이면 계정 오류로 해석 */
export function describeError(error: unknown, context: "login" | "default" = "default"): DescribedError {
  if (!(error instanceof ApiError)) return { scope: "form", message: SERVER_ERROR_MESSAGE, code: null };

  const { failure } = error;
  if (failure.kind === "network") {
    return { scope: "form", message: NETWORK_ERROR_MESSAGE, code: null, uncertain: true };
  }

  const { status, code, fields } = failure;
  if (status === 401) {
    return context === "login"
      ? { scope: "form", message: "이메일 또는 비밀번호가 올바르지 않습니다.", code }
      : { scope: "auth", message: "세션이 만료되었습니다. 다시 로그인한 뒤 저장해 주세요.", code };
  }
  if (status === 403) {
    return {
      scope: "forbidden",
      message: "권한이 없거나 보안 토큰이 만료되었습니다. 다시 시도해 주세요.",
      code,
    };
  }
  if (code === "INVALID_INPUT" && fields.length > 0) {
    return {
      scope: "field",
      message: "입력값을 확인해 주세요.",
      fieldErrors: Object.fromEntries(fields.map(({ field, message }) => [field, message])),
      code,
    };
  }
  if (code && FIELD_MESSAGES[code]) {
    const { field, message } = FIELD_MESSAGES[code];
    return { scope: "field", message, fieldErrors: { [field]: message }, code };
  }
  if (code && FORM_MESSAGES[code]) return { scope: "form", message: FORM_MESSAGES[code], code };
  return { scope: "form", message: SERVER_ERROR_MESSAGE, code };
}
