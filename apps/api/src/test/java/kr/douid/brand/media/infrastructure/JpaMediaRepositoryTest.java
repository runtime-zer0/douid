package kr.douid.brand.media.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.infrastructure.persistence.MediaJpaRepository;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class JpaMediaRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private MediaJpaRepository repository;

    @Test
    void save_저장_후_조회() {
        Media media = Media.upload("photo.jpg", "uuid-photo.jpg", "uploads/media/uuid-photo.jpg",
                "image/jpeg", 1024L);

        Media saved = repository.save(media);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOriginalFilename()).isEqualTo("photo.jpg");
        assertThat(saved.getStoredFilename()).isEqualTo("uuid-photo.jpg");
        assertThat(saved.getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getFileSize()).isEqualTo(1024L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void storedFilename_유니크_제약_위반() {
        repository.save(Media.upload("photo1.jpg", "uuid.jpg", "uploads/media/uuid.jpg",
                "image/jpeg", 1024L));

        assertThatThrownBy(() -> {
            repository.save(Media.upload("photo2.jpg", "uuid.jpg", "uploads/media/uuid2.jpg",
                    "image/png", 2048L));
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findById_조회() {
        Media saved = repository.save(Media.upload("photo.jpg", "uuid-photo.jpg",
                "uploads/media/uuid-photo.jpg", "image/jpeg", 1024L));

        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.findById(9999L)).isEmpty();
    }

    @Test
    void delete_삭제_후_미존재() {
        Media saved = repository.save(Media.upload("photo.jpg", "uuid-del.jpg",
                "uploads/media/uuid-del.jpg", "image/jpeg", 1024L));
        Long id = saved.getId();

        repository.delete(saved);

        assertThat(repository.findById(id)).isEmpty();
    }
}
