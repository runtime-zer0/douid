package kr.douid.brand.client.domain;

import java.util.Optional;

/**
 * {@link ClientIdentity} Aggregate 저장·복원을 위한 domain repository port
 */
public interface ClientIdentityRepository {

    /**
     * 상담 주체를 저장하고 반환
     *
     * @param clientIdentity 저장할 상담 주체
     * @return 저장된 상담 주체
     */
    ClientIdentity save(ClientIdentity clientIdentity);

    /**
     * ID로 상담 주체를 조회
     *
     * @param id 상담 주체 ID
     * @return 상담 주체 (없으면 empty)
     */
    Optional<ClientIdentity> findById(Long id);
}
