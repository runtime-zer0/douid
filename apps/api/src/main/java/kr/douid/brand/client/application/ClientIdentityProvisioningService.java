package kr.douid.brand.client.application;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.client.domain.ClientCredential;
import kr.douid.brand.client.domain.ClientCredentialRepository;
import kr.douid.brand.client.domain.ClientIdentity;
import kr.douid.brand.client.domain.ClientIdentityRepository;
import lombok.RequiredArgsConstructor;

/**
 * 새 상담 주체와 credential을 함께 발급하는 서비스
 *
 * 유효한 기존 credential이 없는 상태에서 실제 상담 시작 요청이 있을 때만 호출된다(FR-002, FR-003).
 */
@Service
@RequiredArgsConstructor
public class ClientIdentityProvisioningService {

    private static final int TOKEN_EXPIRATION_DAYS = 180;

    private final ClientIdentityRepository clientIdentityRepository;
    private final ClientCredentialRepository clientCredentialRepository;
    private final ClientCredentialIssuer clientCredentialIssuer;

    /**
     * 새 상담 주체와 credential을 생성
     *
     * @return 발급된 상담 주체 ID와 raw token
     */
    @Transactional
    public ProvisionedClient provision() {
        ClientIdentity clientIdentity = clientIdentityRepository.save(ClientIdentity.create());

        String rawToken = clientCredentialIssuer.issueRawToken();
        String tokenHash = clientCredentialIssuer.hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(TOKEN_EXPIRATION_DAYS);

        clientCredentialRepository.save(
                ClientCredential.issue(clientIdentity.getId(), tokenHash, expiresAt));

        return new ProvisionedClient(clientIdentity.getId(), rawToken);
    }

    /**
     * 새로 발급된 상담 주체 ID와 raw token
     *
     * @param clientIdentityId 발급된 상담 주체 ID
     * @param rawToken          발급된 raw client_token
     */
    public record ProvisionedClient(Long clientIdentityId, String rawToken) {
    }
}
