package kr.douid.brand.chat.application.query;

import kr.douid.brand.shared.pagination.CursorPageResponse;

/**
 * Conversation 기준 메시지 목록 조회 전용 포트
 */
public interface ConversationMessageQueryService {

    /**
     * Conversation의 메시지 목록을 cursor 기반으로 페이지네이션 조회
     *
     * @param conversationId 조회할 상담의 내부 ID
     * @param cursor          이전 페이지 마지막 메시지 ID (없으면 처음부터)
     * @param size            페이지 크기
     * @return 메시지 목록 페이지
     */
    CursorPageResponse<MessageView> findMessages(Long conversationId, Long cursor, int size);
}
