package kr.douid.brand.ai.application.port;

import java.util.List;

/** application 신호와 HTTP 전송의 경계 */
public interface StreamSink {
    /** 시작 전송
     * @param requestId 일시 요청 식별자
     */
    void started(String requestId);
    /** Delta 전송
     * @param text 텍스트 조각
     */
    void delta(String text);
    /** 검증된 공개 작업물 slug 전송, terminal 전 최대 1회
     * @param portfolioIds 이번 요청 검색 결과의 slug
     */
    void recommendations(List<String> portfolioIds);
    /** 정상 terminal 전송 */
    void completed();
    /** 실패 terminal 또는 시작 전 오류 전송
     * @param reason 실패 사유
     */
    void failed(StopReason reason);
    /** 출력 예산을 초과한 조각의 전송 중단 신호 */
    final class LimitExceeded extends RuntimeException {}
    /** 전송 불가 또는 종료 후 쓰기 중단 신호 */
    final class Disconnected extends RuntimeException {}
    enum StopReason { SUCCEEDED, FAILED, TIMED_OUT, LIMIT_EXCEEDED, DISCONNECTED, CANCELLED }
}
