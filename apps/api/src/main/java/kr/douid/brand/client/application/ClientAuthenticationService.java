package kr.douid.brand.client.application;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import kr.douid.brand.client.domain.ClientCredential;
import kr.douid.brand.client.domain.ClientCredentialRepository;
import lombok.RequiredArgsConstructor;

/**
 * raw client_token으로 현재 상담 주체를 해석하는 인증 서비스
 *
 * credential 검증(존재/미만료/미폐기)을 통과해야만 상담 주체 ID를 반환한다.
 */
@Service
@RequiredArgsConstructor
public class ClientAuthenticationService {

    private final ClientCredentialRepository clientCredentialRepository;
    private final ClientCredentialIssuer clientCredentialIssuer;

    /**
     * raw token으로 상담 주체 ID를 해석
     *
     * @param rawToken 브라우저가 전달한 raw token
     * @return 유효한 credential이면 상담 주체 ID, 아니면 empty
     */
    public Optional<Long> resolve(String rawToken) {
        String tokenHash = clientCredentialIssuer.hash(rawToken);
        return clientCredentialRepository.findByTokenHash(tokenHash)
                .filter(credential -> credential.isValid(LocalDateTime.now()))
                .map(ClientCredential::getClientIdentityId);
    }
}
