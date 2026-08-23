package kr.douid.brand.client.presentation.response;

import java.time.LocalDateTime;

/**
 * 이메일 인증 코드 검증 응답
 *
 * @param email      검증 완료된 이메일 주소
 * @param verifiedAt 검증 완료 시각
 */
public record EmailVerificationResponse(String email, LocalDateTime verifiedAt) {
}
