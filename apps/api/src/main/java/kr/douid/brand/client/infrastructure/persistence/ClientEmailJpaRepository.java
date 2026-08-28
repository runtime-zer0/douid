package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.client.domain.ClientEmail;

/**
 * {@link ClientEmail} Spring Data JPA 기술 전용 repository
 */
public interface ClientEmailJpaRepository extends JpaRepository<ClientEmail, Long> {

    /**
     * normalized email로 검증 완료된 이메일을 조회
     *
     * @param normalizedEmail 조회할 정규화 이메일
     * @return 검증 완료된 이메일 (없으면 empty)
     */
    Optional<ClientEmail> findByNormalizedEmail(String normalizedEmail);

    /**
     * 특정 상담 주체가 소유한 검증 완료된 이메일을 조회
     *
     * @param clientIdentityId 상담 주체 ID
     * @param normalizedEmail   조회할 정규화 이메일
     * @return 검증 완료된 이메일 (없으면 empty)
     */
    Optional<ClientEmail> findByClientIdentityIdAndNormalizedEmail(Long clientIdentityId, String normalizedEmail);
}
