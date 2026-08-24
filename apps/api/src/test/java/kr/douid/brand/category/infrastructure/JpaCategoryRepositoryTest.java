package kr.douid.brand.category.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.infrastructure.persistence.CategoryJpaRepository;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCategoryRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private CategoryJpaRepository repository;

    @Test
    void save_저장_후_조회() {
        Category category = Category.create("브랜딩", "branding", 1, true);

        Category saved = repository.save(category);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSlug()).isEqualTo("branding");
    }

    @Test
    void slug_유니크_제약_위반() {
        repository.save(Category.create("브랜딩", "branding", 1, true));

        assertThatThrownBy(() -> {
            repository.save(Category.create("브랜딩2", "branding", 2, true));
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsBySlug_존재() {
        repository.save(Category.create("브랜딩", "branding", 1, true));

        assertThat(repository.existsBySlug("branding")).isTrue();
        assertThat(repository.existsBySlug("ux")).isFalse();
    }

    @Test
    void existsBySlugAndIdNot_자기자신_제외() {
        Category saved = repository.save(Category.create("브랜딩", "branding", 1, true));

        assertThat(repository.existsBySlugAndIdNot("branding", saved.getId())).isFalse();
    }
}
