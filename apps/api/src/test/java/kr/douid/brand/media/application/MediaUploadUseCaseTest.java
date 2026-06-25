package kr.douid.brand.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.media.application.command.MediaResult;
import kr.douid.brand.media.application.command.MediaUploadCommand;
import kr.douid.brand.media.application.command.MediaUploadUseCase;
import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.application.port.StoredFile;
import kr.douid.brand.media.domain.EmptyMediaFileException;
import kr.douid.brand.media.domain.InvalidMediaFileTypeException;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaRepository;

@ExtendWith(MockitoExtension.class)
class MediaUploadUseCaseTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private MediaRepository mediaRepository;

    private MediaUploadUseCase mediaUploadUseCase;

    @BeforeEach
    void setUp() {
        mediaUploadUseCase = new MediaUploadUseCase(fileStoragePort, mediaRepository);
    }

    @Test
    void upload_정상_업로드() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", "image/jpeg", new ByteArrayInputStream(new byte[]{1}), 1024L);

        StoredFile storedFile = new StoredFile("uuid.jpg", "uploads/media/uuid.jpg");
        given(fileStoragePort.store(anyString(), anyString(), any(), anyLong())).willReturn(storedFile);

        Media saved = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.save(any())).willReturn(saved);

        MediaResult result = mediaUploadUseCase.upload(command);

        assertThat(result.originalFilename()).isEqualTo("photo.jpg");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.fileSize()).isEqualTo(1024L);
    }

    @Test
    void upload_빈파일_예외() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", "image/jpeg", new ByteArrayInputStream(new byte[0]), 0L);

        assertThatThrownBy(() -> mediaUploadUseCase.upload(command))
                .isInstanceOf(EmptyMediaFileException.class);

        then(fileStoragePort).should(never()).store(any(), any(), any(), anyLong());
        then(mediaRepository).should(never()).save(any());
    }

    @Test
    void upload_이미지아닌파일_예외() {
        MediaUploadCommand command = new MediaUploadCommand(
                "doc.pdf", "application/pdf", new ByteArrayInputStream(new byte[]{1}), 1024L);

        assertThatThrownBy(() -> mediaUploadUseCase.upload(command))
                .isInstanceOf(InvalidMediaFileTypeException.class);

        then(fileStoragePort).should(never()).store(any(), any(), any(), anyLong());
    }

    @Test
    void upload_contentType_null_예외() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", null, new ByteArrayInputStream(new byte[]{1}), 1024L);

        assertThatThrownBy(() -> mediaUploadUseCase.upload(command))
                .isInstanceOf(InvalidMediaFileTypeException.class);
    }

    @Test
    void upload_DB저장_실패_시_파일_보상_삭제() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", "image/jpeg", new ByteArrayInputStream(new byte[]{1}), 1024L);

        StoredFile storedFile = new StoredFile("uuid.jpg", "uploads/media/uuid.jpg");
        given(fileStoragePort.store(anyString(), anyString(), any(), anyLong())).willReturn(storedFile);
        given(mediaRepository.save(any())).willThrow(new RuntimeException("DB 저장 실패"));

        assertThatThrownBy(() -> mediaUploadUseCase.upload(command))
                .isInstanceOf(RuntimeException.class);

        then(fileStoragePort).should().delete("uploads/media/uuid.jpg");
    }
}
