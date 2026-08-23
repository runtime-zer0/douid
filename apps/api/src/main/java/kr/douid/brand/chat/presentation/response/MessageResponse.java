package kr.douid.brand.chat.presentation.response;

import java.time.LocalDateTime;

/**
 * 상담 메시지 응답
 *
 * @param id        메시지 ID
 * @param content   메시지 본문
 * @param createdAt 작성 시각
 */
public record MessageResponse(Long id, String content, LocalDateTime createdAt) {
}
