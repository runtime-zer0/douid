package kr.douid.brand.media.application.port;

import java.io.InputStream;

/**
 * 파일 저장소 추상화 포트
 *
 * 로컬 파일시스템과 S3 등 저장소 구현 교체를 위한 분리
 * application 계층은 이 인터페이스에만 의존
 */
public interface FileStoragePort {

    /**
     * 파일을 저장하고 저장 결과를 반환
     *
     * @param originalFilename 원본 파일명 (확장자 추출에 사용)
     * @param contentType      MIME 타입
     * @param inputStream      파일 스트림
     * @param size             파일 크기 (바이트)
     * @return 저장된 파일 정보
     */
    StoredFile store(String originalFilename, String contentType, InputStream inputStream, long size);

    /**
     * 저장된 파일을 삭제
     *
     * @param filePath 저장소 내 상대 경로
     */
    void delete(String filePath);

    /**
     * 저장된 파일을 스트림으로 읽어 반환
     *
     * @param filePath 저장소 내 상대 경로
     * @return 파일 입력 스트림
     */
    InputStream load(String filePath);
}
