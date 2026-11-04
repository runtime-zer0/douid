const DEFAULT_MEDIA_BASE_URL = "/media/cinematic";

/** 시네마틱 미디어 base URL 결정, 끝 `/` 제거 */
export function resolveMediaBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_CINEMATIC_MEDIA_BASE_URL?.trim();
  return (configured || DEFAULT_MEDIA_BASE_URL).replace(/\/+$/, "");
}

// SEAM15 내용(SHA-256 103332ea…27a4), 2026-11-01 `_SEAM15` 접미사를 뗀 이름으로 변경, 이전 후보는 `_ORIGINAL`
// 230프레임·30fps, 0~214는 승인 L01 그대로, 끝 15프레임은 215~229와 0~14를 섞어 15번 프레임으로 이어지게 인코딩 (qa/safari-2026-10-31.md)
const LOOP_FILE = "DOUID_L01_4K_30FPS_WEB_H264.mp4";

/** L01 한 프레임 길이(초), ffprobe 30/1 확인값 */
export const LOOP_FRAME_SECONDS = 1 / 30;

/** L01 반복 재시작 위치(초), 15번 프레임, 0~14번은 G4에서 처음 진입할 때만 재생 */
export const LOOP_RESTART_SECONDS = 15 * LOOP_FRAME_SECONDS;

/** L01 전체 받기 시작 메인 진행률, 운영 기본값 */
export const LOOP_PRELOAD_PROGRESS = 0.95;

const LOOP_PRELOAD_CANDIDATES = new Set(["0.85", "0.9", "0.95"]);

/**
 * L01 받기 시작 진행률 결정, 진단 모드에서만 `cinematic-loop-preload` 후보값 허용
 * 진단 모드 전용 실험값, 운영 기본값은 측정 후 결정(FR-324)
 */
export function resolveLoopPreloadProgress(search: string, debug: boolean): number {
  const requested = new URLSearchParams(search).get("cinematic-loop-preload");
  return debug && requested !== null && LOOP_PRELOAD_CANDIDATES.has(requested) ? Number(requested) : LOOP_PRELOAD_PROGRESS;
}

/** 운영 전달 변형 디렉터리, 1440p 운영 실측 통과 전까지 승인 원본 4k 유지 */
export const MEDIA_VARIANT = "4k";

// 승인 manifest의 filename은 모두 `4k/` 아래, 변형은 디렉터리만 바꾸고 파일명은 그대로 유지
const APPROVED_DIR = "4k/";
const VARIANT_PATTERN = /^[a-z0-9_]+$/;

/** 승인 파일명(`4k/…`)을 변형 디렉터리로 바꿔 미디어 URL 조립, G1~G4·L01 공통 */
export function resolveMediaSrc(baseUrl: string, approvedFilename: string, variant: string = MEDIA_VARIANT): string {
  if (!approvedFilename.startsWith(APPROVED_DIR)) throw new Error(`승인 파일명이 ${APPROVED_DIR}로 시작하지 않음: ${approvedFilename}`);
  return `${baseUrl}/${variant}/${approvedFilename.slice(APPROVED_DIR.length)}`;
}

/** S43 독립 루프(L01) URL, 메인 매핑과 별개 */
export function resolveLoopSrc(): string {
  return resolveMediaSrc(resolveMediaBaseUrl(), `${APPROVED_DIR}${LOOP_FILE}`);
}

/**
 * 진단 모드 전용 변형 디렉터리, `cinematic-media` 값이 경로 이동 문자 없는 이름일 때만 허용
 * 진단 모드가 아니거나 허용되지 않는 값이면 null
 */
export function resolveDebugMediaVariant(search: string, debug: boolean): string | null {
  const requested = new URLSearchParams(search).get("cinematic-media");
  return debug && requested !== null && VARIANT_PATTERN.test(requested) ? requested : null;
}

/** 서버에서 조립된 미디어 URL의 변형 디렉터리(파일명 바로 위)만 교체, 진단 모드 비교용 */
export function replaceMediaVariant(src: string, variant: string): string {
  return src.replace(/\/[^/]+(\/[^/]+)$/, `/${variant}$1`);
}
