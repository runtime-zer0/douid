package kr.douid.brand.chat.presentation.response;

import java.util.UUID;

import kr.douid.brand.chat.application.command.StartConversationResult;

/**
 * 상담 시작/복원/조회 응답
 *
 * @param conversationId 상담의 외부 공개 식별자
 * @param status          상담 상태
 * @param resumed         기존 활성 상담을 재사용했는지 여부
 */
public record ConversationResponse(UUID conversationId, String status, boolean resumed) {

    /**
     * 상담 시작 결과를 응답으로 변환
     *
     * @param result 상담 시작/복원 결과
     * @return 변환된 응답
     */
    public static ConversationResponse from(StartConversationResult result) {
        return new ConversationResponse(
                result.conversationPublicId(), result.status().name(), result.resumed());
    }
}
