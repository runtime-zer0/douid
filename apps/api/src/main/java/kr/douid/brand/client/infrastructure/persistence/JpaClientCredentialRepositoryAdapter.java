package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.client.domain.ClientCredential;
import kr.douid.brand.client.domain.ClientCredentialRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link ClientCredentialRepository} domain port의 JPA 구현체
 *
 * {@link ClientCredentialJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaClientCredentialRepositoryAdapter implements ClientCredentialRepository {

    private final ClientCredentialJpaRepository clientCredentialJpaRepository;

    /**
     * credential을 저장하고 반환
     *
     * @param clientCredential 저장할 credential
     * @return 저장된 credential
     */
    @Override
    public ClientCredential save(ClientCredential clientCredential) {
        return clientCredentialJpaRepository.save(clientCredential);
    }

    /**
     * 토큰 해시로 credential을 조회
     *
     * @param tokenHash 조회할 토큰 해시
     * @return credential (없으면 empty)
     */
    @Override
    public Optional<ClientCredential> findByTokenHash(String tokenHash) {
        return clientCredentialJpaRepository.findByTokenHash(tokenHash);
    }
}
