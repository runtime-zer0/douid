package kr.douid.brand.client.presentation.response;

/**
 * Recovery Email 등록 응답
 *
 * @param email               등록 요청한 이메일 주소
 * @param codeExpiresInSeconds 발송된 인증 코드의 유효기간(초)
 * @param alreadyVerified      이미 본인 소유로 검증 완료된 이메일이라 재발송을 건너뛴 경우 true
 */
public record EmailRegistrationResponse(String email, long codeExpiresInSeconds, boolean alreadyVerified) {
}
