package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientEmailRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link ClientEmailRepository} domain port의 JPA 구현체
 *
 * {@link ClientEmailJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaClientEmailRepositoryAdapter implements ClientEmailRepository {

    private final ClientEmailJpaRepository clientEmailJpaRepository;

    @Override
    public ClientEmail save(ClientEmail clientEmail) {
        return clientEmailJpaRepository.save(clientEmail);
    }

    @Override
    public Optional<ClientEmail> findByNormalizedEmail(String normalizedEmail) {
        return clientEmailJpaRepository.findByNormalizedEmail(normalizedEmail);
    }

    @Override
    public Optional<ClientEmail> findVerifiedByClientIdentityIdAndNormalizedEmail(Long clientIdentityId,
            String normalizedEmail) {
        return clientEmailJpaRepository.findByClientIdentityIdAndNormalizedEmail(clientIdentityId, normalizedEmail);
    }

    @Override
    public void lockByNormalizedEmail(String normalizedEmail) {
        clientEmailJpaRepository.lockByNormalizedEmail(normalizedEmail);
    }
}
