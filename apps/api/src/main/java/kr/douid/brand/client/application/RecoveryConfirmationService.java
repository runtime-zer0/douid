package kr.douid.brand.client.application;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.client.domain.ClientCredential;
import kr.douid.brand.client.domain.ClientCredentialRepository;
import kr.douid.brand.client.domain.ClientRecoveryToken;
import kr.douid.brand.client.domain.ClientRecoveryTokenRepository;
import kr.douid.brand.client.domain.RecoveryTokenExpiredException;
import kr.douid.brand.client.domain.RecoveryTokenInvalidException;
import lombok.RequiredArgsConstructor;

/**
 * Magic Link Recovery Token 검증 및 상담 주체 복원 유스케이스
 *
 * 현재 요청의 {@code ClientIdentityContext}(브라우저에 이미 있을 수 있는 임시 상담 주체)를 전혀
 * 참조하지 않고, 오직 Recovery Token이 가리키는 {@code clientIdentityId}만 사용한다(FR-028, Story 5).
 * 기존 credential은 재사용하지 않고 항상 새로 발급하며(FR-021), 기존 credential을 삭제·수정하지
 * 않는다(FR-024).
 */
@Service
@RequiredArgsConstructor
public class RecoveryConfirmationService {

    private static final int CREDENTIAL_EXPIRATION_DAYS = 180;

    private final ClientRecoveryTokenRepository clientRecoveryTokenRepository;
    private final ClientCredentialRepository clientCredentialRepository;
    private final RecoveryTokenIssuer recoveryTokenIssuer;
    private final ClientCredentialIssuer clientCredentialIssuer;

    /**
     * Recovery Token을 검증해 상담 주체를 복원하고 새 credential을 발급
     *
     * @param rawToken Magic Link의 raw Recovery Token
     * @return 발급된 raw client_token
     * @throws RecoveryTokenInvalidException 토큰이 존재하지 않거나 이미 소비된 경우
     * @throws RecoveryTokenExpiredException 토큰이 만료된 경우
     */
    @Transactional
    public String confirm(String rawToken) {
        String tokenHash = recoveryTokenIssuer.hash(rawToken);

        ClientRecoveryToken token = clientRecoveryTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(RecoveryTokenInvalidException::new);

        token.consume(LocalDateTime.now());

        String rawClientToken = clientCredentialIssuer.issueRawToken();
        String clientTokenHash = clientCredentialIssuer.hash(rawClientToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(CREDENTIAL_EXPIRATION_DAYS);

        clientCredentialRepository.save(
                ClientCredential.issue(token.getClientIdentityId(), clientTokenHash, expiresAt));

        return rawClientToken;
    }
}
