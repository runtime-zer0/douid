package kr.douid.brand.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.application.query.MediaFileResult;
import kr.douid.brand.media.application.query.MediaQueryRepository;
import kr.douid.brand.media.application.query.MediaQueryService;
import kr.douid.brand.media.application.query.MediaView;
import kr.douid.brand.media.domain.MediaNotFoundException;

@ExtendWith(MockitoExtension.class)
class MediaQueryServiceTest {

    @Mock
    private MediaQueryRepository mediaQueryRepository;

    @Mock
    private FileStoragePort fileStoragePort;

    @InjectMocks
    private MediaQueryService mediaQueryService;

    @Test
    void getMedia_단건_조회() {
        MediaView view = fakeMediaView(1L, "photo.jpg");
        given(mediaQueryRepository.findById(1L)).willReturn(Optional.of(view));

        MediaView result = mediaQueryService.getMedia(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.originalFilename()).isEqualTo("photo.jpg");
    }

    @Test
    void getMedia_미존재_예외() {
        given(mediaQueryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> mediaQueryService.getMedia(99L))
                .isInstanceOf(MediaNotFoundException.class);
    }

    @Test
    void getMediaList_목록_페이지네이션() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<MediaView> page = new PageImpl<>(
                List.of(fakeMediaView(2L, "banner.png"), fakeMediaView(1L, "photo.jpg")),
                pageable, 2);
        given(mediaQueryRepository.findAll(pageable)).willReturn(page);

        Page<MediaView> result = mediaQueryService.getMediaList(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).id()).isEqualTo(2L);
    }

    @Test
    void getMediaList_빈목록() {
        Pageable pageable = PageRequest.of(0, 20);
        given(mediaQueryRepository.findAll(pageable)).willReturn(Page.empty(pageable));

        Page<MediaView> result = mediaQueryService.getMediaList(pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getMediaFile_파일스트림_조회() {
        MediaView view = fakeMediaView(1L, "photo.jpg");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        given(mediaQueryRepository.findById(1L)).willReturn(Optional.of(view));
        given(fileStoragePort.load(view.filePath())).willReturn(inputStream);

        MediaFileResult result = mediaQueryService.getMediaFile(1L);

        assertThat(result.originalFilename()).isEqualTo("photo.jpg");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.inputStream()).isSameAs(inputStream);
    }

    private MediaView fakeMediaView(Long id, String originalFilename) {
        return new MediaView(id, originalFilename, "uploads/media/uuid.jpg",
                "image/jpeg", 1024L, LocalDateTime.now());
    }
}
