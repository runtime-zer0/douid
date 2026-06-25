package kr.douid.brand.media.application.query;

import java.io.InputStream;

/**
 * 미디어 파일 스트리밍 결과
 *
 * @param originalFilename 원본 파일명
 * @param contentType      MIME 타입
 * @param inputStream      파일 입력 스트림
 */
public record MediaFileResult(
        String originalFilename,
        String contentType,
        InputStream inputStream) {
}
