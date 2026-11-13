package kr.douid.brand.ai.application.port;

import java.time.Duration;
import java.util.concurrent.Future;

/** 유한 스트림 실행 및 비차단 기한 예약 경계 */
public interface ChatStreamExecutor {
    /** 대기열 없는 실행 제출
     * @param task 실행 작업
     * @return 취소 핸들
     */
    Future<?> submit(Runnable task);
    /** 네트워크 쓰기 없는 기한 콜백 예약
     * @param task 기한 신호
     * @param delay 기한
     * @return 취소 핸들
     */
    Future<?> schedule(Runnable task, Duration delay);
}
