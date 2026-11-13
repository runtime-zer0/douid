/** 화면 표시용 업로드 한도 */
export const MAX_UPLOAD_LABEL = "10MB";

// 서버는 파일·요청 전체 모두 10MB, multipart 오버헤드 여유로 64KB를 뺌
export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024 - 64 * 1024;

/** 업로드 사전 검증, 문제가 있으면 안내 문구 */
export function checkUpload(file: File): string | null {
  if (file.size === 0) return "빈 파일은 업로드할 수 없습니다.";
  if (!file.type.startsWith("image/")) return "이미지 파일만 업로드할 수 있습니다.";
  if (file.size > MAX_UPLOAD_BYTES) return `파일이 너무 큽니다. ${MAX_UPLOAD_LABEL}보다 작은 이미지를 올려 주세요.`;
  return null;
}
