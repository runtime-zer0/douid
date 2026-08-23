package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.client.domain.EmailVerificationChallenge;

/**
 * {@link EmailVerificationChallenge} Spring Data JPA 기술 전용 repository
 */
public interface EmailVerificationChallengeJpaRepository extends JpaRepository<EmailVerificationChallenge, Long> {

    /**
     * 특정 상담 주체·이메일 조합의 가장 최근 챌린지를 조회
     *
     * @param clientIdentityId 챌린지를 발급받은 상담 주체 ID
     * @param normalizedEmail   검증 대상 이메일(정규화)
     * @return 가장 최근 챌린지 (없으면 empty)
     */
    Optional<EmailVerificationChallenge> findFirstByClientIdentityIdAndNormalizedEmailOrderByCreatedAtDesc(
            Long clientIdentityId, String normalizedEmail);
}
