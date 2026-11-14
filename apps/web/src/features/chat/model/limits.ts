// 서버 계약 초안 API-02 기본값 미러, 합의 시 이 파일만 갱신
export const MAX_MESSAGE_CODE_POINTS = 2000;
export const MAX_HISTORY_MESSAGES = 12;
export const MAX_CONTEXT_CODE_POINTS = 12000;
// 서버 실행 60초·emitter 65초보다 길게 두어 서버 STREAM_TIMEOUT이 먼저 오게 함
export const CLIENT_TIMEOUT_MS = 90_000;
