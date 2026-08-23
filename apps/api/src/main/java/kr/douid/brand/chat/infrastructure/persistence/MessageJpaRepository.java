package kr.douid.brand.chat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.chat.domain.Message;

/**
 * {@link Message} Spring Data JPA 기술 전용 repository
 */
public interface MessageJpaRepository extends JpaRepository<Message, Long> {
}
