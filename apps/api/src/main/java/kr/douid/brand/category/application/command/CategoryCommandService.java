package kr.douid.brand.category.application.command;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryDeletionPolicy;
import kr.douid.brand.category.domain.CategoryRepository;
import kr.douid.brand.shared.exception.BusinessException;
import kr.douid.brand.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

/**
 * 카테고리 상태 변경 유스케이스를 처리하는 서비스
 *
 * 생성, 수정, 삭제 흐름과 삭제 정책 검증을 담당한다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final List<CategoryDeletionPolicy> deletionPolicies;

    /**
     * 새 카테고리를 생성
     *
     * @param command 카테고리 생성 입력값
     * @return 생성된 카테고리 결과
     * @throws BusinessException slug가 이미 존재하는 경우
     */
    public CategoryResult createCategory(CreateCategoryCommand command) {
        if (categoryRepository.existsBySlug(command.slug())) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
        try {
            Category category = Category.create(
                    command.name(),
                    command.slug(),
                    command.displayOrder(),
                    command.visible()
            );
            return CategoryResult.from(categoryRepository.save(category));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
    }

    /**
     * 기존 카테고리 정보를 수정
     *
     * @param command 카테고리 수정 입력값
     * @return 수정된 카테고리 결과
     * @throws BusinessException 카테고리를 찾을 수 없거나 slug가 이미 존재하는 경우
     */
    public CategoryResult updateCategory(UpdateCategoryCommand command) {
        Category category = categoryRepository.findById(command.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (categoryRepository.existsBySlugAndIdNot(command.slug(), command.id())) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
        try {
            category.update(command.name(), command.slug(), command.displayOrder(), command.visible());
            return CategoryResult.from(category);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATE);
        }
    }

    /**
     * 기존 카테고리를 삭제
     *
     * @param command 카테고리 삭제 입력값
     * @throws BusinessException 카테고리를 찾을 수 없는 경우
     */
    public void deleteCategory(DeleteCategoryCommand command) {
        Category category = categoryRepository.findById(command.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        deletionPolicies.forEach(policy -> policy.validate(category));
        categoryRepository.delete(category);
    }
}
