package kr.douid.brand.media.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.application.port.StoredFile;
import kr.douid.brand.media.domain.EmptyMediaFileException;
import kr.douid.brand.media.domain.InvalidMediaFileTypeException;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaRepository;
import lombok.RequiredArgsConstructor;

/**
 * 이미지 업로드 유스케이스
 *
 * MIME 타입 검사 → 파일 저장 → 메타데이터 저장 순서
 * DB 저장 실패 시 이미 저장된 파일 보상 삭제
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MediaUploadUseCase {

    private final FileStoragePort fileStoragePort;
    private final MediaRepository mediaRepository;

    /**
     * 이미지 파일을 업로드하고 미디어 메타데이터를 저장
     *
     * DB 저장 실패 시 저장된 파일 보상 삭제
     *
     * @param command 업로드 요청 커맨드
     * @return 저장된 미디어 결과
     * @throws EmptyMediaFileException       빈 파일인 경우
     * @throws InvalidMediaFileTypeException 이미지 타입이 아닌 경우
     */
    public MediaResult upload(MediaUploadCommand command) {
        validateImageFile(command);

        StoredFile storedFile = fileStoragePort.store(
                command.originalFilename(),
                command.contentType(),
                command.inputStream(),
                command.fileSize());

        try {
            Media media = Media.upload(
                    command.originalFilename(),
                    storedFile.storedFilename(),
                    storedFile.filePath(),
                    command.contentType(),
                    command.fileSize());

            return MediaResult.from(mediaRepository.save(media));
        } catch (Exception e) {
            fileStoragePort.delete(storedFile.filePath());
            throw e;
        }
    }

    /**
     * 업로드 파일의 MIME 타입과 크기를 검증
     *
     * @param command 검증 대상 커맨드
     * @throws EmptyMediaFileException        빈 파일인 경우
     * @throws InvalidMediaFileTypeException  이미지 타입이 아닌 경우
     */
    private void validateImageFile(MediaUploadCommand command) {
        if (command.fileSize() == 0) {
            throw new EmptyMediaFileException();
        }
        if (command.contentType() == null || !command.contentType().startsWith("image/")) {
            throw new InvalidMediaFileTypeException();
        }
    }
}
