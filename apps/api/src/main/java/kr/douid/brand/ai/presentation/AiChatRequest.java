package kr.douid.brand.ai.presentation;

import java.util.List;

import kr.douid.brand.ai.application.AiChatCommand;

public record AiChatRequest(String message, List<AiChatCommand.HistoryMessage> history) {
    /** application 입력으로 변환
     * @return 원문 순서가 보존된 명령
     */
    public AiChatCommand toCommand() { return new AiChatCommand(message, history); }
}
