package kr.douid.brand.media.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.media.application.command.MediaDeleteCommandService;
import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaDeletionPolicy;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.media.domain.MediaRepository;

@ExtendWith(MockitoExtension.class)
class MediaDeleteCommandServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private MediaDeletionPolicy deletionPolicy;

    private MediaDeleteCommandService mediaDeleteCommandService;

    @BeforeEach
    void setUp() {
        mediaDeleteCommandService = new MediaDeleteCommandService(
                mediaRepository, fileStoragePort, List.of(deletionPolicy));
    }

    @Test
    void delete_정상_삭제() {
        Media media = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.findById(1L)).willReturn(Optional.of(media));

        mediaDeleteCommandService.delete(1L);

        then(deletionPolicy).should().validate(media);
        then(fileStoragePort).should().delete("uploads/media/uuid.jpg");
        then(mediaRepository).should().delete(media);
    }

    @Test
    void delete_미존재_예외() {
        given(mediaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> mediaDeleteCommandService.delete(99L))
                .isInstanceOf(MediaNotFoundException.class);

        then(mediaRepository).should(never()).delete(any());
    }

    @Test
    void delete_파일삭제_실패해도_메타데이터_삭제() {
        Media media = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.findById(1L)).willReturn(Optional.of(media));
        willThrow(new RuntimeException("파일 삭제 실패")).given(fileStoragePort).delete(any());

        // 파일 삭제 실패 시 예외를 던지지 않고 메타데이터 삭제를 계속 진행
        mediaDeleteCommandService.delete(1L);

        then(mediaRepository).should().delete(media);
    }

    @Test
    void delete_삭제정책_거부_예외() {
        Media media = Media.upload("photo.jpg", "uuid.jpg", "uploads/media/uuid.jpg", "image/jpeg", 1024L);
        given(mediaRepository.findById(1L)).willReturn(Optional.of(media));
        willThrow(new RuntimeException("정책 위반")).given(deletionPolicy).validate(media);

        assertThatThrownBy(() -> mediaDeleteCommandService.delete(1L))
                .isInstanceOf(RuntimeException.class);

        then(fileStoragePort).should(never()).delete(any());
        then(mediaRepository).should(never()).delete(any());
    }
}
