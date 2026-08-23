package kr.douid.brand.chat.application.query;

import java.time.LocalDateTime;

/**
 * 메시지 목록 조회 결과
 *
 * @param id        메시지 ID
 * @param content   메시지 본문
 * @param createdAt 작성 시각
 */
public record MessageView(Long id, String content, LocalDateTime createdAt) {
}
