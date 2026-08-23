package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.douid.brand.client.domain.ClientRecoveryToken;

/**
 * {@link ClientRecoveryToken} Spring Data JPA 기술 전용 repository
 */
public interface ClientRecoveryTokenJpaRepository extends JpaRepository<ClientRecoveryToken, Long> {

    /**
     * 토큰 해시로 Recovery Token을 비관적 락으로 조회
     *
     * @param tokenHash 조회할 토큰 해시
     * @return Recovery Token (없으면 empty)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ClientRecoveryToken t where t.tokenHash = :tokenHash")
    Optional<ClientRecoveryToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
