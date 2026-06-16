package kr.douid.brand.category.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void create_정상_생성() {
        Category category = Category.create("브랜딩", "branding", 1, true);

        assertThat(category.getName()).isEqualTo("브랜딩");
        assertThat(category.getSlug()).isEqualTo("branding");
        assertThat(category.getDisplayOrder()).isEqualTo(1);
        assertThat(category.isVisible()).isTrue();
    }

    @Test
    void update_필드_변경() {
        Category category = Category.create("브랜딩", "branding", 1, true);

        category.update("UX", "ux", 2, false);

        assertThat(category.getName()).isEqualTo("UX");
        assertThat(category.getSlug()).isEqualTo("ux");
        assertThat(category.getDisplayOrder()).isEqualTo(2);
        assertThat(category.isVisible()).isFalse();
    }
}
