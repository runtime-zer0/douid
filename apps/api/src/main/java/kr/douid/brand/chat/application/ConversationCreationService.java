package kr.douid.brand.chat.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import lombok.RequiredArgsConstructor;

/**
 * 새 상담을 별도 트랜잭션으로 생성
 *
 * PostgreSQL은 unique index 위반이 발생한 트랜잭션을 즉시 abort 상태로 만들어 같은 트랜잭션에서
 * 재조회를 이어갈 수 없다. {@link ConversationStartService}가 생성 시도와 실패 후 재조회를 같은
 * 트랜잭션에서 처리하지 않도록, 생성 트랜잭션을 이 컴포넌트로 물리적으로 분리한다.
 */
@Service
@RequiredArgsConstructor
public class ConversationCreationService {

    private final ConversationRepository conversationRepository;

    /**
     * 새 활성 상담을 생성
     *
     * 호출자가 unique index 위반({@link org.springframework.dao.DataIntegrityViolationException})을
     * 잡아 재조회로 대체할 수 있도록 독립된 트랜잭션에서 실행한다.
     *
     * @param clientIdentityId 상담 소유자 ID
     * @return 생성된 상담
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Conversation create(Long clientIdentityId) {
        return conversationRepository.save(Conversation.open(clientIdentityId));
    }
}
