package kr.douid.brand.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.media.application.command.MediaCommandService;
import kr.douid.brand.media.application.command.MediaResult;
import kr.douid.brand.media.application.command.MediaUploadCommand;
import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.application.port.StoredFile;
import kr.douid.brand.media.domain.EmptyMediaFileException;
import kr.douid.brand.media.domain.InvalidMediaFileTypeException;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaInUseException;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.media.domain.MediaRepository;
import kr.douid.brand.work.application.port.WorkReferenceChecker;

@ExtendWith(MockitoExtension.class)
class MediaCommandServiceTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private WorkReferenceChecker workReferenceChecker;

    private MediaCommandService mediaCommandService;

    @BeforeEach
    void setUp() {
        mediaCommandService = new MediaCommandService(fileStoragePort, mediaRepository, workReferenceChecker);
    }

    @Test
    void upload_정상_업로드() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", "image/jpeg", new ByteArrayInputStream(new byte[]{1}), 1024L);

        StoredFile storedFile = new StoredFile("uuid.jpg", "uploads/media/uuid.jpg");
        given(fileStoragePort.store(anyString(), anyString(), any(), anyLong())).willReturn(storedFile);

        Media saved = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.save(any())).willReturn(saved);

        MediaResult result = mediaCommandService.upload(command);

        assertThat(result.originalFilename()).isEqualTo("photo.jpg");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.fileSize()).isEqualTo(1024L);
    }

    @Test
    void upload_빈파일_예외() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", "image/jpeg", new ByteArrayInputStream(new byte[0]), 0L);

        assertThatThrownBy(() -> mediaCommandService.upload(command))
                .isInstanceOf(EmptyMediaFileException.class);

        then(fileStoragePort).should(never()).store(any(), any(), any(), anyLong());
        then(mediaRepository).should(never()).save(any());
    }

    @Test
    void upload_이미지아닌파일_예외() {
        MediaUploadCommand command = new MediaUploadCommand(
                "doc.pdf", "application/pdf", new ByteArrayInputStream(new byte[]{1}), 1024L);

        assertThatThrownBy(() -> mediaCommandService.upload(command))
                .isInstanceOf(InvalidMediaFileTypeException.class);

        then(fileStoragePort).should(never()).store(any(), any(), any(), anyLong());
    }

    @Test
    void upload_contentType_null_예외() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", null, new ByteArrayInputStream(new byte[]{1}), 1024L);

        assertThatThrownBy(() -> mediaCommandService.upload(command))
                .isInstanceOf(InvalidMediaFileTypeException.class);
    }

    @Test
    void upload_DB저장_실패_시_파일_보상_삭제() {
        MediaUploadCommand command = new MediaUploadCommand(
                "photo.jpg", "image/jpeg", new ByteArrayInputStream(new byte[]{1}), 1024L);

        StoredFile storedFile = new StoredFile("uuid.jpg", "uploads/media/uuid.jpg");
        given(fileStoragePort.store(anyString(), anyString(), any(), anyLong())).willReturn(storedFile);
        given(mediaRepository.save(any())).willThrow(new RuntimeException("DB 저장 실패"));

        assertThatThrownBy(() -> mediaCommandService.upload(command))
                .isInstanceOf(RuntimeException.class);

        then(fileStoragePort).should().delete("uploads/media/uuid.jpg");
    }

    @Test
    void delete_정상_삭제() {
        Media media = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.findById(1L)).willReturn(Optional.of(media));
        given(workReferenceChecker.existsByMediaId(media.getId())).willReturn(false);

        mediaCommandService.delete(1L);

        then(fileStoragePort).should().delete("uploads/media/uuid.jpg");
        then(mediaRepository).should().delete(media);
    }

    @Test
    void delete_미존재_예외() {
        given(mediaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> mediaCommandService.delete(99L))
                .isInstanceOf(MediaNotFoundException.class);

        then(mediaRepository).should(never()).delete(any());
    }

    @Test
    void delete_파일삭제_실패해도_메타데이터_삭제() {
        Media media = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.findById(1L)).willReturn(Optional.of(media));
        given(workReferenceChecker.existsByMediaId(media.getId())).willReturn(false);
        willThrow(new RuntimeException("파일 삭제 실패")).given(fileStoragePort).delete(any());

        mediaCommandService.delete(1L);

        then(mediaRepository).should().delete(media);
    }

    @Test
    void delete_작업물에서_사용중_예외() {
        Media media = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.findById(1L)).willReturn(Optional.of(media));
        given(workReferenceChecker.existsByMediaId(media.getId())).willReturn(true);

        assertThatThrownBy(() -> mediaCommandService.delete(1L))
                .isInstanceOf(MediaInUseException.class);

        then(fileStoragePort).should(never()).delete(any());
        then(mediaRepository).should(never()).delete(any());
    }
}
