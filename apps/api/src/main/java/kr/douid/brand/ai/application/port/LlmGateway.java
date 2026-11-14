package kr.douid.brand.ai.application.port;

import java.util.function.Consumer;
import kr.douid.brand.ai.application.AiChatCommand;

/** 공급자와 무관한 로컬 준비 및 단일 스트림 실행 경계 */
public interface LlmGateway {
    /** 외부 호출 없는 실행 준비
     * @param command 검증된 클라이언트 입력
     * @return 한 번 실행 가능한 스트림
     */
    LlmStream prepare(AiChatCommand command);

    interface LlmStream extends AutoCloseable {
        /** 단일 스레드의 순차 Delta 생성, 명시적 정상 완료와 유효 텍스트 확인 후에만 정상 반환
         * @param delta 순차 텍스트 소비자
         * @throws Exception 실행 실패
         */
        void run(Consumer<String> delta) throws Exception;
        /** thread-safe 비차단 취소 요청, 즉시 중단 보장 없음 */
        void cancel();
        /** 실제 실행 종료 후 자원 반환, 중복 호출 허용 */
        @Override void close();
    }
}
