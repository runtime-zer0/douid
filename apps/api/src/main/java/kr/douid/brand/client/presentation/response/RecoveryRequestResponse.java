package kr.douid.brand.client.presentation.response;

/**
 * Magic Link 복원 요청 응답
 *
 * 이메일 등록 여부와 무관하게 항상 동일한 메시지를 반환한다(FR-014).
 *
 * @param message 요청 접수 안내 메시지
 */
public record RecoveryRequestResponse(String message) {
}
