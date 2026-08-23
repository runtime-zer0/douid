package kr.douid.brand.client.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Magic Link Recovery Token 검증 요청
 *
 * @param token Magic Link의 raw Recovery Token
 */
public record RecoveryConfirmRequest(@NotBlank String token) {
}
