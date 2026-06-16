package kr.douid.brand.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryQueryService categoryQueryService;

    @Test
    void findAllForAdmin_공개비공개_모두_반환() {
        Category visible = Category.create("브랜딩", "branding", 1, true);
        Category hidden = Category.create("내부", "internal", 2, false);
        given(categoryRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc())
                .willReturn(List.of(visible, hidden));

        List<CategoryResult> result = categoryQueryService.findAllForAdmin();

        assertThat(result).hasSize(2);
    }

    @Test
    void findAllVisible_공개만_반환() {
        Category visible = Category.create("브랜딩", "branding", 1, true);
        given(categoryRepository.findAllByVisibleTrueOrderByDisplayOrderAscCreatedAtAsc())
                .willReturn(List.of(visible));

        List<CategoryResult> result = categoryQueryService.findAllVisible();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).visible()).isTrue();
    }

    @Test
    void findAllForAdmin_빈_목록() {
        given(categoryRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc())
                .willReturn(List.of());

        List<CategoryResult> result = categoryQueryService.findAllForAdmin();

        assertThat(result).isEmpty();
    }
}
