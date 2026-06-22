package kr.douid.brand.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.category.application.query.CategoryListItem;
import kr.douid.brand.category.application.query.CategoryQueryRepository;
import kr.douid.brand.category.application.query.CategoryQueryService;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    @Mock
    private CategoryQueryRepository categoryQueryRepository;

    @InjectMocks
    private CategoryQueryService categoryQueryService;

    @Test
    void getAdminCategoryList_공개비공개_모두_반환() {
        given(categoryQueryRepository.findAdminCategoryList()).willReturn(List.of(
                new CategoryListItem(1L, "브랜딩", "branding", 1, true),
                new CategoryListItem(2L, "내부", "internal", 2, false)
        ));

        List<CategoryListItem> result = categoryQueryService.getAdminCategoryList();

        assertThat(result).hasSize(2);
    }

    @Test
    void getPublicCategoryList_공개만_반환() {
        given(categoryQueryRepository.findPublicCategoryList()).willReturn(List.of(
                new CategoryListItem(1L, "브랜딩", "branding", 1, true)
        ));

        List<CategoryListItem> result = categoryQueryService.getPublicCategoryList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).visible()).isTrue();
    }

    @Test
    void getAdminCategoryList_빈_목록() {
        given(categoryQueryRepository.findAdminCategoryList()).willReturn(List.of());

        List<CategoryListItem> result = categoryQueryService.getAdminCategoryList();

        assertThat(result).isEmpty();
    }
}
