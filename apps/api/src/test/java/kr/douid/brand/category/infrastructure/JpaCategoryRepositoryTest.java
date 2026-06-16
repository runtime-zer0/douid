package kr.douid.brand.category.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.douid.brand.category.domain.Category;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCategoryRepositoryTest {

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
    private JpaCategoryRepository repository;

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

    @Test
    void findAll_displayOrder_createdAt_정렬() {
        repository.save(Category.create("C", "c", 2, true));
        repository.save(Category.create("A", "a", 1, true));
        repository.save(Category.create("B", "b", 2, true));

        List<Category> result = repository.findAllByOrderByDisplayOrderAscCreatedAtAsc();

        assertThat(result).extracting(Category::getSlug)
                .containsExactly("a", "c", "b");
    }

    @Test
    void findAllVisible_공개만_반환() {
        repository.save(Category.create("공개", "public", 1, true));
        repository.save(Category.create("비공개", "hidden", 2, false));

        List<Category> result = repository.findAllByVisibleTrueOrderByDisplayOrderAscCreatedAtAsc();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSlug()).isEqualTo("public");
    }
}
