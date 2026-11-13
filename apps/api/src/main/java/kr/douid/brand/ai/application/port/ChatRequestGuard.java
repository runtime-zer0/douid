package kr.douid.brand.ai.application.port;

/** 요청 빈도와 실행·연결 점유 경계 */
public interface ChatRequestGuard {
    /** 요청 시도 소비, 실패 시에도 반환하지 않음 */
    void attempt();
    /** 실행·연결 점유 획득
     * @return 중복 반환 가능한 lease
     */
    Lease acquire();
    interface Lease extends AutoCloseable {
        /** 점유 한 번 반환 */
        @Override void close();
    }
}
