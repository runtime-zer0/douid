package kr.douid.brand.category.application;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryDeletionPolicy;
import kr.douid.brand.category.domain.CategoryRepository;
import kr.douid.brand.category.presentation.dto.CreateCategoryRequest;
import kr.douid.brand.category.presentation.dto.UpdateCategoryRequest;
import kr.douid.brand.shared.exception.BusinessException;
import kr.douid.brand.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final List<CategoryDeletionPolicy> deletionPolicies;

    public CategoryResult createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
        try {
            Category category = Category.create(
                    request.getName(),
                    request.getSlug(),
                    request.getDisplayOrder(),
                    request.isVisible()
            );
            return CategoryResult.from(categoryRepository.save(category));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
    }

    public CategoryResult updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
        try {
            category.update(request.name(), request.slug(), request.displayOrder(), request.visible());
            return CategoryResult.from(category);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        deletionPolicies.forEach(policy -> policy.validate(category));
        categoryRepository.delete(category);
    }
}
