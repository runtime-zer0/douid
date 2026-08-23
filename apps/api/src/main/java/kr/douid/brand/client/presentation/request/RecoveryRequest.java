package kr.douid.brand.client.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Magic Link 기반 상담 복원 요청
 *
 * @param email 복원을 요청할 이메일 주소
 */
public record RecoveryRequest(@NotBlank @Email String email) {
}
