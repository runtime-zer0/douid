package kr.douid.brand.client.presentation.response;

/**
 * Magic Link 복원 완료 응답
 *
 * @param recovered 복원 성공 여부
 */
public record RecoveryConfirmResponse(boolean recovered) {
}
