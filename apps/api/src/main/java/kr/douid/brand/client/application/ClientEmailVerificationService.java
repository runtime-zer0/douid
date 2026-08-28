package kr.douid.brand.client.application;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.domain.ClientEmailRepository;
import kr.douid.brand.client.domain.EmailAlreadyOwnedException;
import kr.douid.brand.client.domain.EmailVerificationChallenge;
import kr.douid.brand.client.domain.EmailVerificationChallengeRepository;
import kr.douid.brand.client.domain.VerificationCodeExpiredException;
import lombok.RequiredArgsConstructor;

/**
 * 이메일 인증 코드 검증 유스케이스
 *
 * 코드 발급 당시의 상담 주체 credential로만 검증이 가능하다(Story 2 방어) — 다른 상담 주체의 챌린지는
 * 애초에 조회되지 않는다. 동일 normalized email이 동시에 여러 상담 주체의 검증 완료 이메일이 되는 걸
 * 막는 최종 방어선은 {@code client_email.normalized_email}의 조건부(partial) unique index다
 * (FR-011) — 사전 확인(등록 시점)은 UX상 빠른 실패일 뿐이고, 이 index 위반이 실질적 유일성 보장이다.
 */
@Service
@RequiredArgsConstructor
public class ClientEmailVerificationService {

    private final ClientEmailRepository clientEmailRepository;
    private final EmailVerificationChallengeRepository emailVerificationChallengeRepository;
    private final EmailNormalizer emailNormalizer;
    private final EmailVerificationCodeIssuer emailVerificationCodeIssuer;

    /**
     * 제출된 인증 코드를 검증해 이메일을 VERIFIED 상태로 전환
     *
     * @param clientIdentityId 코드를 제출한 상담 주체 ID
     * @param email             검증할 이메일 주소
     * @param rawCode           제출된 raw 인증 코드
     * @return 검증 완료된 이메일
     * @throws VerificationCodeExpiredException 챌린지가 없거나 만료·소비·시도초과된 경우
     * @throws kr.douid.brand.client.domain.VerificationCodeInvalidException 코드가 일치하지 않는 경우
     * @throws EmailAlreadyOwnedException 사전 확인 이후 경쟁 조건으로 다른 상담 주체가 먼저 검증 완료했거나,
     *         이미 검증 완료된 이메일인 경우
     */
    @Transactional
    public ClientEmail verify(Long clientIdentityId, String email, String rawCode) {
        String normalizedEmail = emailNormalizer.normalize(email);

        EmailVerificationChallenge challenge = emailVerificationChallengeRepository
                .findLatestByClientIdentityIdAndNormalizedEmail(clientIdentityId, normalizedEmail)
                .orElseThrow(VerificationCodeExpiredException::new);

        String candidateHash = emailVerificationCodeIssuer.hash(rawCode);
        challenge.verify(candidateHash, LocalDateTime.now());

        if (clientEmailRepository.findByNormalizedEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyOwnedException();
        }

        try {
            return clientEmailRepository.save(
                    ClientEmail.verify(clientIdentityId, email, normalizedEmail, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyOwnedException();
        }
    }
}
