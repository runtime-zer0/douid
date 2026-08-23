package kr.douid.brand.client.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Recovery Email 등록 요청
 *
 * @param email 등록할 이메일 주소
 */
public record RegisterEmailRequest(@NotBlank @Email String email) {
}
