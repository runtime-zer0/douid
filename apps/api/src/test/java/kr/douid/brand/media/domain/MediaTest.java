package kr.douid.brand.media.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaTest {

    @Test
    void upload_정상_생성() {
        Media media = Media.upload("photo.jpg", "uuid-photo.jpg", "uploads/media/uuid-photo.jpg",
                "image/jpeg", 102400L);

        assertThat(media.getOriginalFilename()).isEqualTo("photo.jpg");
        assertThat(media.getStoredFilename()).isEqualTo("uuid-photo.jpg");
        assertThat(media.getFilePath()).isEqualTo("uploads/media/uuid-photo.jpg");
        assertThat(media.getContentType()).isEqualTo("image/jpeg");
        assertThat(media.getFileSize()).isEqualTo(102400L);
    }

    @Test
    void upload_저장파일명_원본파일명_분리() {
        Media media = Media.upload("my image.jpg", "abc123.jpg", "uploads/media/abc123.jpg",
                "image/jpeg", 1024L);

        assertThat(media.getOriginalFilename()).isEqualTo("my image.jpg");
        assertThat(media.getStoredFilename()).isEqualTo("abc123.jpg");
        assertThat(media.getOriginalFilename()).isNotEqualTo(media.getStoredFilename());
    }

    @Test
    void upload_png_이미지() {
        Media media = Media.upload("banner.png", "uuid-banner.png", "uploads/media/uuid-banner.png",
                "image/png", 204800L);

        assertThat(media.getContentType()).isEqualTo("image/png");
        assertThat(media.getFileSize()).isEqualTo(204800L);
    }
}
