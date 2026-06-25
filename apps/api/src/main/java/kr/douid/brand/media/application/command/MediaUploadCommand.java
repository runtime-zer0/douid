package kr.douid.brand.media.application.command;

import java.io.InputStream;

/**
 * 미디어 업로드 요청 커맨드
 *
 * @param originalFilename 원본 파일명
 * @param contentType      MIME 타입
 * @param inputStream      파일 스트림
 * @param fileSize         파일 크기 (바이트)
 */
public record MediaUploadCommand(
        String originalFilename,
        String contentType,
        InputStream inputStream,
        long fileSize) {
}
