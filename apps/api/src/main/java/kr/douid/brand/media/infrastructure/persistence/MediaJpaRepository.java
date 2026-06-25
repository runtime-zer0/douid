package kr.douid.brand.media.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.media.domain.Media;

/**
 * 미디어 JPA 전용 기술 repository
 *
 * Domain Repository 직접 상속 없이 {@link JpaMediaRepositoryAdapter}에서 위임 호출
 */
public interface MediaJpaRepository extends JpaRepository<Media, Long> {
}
