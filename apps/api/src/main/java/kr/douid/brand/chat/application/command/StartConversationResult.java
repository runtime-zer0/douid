package kr.douid.brand.chat.application.command;

import java.util.UUID;

import kr.douid.brand.chat.domain.ConversationStatus;

/**
 * 상담 시작/복원 결과
 *
 * @param conversationPublicId 상담의 외부 공개 식별자
 * @param status                상담 상태
 * @param resumed               기존 활성 상담을 재사용했는지 여부
 * @param rawClientToken        새로 발급된 raw client_token (재사용한 경우 null)
 */
public record StartConversationResult(
        UUID conversationPublicId,
        ConversationStatus status,
        boolean resumed,
        String rawClientToken) {
}
