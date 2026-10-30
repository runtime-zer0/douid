const DEFAULT_MEDIA_BASE_URL = "/media/cinematic";

/** 시네마틱 미디어 base URL 결정, 끝 `/` 제거 */
export function resolveMediaBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_CINEMATIC_MEDIA_BASE_URL?.trim();
  return (configured || DEFAULT_MEDIA_BASE_URL).replace(/\/+$/, "");
}
