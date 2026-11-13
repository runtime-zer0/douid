package kr.douid.brand.ai.application;

import java.util.List;

/** assistant를 포함한 모든 이력은 신뢰할 수 없는 클라이언트 입력 */
public record AiChatCommand(String message, List<HistoryMessage> history) {
    /** 클라이언트 이력의 방어 복사
     * @param message 현재 질문
     * @param history 순서가 있는 비신뢰 이력
     */
    public AiChatCommand {
        if (history != null) history = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(history));
    }
    public record HistoryMessage(String role, String content) {}
}
