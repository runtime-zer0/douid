/**
 * 서버 LocalDateTime을 `YYYY-MM-DD HH:mm`로 표시
 * 오프셋이 없어 `Date` 파싱은 브라우저 시간대로 의미가 바뀌므로 문자열만 자름
 */
export function formatDateTime(value: string) {
  return value.length >= 16 ? value.slice(0, 16).replace("T", " ") : value;
}
