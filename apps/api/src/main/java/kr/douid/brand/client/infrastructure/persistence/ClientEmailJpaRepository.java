package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * normalized email 문자열을 키로 하는 PostgreSQL 트랜잭션 범위 advisory lock을 획득
     *
     * row 존재 여부와 무관하게 동일 문자열에 대한 동시 트랜잭션을 직렬화하며, 트랜잭션 종료 시
     * 자동 해제된다.
     *
     * @param normalizedEmail 락 키로 사용할 정규화 이메일
     */
    @Query(value = "select pg_advisory_xact_lock(hashtext(:normalizedEmail))", nativeQuery = true)
    void lockByNormalizedEmail(@Param("normalizedEmail") String normalizedEmail);
}
