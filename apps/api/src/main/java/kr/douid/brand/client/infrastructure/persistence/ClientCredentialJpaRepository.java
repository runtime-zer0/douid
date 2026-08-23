package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.client.domain.ClientCredential;

/**
 * {@link ClientCredential} Spring Data JPA 기술 전용 repository
 */
public interface ClientCredentialJpaRepository extends JpaRepository<ClientCredential, Long> {

    /**
     * 토큰 해시로 credential을 조회
     *
     * @param tokenHash 조회할 토큰 해시
     * @return credential (없으면 empty)
     */
    Optional<ClientCredential> findByTokenHash(String tokenHash);
}
