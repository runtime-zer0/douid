package kr.douid.brand.chat.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import kr.douid.brand.chat.domain.Message;
import kr.douid.brand.chat.domain.MessageRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link MessageRepository} domain port의 JPA 구현체
 *
 * {@link MessageJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaMessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository messageJpaRepository;

    /**
     * 메시지를 저장하고 반환
     *
     * @param message 저장할 메시지
     * @return 저장된 메시지
     */
    @Override
    public Message save(Message message) {
        return messageJpaRepository.save(message);
    }
}
