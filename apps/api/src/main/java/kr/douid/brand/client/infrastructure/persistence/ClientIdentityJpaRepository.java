package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.douid.brand.client.domain.ClientIdentity;

/**
 * {@link ClientIdentity} Spring Data JPA 기술 전용 repository
 */
public interface ClientIdentityJpaRepository extends JpaRepository<ClientIdentity, Long> {

    /**
     * ID로 상담 주체를 비관적 락으로 조회
     *
     * @param id 상담 주체 ID
     * @return 상담 주체 (없으면 empty)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClientIdentity c where c.id = :id")
    Optional<ClientIdentity> findByIdForUpdate(@Param("id") Long id);
}
