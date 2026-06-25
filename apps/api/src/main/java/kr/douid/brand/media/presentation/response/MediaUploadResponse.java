package kr.douid.brand.media.presentation.response;

import java.time.LocalDateTime;

import kr.douid.brand.media.application.command.MediaResult;

/**
 * 미디어 업로드 API 응답
 *
 * @param id               미디어 식별자
 * @param url              접근 가능한 URL
 * @param originalFilename 원본 파일명
 * @param contentType      MIME 타입
 * @param fileSize         파일 크기 (바이트)
 * @param createdAt        생성 시각
 */
public record MediaUploadResponse(
        Long id,
        String url,
        String originalFilename,
        String contentType,
        long fileSize,
        LocalDateTime createdAt) {

    /**
     * MediaResult로부터 업로드 응답 생성
     *
     * @param result  업로드 결과
     * @param baseUrl API 기본 경로 (예: /api/media)
     * @return 변환된 응답
     */
    public static MediaUploadResponse from(MediaResult result, String baseUrl) {
        return new MediaUploadResponse(
                result.id(),
                baseUrl + "/" + result.id() + "/file",
                result.originalFilename(),
                result.contentType(),
                result.fileSize(),
                result.createdAt());
    }
}
