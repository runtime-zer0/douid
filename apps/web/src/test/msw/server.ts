import { setupServer } from "msw/node";

/** 테스트 전역 MSW 서버, 테스트별 응답은 `server.use()`로 등록 */
export const server = setupServer();
