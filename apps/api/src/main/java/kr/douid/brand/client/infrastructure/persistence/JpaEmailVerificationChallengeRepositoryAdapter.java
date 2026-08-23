package kr.douid.brand.client.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.client.domain.EmailVerificationChallenge;
import kr.douid.brand.client.domain.EmailVerificationChallengeRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link EmailVerificationChallengeRepository} domain port의 JPA 구현체
 *
 * {@link EmailVerificationChallengeJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaEmailVerificationChallengeRepositoryAdapter implements EmailVerificationChallengeRepository {

    private final EmailVerificationChallengeJpaRepository emailVerificationChallengeJpaRepository;

    @Override
    public EmailVerificationChallenge save(EmailVerificationChallenge challenge) {
        return emailVerificationChallengeJpaRepository.save(challenge);
    }

    @Override
    public Optional<EmailVerificationChallenge> findLatestByClientIdentityIdAndNormalizedEmail(
            Long clientIdentityId, String normalizedEmail) {
        return emailVerificationChallengeJpaRepository
                .findFirstByClientIdentityIdAndNormalizedEmailOrderByCreatedAtDesc(clientIdentityId,
                        normalizedEmail);
    }
}
