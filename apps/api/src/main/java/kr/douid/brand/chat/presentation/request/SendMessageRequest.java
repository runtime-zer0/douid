package kr.douid.brand.chat.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 메시지 전송 요청
 *
 * @param content 메시지 본문 (필수, 공백 불가)
 */
public record SendMessageRequest(@NotBlank String content) {
}
