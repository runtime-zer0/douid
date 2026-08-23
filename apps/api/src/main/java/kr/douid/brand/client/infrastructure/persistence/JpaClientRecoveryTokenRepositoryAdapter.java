package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.client.domain.ClientRecoveryToken;
import kr.douid.brand.client.domain.ClientRecoveryTokenRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link ClientRecoveryTokenRepository} domain port의 JPA 구현체
 *
 * {@link ClientRecoveryTokenJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaClientRecoveryTokenRepositoryAdapter implements ClientRecoveryTokenRepository {

    private final ClientRecoveryTokenJpaRepository clientRecoveryTokenJpaRepository;

    @Override
    public ClientRecoveryToken save(ClientRecoveryToken token) {
        return clientRecoveryTokenJpaRepository.save(token);
    }

    @Override
    public Optional<ClientRecoveryToken> findByTokenHashForUpdate(String tokenHash) {
        return clientRecoveryTokenJpaRepository.findByTokenHashForUpdate(tokenHash);
    }
}
