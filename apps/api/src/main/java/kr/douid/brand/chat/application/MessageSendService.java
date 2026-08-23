package kr.douid.brand.chat.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.chat.application.query.MessageView;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationClosedException;
import kr.douid.brand.chat.domain.Message;
import kr.douid.brand.chat.domain.MessageRepository;
import lombok.RequiredArgsConstructor;

/**
 * 상담 메시지 전송 유스케이스
 *
 * ownership이 확인된 활성(OPEN) 상담에만 메시지를 남길 수 있다(FR-027, FR-028).
 */
@Service
@RequiredArgsConstructor
public class MessageSendService {

    private final ConversationLookupService conversationLookupService;
    private final MessageRepository messageRepository;

    /**
     * 메시지를 전송
     *
     * @param conversationPublicId 메시지를 보낼 상담의 공개 식별자
     * @param clientIdentityId      인증된 상담 주체 ID (미인증이면 empty)
     * @param content                메시지 본문
     * @return 저장된 메시지 조회 결과
     * @throws ConversationClosedException 상담이 이미 종료(CLOSED)된 경우
     */
    @Transactional
    public MessageView send(UUID conversationPublicId, Optional<Long> clientIdentityId, String content) {
        Conversation conversation = conversationLookupService.getOwned(conversationPublicId, clientIdentityId);
        if (!conversation.isOpen()) {
            throw new ConversationClosedException();
        }

        Message message = messageRepository.save(Message.write(conversation.getId(), content));
        return new MessageView(message.getId(), message.getContent(), message.getCreatedAt());
    }
}
