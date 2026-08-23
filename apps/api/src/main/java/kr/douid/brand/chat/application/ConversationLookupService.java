package kr.douid.brand.chat.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationNotFoundException;
import kr.douid.brand.chat.domain.ConversationRepository;
import lombok.RequiredArgsConstructor;

/**
 * ownership이 검증된 Conversation을 조회하는 유스케이스
 *
 * Conversation 조회 및 메시지 조회/전송이 공통으로 거쳐야 하는 ownership 검증 지점이다.
 */
@Service
@RequiredArgsConstructor
public class ConversationLookupService {

    private final ConversationRepository conversationRepository;
    private final ConversationOwnershipValidator conversationOwnershipValidator;

    /**
     * ownership이 검증된 상담을 조회
     *
     * @param publicId          조회할 상담의 공개 식별자
     * @param clientIdentityId  인증된 상담 주체 ID (미인증이면 empty)
     * @return ownership이 확인된 상담
     * @throws ConversationNotFoundException 상담이 존재하지 않는 경우
     */
    public Conversation getOwned(UUID publicId, Optional<Long> clientIdentityId) {
        Conversation conversation = conversationRepository.findByPublicId(publicId)
                .orElseThrow(ConversationNotFoundException::new);
        conversationOwnershipValidator.validate(conversation, clientIdentityId);
        return conversation;
    }
}
