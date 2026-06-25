package kr.douid.brand.media.presentation.response;

import java.time.LocalDateTime;

import kr.douid.brand.media.application.query.MediaView;

/**
 * 미디어 조회 API 응답 (단건 및 목록 공용)
 *
 * @param id               미디어 식별자
 * @param url              접근 가능한 URL
 * @param originalFilename 원본 파일명
 * @param contentType      MIME 타입
 * @param fileSize         파일 크기 (바이트)
 * @param createdAt        생성 시각
 */
public record MediaListResponse(
        Long id,
        String url,
        String originalFilename,
        String contentType,
        long fileSize,
        LocalDateTime createdAt) {

    /**
     * MediaView로부터 응답 생성
     *
     * @param view    변환 대상 view
     * @param baseUrl API 기본 경로 (예: /api/media)
     * @return 변환된 응답
     */
    public static MediaListResponse from(MediaView view, String baseUrl) {
        return new MediaListResponse(
                view.id(),
                baseUrl + "/" + view.id() + "/file",
                view.originalFilename(),
                view.contentType(),
                view.fileSize(),
                view.createdAt());
    }
}
