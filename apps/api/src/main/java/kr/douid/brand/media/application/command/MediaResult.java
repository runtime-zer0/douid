package kr.douid.brand.media.application.command;

import java.time.LocalDateTime;

import kr.douid.brand.media.domain.Media;

/**
 * 미디어 커맨드 처리 결과
 *
 * @param id               미디어 식별자
 * @param originalFilename 원본 파일명
 * @param filePath         저장소 내 상대 경로
 * @param contentType      MIME 타입
 * @param fileSize         파일 크기 (바이트)
 * @param createdAt        생성 시각
 */
public record MediaResult(
        Long id,
        String originalFilename,
        String filePath,
        String contentType,
        long fileSize,
        LocalDateTime createdAt) {

    /**
     * Media 엔티티로부터 결과 생성
     *
     * @param media 변환 대상 미디어
     * @return 변환된 결과
     */
    public static MediaResult from(Media media) {
        return new MediaResult(
                media.getId(),
                media.getOriginalFilename(),
                media.getFilePath(),
                media.getContentType(),
                media.getFileSize(),
                media.getCreatedAt());
    }
}
