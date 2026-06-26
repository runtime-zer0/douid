package kr.douid.brand.work.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkMediaItem;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkVisibility;
import kr.douid.brand.work.infrastructure.persistence.WorkJpaRepository;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class JpaWorkRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private WorkJpaRepository repository;

    @Test
    void save_저장_후_조회() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE);

        Work saved = repository.save(work);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSlug()).isEqualTo("brand-renewal");
        assertThat(saved.getVisibility()).isEqualTo(WorkVisibility.VISIBLE);
    }

    @Test
    void slug_유니크_제약_위반() {
        repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE));

        assertThatThrownBy(() -> {
            repository.save(Work.create("다른 작업물", "brand-renewal", "요약", "상세", 2L,
                    WorkVisibility.HIDDEN));
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsBySlug_존재() {
        repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE));

        assertThat(repository.existsBySlug("brand-renewal")).isTrue();
        assertThat(repository.existsBySlug("missing")).isFalse();
    }

    @Test
    void existsBySlugAndIdNot_자기자신_제외() {
        Work saved = repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE));

        assertThat(repository.existsBySlugAndIdNot("brand-renewal", saved.getId())).isFalse();
    }

    @Test
    void existsByCategoryId_카테고리_참조_확인() {
        repository.save(Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE));

        assertThat(repository.existsByCategoryId(1L)).isTrue();
        assertThat(repository.existsByCategoryId(99L)).isFalse();
    }

    @Test
    void existsByMediaItems_MediaId_미디어_참조_확인() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE);
        work.replaceMediaItems(List.of(new WorkMediaItem(10L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지")));
        repository.saveAndFlush(work);

        assertThat(repository.existsByMediaItems_MediaId(10L)).isTrue();
        assertThat(repository.existsByMediaItems_MediaId(99L)).isFalse();
    }
}
