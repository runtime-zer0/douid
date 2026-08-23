package kr.douid.brand.client.domain;

import java.util.Optional;

/**
 * {@link EmailVerificationChallenge} 저장·조회를 위한 domain repository port
 */
public interface EmailVerificationChallengeRepository {

    /**
     * 챌린지를 저장하고 반환
     *
     * @param challenge 저장할 챌린지
     * @return 저장된 챌린지
     */
    EmailVerificationChallenge save(EmailVerificationChallenge challenge);

    /**
     * 특정 상담 주체·이메일 조합의 가장 최근 챌린지를 조회
     *
     * 다른 상담 주체가 발급받은 챌린지는 조회되지 않는다(Story 2 방어).
     *
     * @param clientIdentityId 챌린지를 발급받은 상담 주체 ID
     * @param normalizedEmail   검증 대상 이메일(정규화)
     * @return 가장 최근 챌린지 (없으면 empty)
     */
    Optional<EmailVerificationChallenge> findLatestByClientIdentityIdAndNormalizedEmail(Long clientIdentityId,
            String normalizedEmail);
}
