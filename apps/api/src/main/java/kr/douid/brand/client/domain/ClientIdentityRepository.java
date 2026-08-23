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

    /**
     * ID로 상담 주체를 비관적 락으로 조회
     *
     * 동일 상담 주체에서 거의 동시에 여러 상담 시작 요청이 발생해도 활성 Conversation
     * 조회/생성이 직렬화되도록, 아직 존재하지 않는 Conversation 행이 아니라 이미 존재하는
     * {@link ClientIdentity} 행에 락을 건다(FR-026).
     *
     * @param id 상담 주체 ID
     * @return 상담 주체 (없으면 empty)
     */
    Optional<ClientIdentity> findByIdForUpdate(Long id);
}
