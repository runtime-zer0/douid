package kr.douid.brand.media.infrastructure.query;

import java.time.LocalDateTime;

import kr.douid.brand.media.application.query.MediaView;

/**
 * QueryDSL 조회용 미디어 projection
 *
 * {@link MediaView} 변환 후 application 계층 반환
 */
public record MediaListProjection(
        Long id,
        String originalFilename,
        String filePath,
        String contentType,
        long fileSize,
        LocalDateTime createdAt) {

    /**
     * projection을 application query DTO로 변환
     *
     * @return 변환된 {@link MediaView}
     */
    public MediaView toView() {
        return new MediaView(
                id,
                originalFilename,
                filePath,
                contentType,
                fileSize,
                createdAt);
    }
}
