package kr.douid.brand.media.application.query;

import java.time.LocalDateTime;

/**
 * 미디어 조회 read model
 *
 * @param id               미디어 식별자
 * @param originalFilename 원본 파일명
 * @param filePath         저장소 내 경로 (파일 서빙에 사용)
 * @param contentType      MIME 타입
 * @param fileSize         파일 크기 (바이트)
 * @param createdAt        생성 시각
 */
public record MediaView(
        Long id,
        String originalFilename,
        String filePath,
        String contentType,
        long fileSize,
        LocalDateTime createdAt) {
}
