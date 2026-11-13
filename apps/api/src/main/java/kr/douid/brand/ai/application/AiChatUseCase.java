package kr.douid.brand.ai.application;

import java.util.function.Consumer;

import kr.douid.brand.ai.application.port.ChatRequestGuard;
import kr.douid.brand.ai.application.port.ChatStreamExecutor;
import kr.douid.brand.ai.application.port.LlmGateway;
import kr.douid.brand.ai.application.port.StreamSink;

/** AI Chat 검증·수락 application 경계 */
public final class AiChatUseCase {
    private final LlmGateway gateway;
    private final ChatRequestGuard guard;
    private final AiChatPolicy policy;
    private final ChatStreamExecutor executor;

    /** 검증·수락 의존성 구성
     * @param gateway 로컬 준비 경계
     * @param guard 요청 및 점유 제한
     * @param executor 유한 실행 경계
     * @param policy 불변 정책
     */
    public AiChatUseCase(LlmGateway gateway, ChatRequestGuard guard, AiChatPolicy policy, ChatStreamExecutor executor) {
        this.gateway = gateway;
        this.guard = guard;
        this.policy = policy;
        this.executor = executor;
    }

    /** 요청 시도 소비 */
    public void attempt() { guard.attempt(); }

    /** 입력 검증 후 실행 준비와 점유 획득
     * @param command 클라이언트 입력
     * @return 실행 전 자원 소유권
     * @throws AiChatException 입력 검증 또는 점유 제한 실패
     * @throws AiChatException.Unavailable 처리기 미준비
     */
    public Prepared prepare(AiChatCommand command) {
        if (command == null || blank(command.message()) || command.history() == null
                || length(command.message()) > policy.maxMessageCodePoints()
                || command.history().size() > policy.maxHistoryMessages()) throw AiChatException.invalid();
        long context = length(command.message());
        for (var message : command.history()) {
            if (message == null || !("user".equals(message.role()) || "assistant".equals(message.role()))
                    || blank(message.content()) || length(message.content()) > policy.maxHistoryMessageCodePoints()) {
                throw AiChatException.invalid();
            }
            context += length(message.content());
        }
        if (context > policy.maxContextCodePoints()) throw AiChatException.invalid();
        var source = gateway.prepare(command);
        try {
            return new Prepared(source, guard.acquire());
        } catch (RuntimeException e) {
            try { source.close(); } catch (RuntimeException ignored) { }
            throw e;
        }
    }

    /** callback 연결 후 유한 실행 제출
     * @param prepared 준비된 실행 소유권
     * @param sink 순차 전송 경계
     * @param callbacks 초기화 전에 등록할 transport callback
     * @return 초기화 신호를 받을 실행
     */
    public AiChatExecution start(Prepared prepared, StreamSink sink,
            Consumer<AiChatExecution> callbacks) {
        var execution = new AiChatExecution(prepared, sink, executor, policy);
        try {
            callbacks.accept(execution);
            execution.start();
            return execution;
        } catch (RuntimeException e) {
            execution.abortBeforeStart();
            throw e;
        }
    }

    private int length(String text) { return text.codePointCount(0, text.length()); }
    private boolean blank(String text) {
        return text == null || text.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }

    public record Prepared(LlmGateway.LlmStream source, ChatRequestGuard.Lease lease) implements AutoCloseable {
        /** 미실행 준비 자원과 점유 반환 */
        @Override
        public void close() {
            try { source.close(); }
            finally { lease.close(); }
        }
    }
}
