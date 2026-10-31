const DEFAULT_MEDIA_BASE_URL = "/media/cinematic";

/** 시네마틱 미디어 base URL 결정, 끝 `/` 제거 */
export function resolveMediaBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_CINEMATIC_MEDIA_BASE_URL?.trim();
  return (configured || DEFAULT_MEDIA_BASE_URL).replace(/\/+$/, "");
}

// 230프레임·30fps, 0~214는 승인 L01 그대로, 끝 15프레임은 215~229와 0~14를 섞어 15번 프레임으로 이어지게 인코딩 (qa/safari-2026-10-31.md)
const LOOP_FILE = "DOUID_L01_4K_30FPS_WEB_H264_SEAM15.mp4";

/** L01 한 프레임 길이(초), ffprobe 30/1 확인값 */
export const LOOP_FRAME_SECONDS = 1 / 30;

/** L01 반복 재시작 위치(초), 15번 프레임, 0~14번은 G4에서 처음 진입할 때만 재생 */
export const LOOP_RESTART_SECONDS = 15 * LOOP_FRAME_SECONDS;

/** S43 독립 루프(L01) URL, 메인 매핑과 별개 */
export function resolveLoopSrc(): string {
  return `${resolveMediaBaseUrl()}/4k/${LOOP_FILE}`;
}
