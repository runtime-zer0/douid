package kr.douid.brand.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

/**
 * Conversation에 속한 개별 상담 메시지
 *
 * 별도의 공개 식별자나 단건 조회 수단을 갖지 않으며, 항상 Conversation 기준으로만 조회한다.
 * 발신자 구분({@code senderId})은 이번 Phase 범위 밖이다(chat/CLAUDE.md 참고).
 */
@Getter
@Entity
@Table(name = "message")
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    protected Message() {
    }

    private Message(Long conversationId, String content) {
        this.conversationId = conversationId;
        this.content = content;
    }

    /**
     * 새 메시지를 작성
     *
     * @param conversationId 메시지가 속할 상담 ID
     * @param content        메시지 본문
     * @return 생성된 메시지
     */
    public static Message write(Long conversationId, String content) {
        return new Message(conversationId, content);
    }
}
