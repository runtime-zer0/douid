package kr.douid.brand.media.application.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.application.port.StoredFile;
import kr.douid.brand.media.domain.EmptyMediaFileException;
import kr.douid.brand.media.domain.InvalidMediaFileTypeException;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaInUseException;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.media.domain.MediaRepository;
import kr.douid.brand.work.application.port.WorkReferenceChecker;
import lombok.RequiredArgsConstructor;

/**
 * 미디어 생성, 삭제 유스케이스를 처리하는 서비스
 *
 * 업로드는 파일 저장 후 메타데이터를 저장하고, DB 저장 실패 시 파일을 보상 삭제한다.
 * 삭제는 Work 참조 여부 확인 후 파일과 메타데이터를 삭제한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MediaCommandService {

    private static final Logger log = LoggerFactory.getLogger(MediaCommandService.class);

    private final FileStoragePort fileStoragePort;
    private final MediaRepository mediaRepository;
    private final WorkReferenceChecker workReferenceChecker;

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
     * 미디어를 삭제
     *
     * @param id 삭제할 미디어 식별자
     * @throws MediaNotFoundException 미디어가 존재하지 않는 경우
     * @throws MediaInUseException    작업물에서 사용 중인 경우
     */
    public void delete(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(MediaNotFoundException::new);

        if (workReferenceChecker.existsByMediaId(media.getId())) {
            throw new MediaInUseException();
        }

        try {
            fileStoragePort.delete(media.getFilePath());
        } catch (Exception e) {
            log.warn("미디어 파일 삭제 실패 (id={}, path={}): {}", id, media.getFilePath(), e.getMessage());
        }

        mediaRepository.delete(media);
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
