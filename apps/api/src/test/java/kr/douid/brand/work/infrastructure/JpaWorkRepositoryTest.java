package kr.douid.brand.work.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.infrastructure.persistence.CategoryJpaRepository;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.infrastructure.persistence.MediaJpaRepository;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkMediaItem;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkVisibility;
import kr.douid.brand.work.infrastructure.persistence.WorkJpaRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class JpaWorkRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private WorkJpaRepository repository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private MediaJpaRepository mediaJpaRepository;

    private Long categoryId;
    private Long otherCategoryId;

    @BeforeEach
    void setUp() {
        categoryId = categoryJpaRepository.save(Category.create("브랜딩", "branding", 0, true)).getId();
        otherCategoryId = categoryJpaRepository.save(Category.create("웹디자인", "web-design", 1, true)).getId();
    }

    @Test
    void save_저장_후_조회() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", categoryId,
                WorkVisibility.VISIBLE);

        Work saved = repository.save(work);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSlug()).isEqualTo("brand-renewal");
        assertThat(saved.getVisibility()).isEqualTo(WorkVisibility.VISIBLE);
    }

    @Test
    void slug_유니크_제약_위반() {
        repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", categoryId,
                WorkVisibility.VISIBLE));

        assertThatThrownBy(() -> {
            repository.save(Work.create("다른 작업물", "brand-renewal", "요약", "상세", otherCategoryId,
                    WorkVisibility.HIDDEN));
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsBySlug_존재() {
        repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", categoryId,
                WorkVisibility.VISIBLE));

        assertThat(repository.existsBySlug("brand-renewal")).isTrue();
        assertThat(repository.existsBySlug("missing")).isFalse();
    }

    @Test
    void existsBySlugAndIdNot_자기자신_제외() {
        Work saved = repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", categoryId,
                WorkVisibility.VISIBLE));

        assertThat(repository.existsBySlugAndIdNot("brand-renewal", saved.getId())).isFalse();
    }

    @Test
    void existsByCategoryId_카테고리_참조_확인() {
        repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", categoryId,
                WorkVisibility.VISIBLE));

        assertThat(repository.existsByCategoryId(categoryId)).isTrue();
        assertThat(repository.existsByCategoryId(otherCategoryId)).isFalse();
    }

    @Test
    void existsByMediaItems_MediaId_미디어_참조_확인() {
        Long mediaId = mediaJpaRepository.save(
                Media.upload("thumb.png", "stored-thumb.png", "media/stored-thumb.png", "image/png", 100L))
                .getId();

        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", categoryId,
                WorkVisibility.VISIBLE);
        work.replaceMediaItems(List.of(new WorkMediaItem(mediaId, WorkMediaRole.THUMBNAIL, 0, "대표 이미지")));
        repository.saveAndFlush(work);

        assertThat(repository.existsByMediaItems_MediaId(mediaId)).isTrue();
        assertThat(repository.existsByMediaItems_MediaId(99999L)).isFalse();
    }
}
