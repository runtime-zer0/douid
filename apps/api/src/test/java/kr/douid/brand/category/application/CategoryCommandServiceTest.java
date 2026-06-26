package kr.douid.brand.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.category.application.command.CategoryCommandService;
import kr.douid.brand.category.application.command.CategoryResult;
import kr.douid.brand.category.application.command.CreateCategoryCommand;
import kr.douid.brand.category.application.command.DeleteCategoryCommand;
import kr.douid.brand.category.application.command.UpdateCategoryCommand;
import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryHasWorksException;
import kr.douid.brand.category.domain.CategoryNotFoundException;
import kr.douid.brand.category.domain.CategoryRepository;
import kr.douid.brand.category.domain.CategorySlugDuplicateException;
import kr.douid.brand.work.application.port.WorkReferenceChecker;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private WorkReferenceChecker workReferenceChecker;

    private CategoryCommandService categoryCommandService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        categoryCommandService = new CategoryCommandService(
                categoryRepository,
                workReferenceChecker
        );
    }

    @Test
    void createCategory_정상_생성() {
        CreateCategoryCommand command = new CreateCategoryCommand("브랜딩", "branding", 1, true);
        Category saved = Category.create("브랜딩", "branding", 1, true);
        given(categoryRepository.existsBySlug("branding")).willReturn(false);
        given(categoryRepository.save(any())).willReturn(saved);

        CategoryResult result = categoryCommandService.createCategory(command);

        assertThat(result.name()).isEqualTo("브랜딩");
        assertThat(result.slug()).isEqualTo("branding");
    }

    @Test
    void createCategory_슬러그_중복_예외() {
        CreateCategoryCommand command = new CreateCategoryCommand("브랜딩", "branding", 1, true);
        given(categoryRepository.existsBySlug("branding")).willReturn(true);

        assertThatThrownBy(() -> categoryCommandService.createCategory(command))
                .isInstanceOf(CategorySlugDuplicateException.class);
    }

    @Test
    void updateCategory_정상_수정() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        UpdateCategoryCommand command = new UpdateCategoryCommand(1L, "UX", "ux", 2, false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsBySlugAndIdNot("ux", 1L)).willReturn(false);

        CategoryResult result = categoryCommandService.updateCategory(command);

        assertThat(result.name()).isEqualTo("UX");
        assertThat(result.slug()).isEqualTo("ux");
    }

    @Test
    void updateCategory_자기슬러그_허용() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        UpdateCategoryCommand command = new UpdateCategoryCommand(1L, "브랜딩 v2", "branding", 1, true);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsBySlugAndIdNot("branding", 1L)).willReturn(false);

        CategoryResult result = categoryCommandService.updateCategory(command);

        assertThat(result.slug()).isEqualTo("branding");
    }

    @Test
    void updateCategory_타슬러그_중복_예외() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        UpdateCategoryCommand command = new UpdateCategoryCommand(1L, "UX", "ux", 2, false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsBySlugAndIdNot("ux", 1L)).willReturn(true);

        assertThatThrownBy(() -> categoryCommandService.updateCategory(command))
                .isInstanceOf(CategorySlugDuplicateException.class);
    }

    @Test
    void updateCategory_미존재_예외() {
        UpdateCategoryCommand command = new UpdateCategoryCommand(99L, "UX", "ux", 2, false);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryCommandService.updateCategory(command))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deleteCategory_정상_삭제() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(workReferenceChecker.existsByCategoryId(category.getId())).willReturn(false);

        categoryCommandService.deleteCategory(DeleteCategoryCommand.of(1L));

        then(categoryRepository).should().delete(category);
    }

    @Test
    void deleteCategory_작업물_참조중_예외() {
        Category category = Category.create("브랜딩", "branding", 1, true);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(workReferenceChecker.existsByCategoryId(category.getId())).willReturn(true);

        assertThatThrownBy(() -> categoryCommandService.deleteCategory(DeleteCategoryCommand.of(1L)))
                .isInstanceOf(CategoryHasWorksException.class);

        then(categoryRepository).should(never()).delete(any());
    }

    @Test
    void deleteCategory_미존재_예외() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryCommandService.deleteCategory(DeleteCategoryCommand.of(99L)))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
