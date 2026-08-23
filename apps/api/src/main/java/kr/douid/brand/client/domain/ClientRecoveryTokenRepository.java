package kr.douid.brand.client.domain;

import java.util.Optional;

/**
 * {@link ClientRecoveryToken} 저장·조회를 위한 domain repository port
 */
public interface ClientRecoveryTokenRepository {

    /**
     * Recovery Token을 저장하고 반환
     *
     * @param token 저장할 토큰
     * @return 저장된 토큰
     */
    ClientRecoveryToken save(ClientRecoveryToken token);

    /**
     * 토큰 해시로 Recovery Token을 비관적 락으로 조회
     *
     * 동일 토큰에 대한 동시 소비 요청이 있어도 성공적인 소비가 최대 한 번만 일어나도록 직렬화한다.
     *
     * @param tokenHash 조회할 토큰 해시
     * @return Recovery Token (없으면 empty)
     */
    Optional<ClientRecoveryToken> findByTokenHashForUpdate(String tokenHash);
}
