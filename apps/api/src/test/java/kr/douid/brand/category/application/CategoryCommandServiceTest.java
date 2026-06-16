package kr.douid.brand.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryRepository;
import kr.douid.brand.category.presentation.dto.CreateCategoryRequest;
import kr.douid.brand.category.presentation.dto.UpdateCategoryRequest;
import kr.douid.brand.shared.exception.BusinessException;
import kr.douid.brand.shared.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryCommandService categoryCommandService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        categoryCommandService = new CategoryCommandService(categoryRepository, List.of());
    }

    @Test
    void createCategory_정상_생성() {
        CreateCategoryRequest request = createRequest("브랜딩", "branding", 1, true);
        Category saved = Category.create("브랜딩", "branding", 1, true);
        given(categoryRepository.existsBySlug("branding")).willReturn(false);
        given(categoryRepository.save(any())).willReturn(saved);

        CategoryResult result = categoryCommandService.createCategory(request);

        assertThat(result.name()).isEqualTo("브랜딩");
        assertThat(result.slug()).isEqualTo("branding");
    }

    @Test
    void createCategory_슬러그_중복_예외() {
        CreateCategoryRequest request = createRequest("브랜딩", "branding", 1, true);
        given(categoryRepository.existsBySlug("branding")).willReturn(true);

        assertThatThrownBy(() -> categoryCommandService.createCategory(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_SLUG_DUPLICATE);
    }

    @Test
    void updateCategory_정상_수정() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        UpdateCategoryRequest request = updateRequest("UX", "ux", 2, false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsBySlugAndIdNot("ux", 1L)).willReturn(false);

        CategoryResult result = categoryCommandService.updateCategory(1L, request);

        assertThat(result.name()).isEqualTo("UX");
        assertThat(result.slug()).isEqualTo("ux");
    }

    @Test
    void updateCategory_자기슬러그_허용() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        UpdateCategoryRequest request = updateRequest("브랜딩 v2", "branding", 1, true);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsBySlugAndIdNot("branding", 1L)).willReturn(false);

        CategoryResult result = categoryCommandService.updateCategory(1L, request);

        assertThat(result.slug()).isEqualTo("branding");
    }

    @Test
    void updateCategory_타슬러그_중복_예외() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        UpdateCategoryRequest request = updateRequest("UX", "ux", 2, false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsBySlugAndIdNot("ux", 1L)).willReturn(true);

        assertThatThrownBy(() -> categoryCommandService.updateCategory(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_SLUG_DUPLICATE);
    }

    @Test
    void updateCategory_미존재_예외() {
        UpdateCategoryRequest request = updateRequest("UX", "ux", 2, false);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryCommandService.updateCategory(99L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void deleteCategory_정상_삭제() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        categoryCommandService.deleteCategory(1L);

        then(categoryRepository).should().delete(category);
    }

    @Test
    void deleteCategory_미존재_예외() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryCommandService.deleteCategory(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    private CreateCategoryRequest createRequest(String name, String slug, int displayOrder, boolean visible) {
        try {
            CreateCategoryRequest req = new CreateCategoryRequest();
            setField(req, "name", name);
            setField(req, "slug", slug);
            setField(req, "displayOrder", displayOrder);
            setField(req, "visible", visible);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UpdateCategoryRequest updateRequest(String name, String slug, int displayOrder, boolean visible) {
        return new UpdateCategoryRequest(name, slug, displayOrder, visible);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
