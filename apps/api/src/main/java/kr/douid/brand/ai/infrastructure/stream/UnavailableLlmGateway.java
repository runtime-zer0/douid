package kr.douid.brand.ai.infrastructure.stream;

import kr.douid.brand.ai.application.AiChatCommand;
import kr.douid.brand.ai.application.AiChatException;
import kr.douid.brand.ai.application.port.LlmGateway;

/** 실제 AI 처리기 미준비를 명시하는 기본 adapter */
public final class UnavailableLlmGateway implements LlmGateway {
    /** 실제 AI 미준비 오류 반환
     * @param command 검증된 입력
     * @return 반환되지 않음
     * @throws AiChatException.Unavailable 처리기 미준비
     */
    @Override
    public LlmStream prepare(AiChatCommand command) { throw new AiChatException.Unavailable(); }
}
