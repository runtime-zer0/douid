package kr.douid.brand.media.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.application.port.MediaFileSaveException;
import kr.douid.brand.media.application.port.StoredFile;

/**
 * 로컬 파일시스템 기반 {@link FileStoragePort} 구현체
 *
 * S3/CloudFront로 전환 시 이 Adapter만 교체하면 된다.
 * application 계층과 domain 수정 없이 저장소 교체 가능
 */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path uploadDir;

    /**
     * 로컬 파일 저장소 초기화
     *
     * @param uploadDirPath 파일 저장 루트 경로 (application.yaml의 media.upload.dir)
     */
    public LocalFileStorageAdapter(@Value("${media.upload.dir}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 디렉토리를 생성할 수 없습니다: " + uploadDirPath, e);
        }
    }

    /**
     * 파일을 로컬 저장소에 저장하고 저장 결과를 반환
     *
     * UUID + 원본 확장자로 저장 파일명을 생성해 충돌 방지
     * 반환하는 storageKey는 uploadDir 기준 상대 키이며 DB에 저장되는 값이다.
     *
     * @param originalFilename 원본 파일명
     * @param contentType      MIME 타입 (미사용, 확장자는 originalFilename에서 추출)
     * @param inputStream      파일 스트림
     * @param size             파일 크기 (바이트)
     * @return 저장된 파일 정보
     * @throws MediaFileSaveException 저장 실패 시
     */
    @Override
    public StoredFile store(String originalFilename, String contentType, InputStream inputStream, long size) {
        String extension = extractExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;
        String storageKey = "uploads/media/" + storedFilename;
        Path targetPath = uploadDir.resolve(storedFilename);

        try {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MediaFileSaveException(e);
        }

        return new StoredFile(storedFilename, storageKey);
    }

    /**
     * 로컬 저장소에서 파일을 삭제
     *
     * @param storageKey DB에 저장된 상대 키 (예: uploads/media/uuid.jpg)
     */
    @Override
    public void delete(String storageKey) {
        String filename = extractFilename(storageKey);
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new MediaFileSaveException(e);
        }
    }

    /**
     * 로컬 저장소에서 파일 스트림을 반환
     *
     * @param storageKey DB에 저장된 상대 키 (예: uploads/media/uuid.jpg)
     * @return 파일 입력 스트림
     * @throws MediaFileSaveException 파일 읽기 실패 시
     */
    @Override
    public InputStream load(String storageKey) {
        String filename = extractFilename(storageKey);
        try {
            return Files.newInputStream(uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new MediaFileSaveException(e);
        }
    }

    /**
     * 파일명에서 확장자를 추출
     *
     * @param filename 원본 파일명
     * @return 확장자 (점 포함, 없으면 빈 문자열)
     */
    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }

    /**
     * storageKey에서 파일명 부분만 추출
     *
     * @param storageKey 상대 키 (예: uploads/media/uuid.jpg)
     * @return 파일명 (예: uuid.jpg)
     */
    private String extractFilename(String storageKey) {
        int slashIndex = storageKey.lastIndexOf('/');
        return slashIndex >= 0 ? storageKey.substring(slashIndex + 1) : storageKey;
    }
}
