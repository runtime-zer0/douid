package kr.douid.brand.client.domain;

import java.util.Optional;

/**
 * {@link ClientCredential} 저장·조회를 위한 domain repository port
 */
public interface ClientCredentialRepository {

    /**
     * credential을 저장하고 반환
     *
     * @param clientCredential 저장할 credential
     * @return 저장된 credential
     */
    ClientCredential save(ClientCredential clientCredential);

    /**
     * 토큰 해시로 credential을 조회
     *
     * @param tokenHash 조회할 토큰 해시
     * @return credential (없으면 empty)
     */
    Optional<ClientCredential> findByTokenHash(String tokenHash);
}
