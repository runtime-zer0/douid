package kr.douid.brand.chat.domain;

/**
 * Conversation 상태
 *
 * 이번 Phase는 OPEN/CLOSED 2단계만 사용한다. AI/Admin handoff를 위한 추가 상태는
 * 이번 Phase 범위 밖이다(chat/CLAUDE.md 참고).
 */
public enum ConversationStatus {
    OPEN,
    CLOSED
}
