package kr.douid.brand.media.application.port;

/**
 * 파일 저장 결과
 *
 * @param storedFilename UUID 기반으로 생성된 저장 파일명
 * @param filePath       저장소 내 상대 경로
 */
public record StoredFile(String storedFilename, String filePath) {
}
