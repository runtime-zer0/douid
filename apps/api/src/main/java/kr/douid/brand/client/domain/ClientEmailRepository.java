package kr.douid.brand.client.domain;

import java.util.Optional;

/**
 * {@link ClientEmail} 저장·조회를 위한 domain repository port
 */
public interface ClientEmailRepository {

    /**
     * 검증 완료된 이메일을 저장하고 반환
     *
     * @param clientEmail 저장할 이메일
     * @return 저장된 이메일
     */
    ClientEmail save(ClientEmail clientEmail);

    /**
     * normalized email로 검증 완료된 이메일을 조회
     *
     * 상담 주체와 무관하게 이 normalized email을 검증 완료한 레코드가 있는지 확인하는 데 사용한다
     * (FR-011 재조회, FR-013 Recovery 조회에 공용 사용).
     *
     * @param normalizedEmail 조회할 정규화 이메일
     * @return 검증 완료된 이메일 (없으면 empty)
     */
    Optional<ClientEmail> findByNormalizedEmail(String normalizedEmail);

    /**
     * 특정 상담 주체가 소유한 검증 완료된 이메일을 조회
     *
     * 동일 상담 주체의 중복 등록 여부를 판단하는 데 사용한다.
     *
     * @param clientIdentityId 상담 주체 ID
     * @param normalizedEmail   조회할 정규화 이메일
     * @return 검증 완료된 이메일 (없으면 empty)
     */
    Optional<ClientEmail> findVerifiedByClientIdentityIdAndNormalizedEmail(Long clientIdentityId,
            String normalizedEmail);

    /**
     * 동일 normalized email에 대한 검증 완료 요청을 직렬화하는 트랜잭션 범위 락
     *
     * {@code client_email}은 최초 검증 완료 시점 전까지 해당 normalized email의 row가 존재하지
     * 않아 row 단위 비관적 락을 걸 수 없다. 두 상담 주체가 동일 이메일을 거의 동시에 검증 완료
     * 시도할 때 재조회만으로는 막을 수 없는 경쟁 조건을 막기 위해 이 메서드로 트랜잭션을 직렬화한
     * 뒤 재조회한다(FR-011, research.md #10).
     *
     * @param normalizedEmail 락을 걸 정규화 이메일
     */
    void lockByNormalizedEmail(String normalizedEmail);
}
