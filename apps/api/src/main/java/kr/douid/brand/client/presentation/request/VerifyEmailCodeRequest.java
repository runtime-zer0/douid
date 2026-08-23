package kr.douid.brand.client.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 이메일 인증 코드 검증 요청
 *
 * @param email 검증할 이메일 주소
 * @param code  6자리 숫자 인증 코드
 */
public record VerifyEmailCodeRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다.") String code) {
}
