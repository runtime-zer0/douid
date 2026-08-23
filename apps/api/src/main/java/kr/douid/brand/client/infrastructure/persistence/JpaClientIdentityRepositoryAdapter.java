package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.client.domain.ClientIdentity;
import kr.douid.brand.client.domain.ClientIdentityRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link ClientIdentityRepository} domain port의 JPA 구현체
 *
 * {@link ClientIdentityJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaClientIdentityRepositoryAdapter implements ClientIdentityRepository {

    private final ClientIdentityJpaRepository clientIdentityJpaRepository;

    /**
     * 상담 주체를 저장하고 반환
     *
     * @param clientIdentity 저장할 상담 주체
     * @return 저장된 상담 주체
     */
    @Override
    public ClientIdentity save(ClientIdentity clientIdentity) {
        return clientIdentityJpaRepository.save(clientIdentity);
    }

    /**
     * ID로 상담 주체를 조회
     *
     * @param id 상담 주체 ID
     * @return 상담 주체 (없으면 empty)
     */
    @Override
    public Optional<ClientIdentity> findById(Long id) {
        return clientIdentityJpaRepository.findById(id);
    }

    /**
     * ID로 상담 주체를 비관적 락으로 조회
     *
     * @param id 상담 주체 ID
     * @return 상담 주체 (없으면 empty)
     */
    @Override
    public Optional<ClientIdentity> findByIdForUpdate(Long id) {
        return clientIdentityJpaRepository.findByIdForUpdate(id);
    }
}
