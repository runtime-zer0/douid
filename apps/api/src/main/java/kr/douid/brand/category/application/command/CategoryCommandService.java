package kr.douid.brand.category.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryHasWorksException;
import kr.douid.brand.category.domain.CategoryNotFoundException;
import kr.douid.brand.category.domain.CategoryRepository;
import kr.douid.brand.category.domain.CategorySlugDuplicateException;
import kr.douid.brand.work.application.port.WorkReferenceChecker;
import lombok.RequiredArgsConstructor;

/**
 * 카테고리 상태 변경 유스케이스를 처리하는 서비스
 *
 * 생성, 수정, 삭제 흐름과 Work 참조 무결성 확인을 담당한다
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final WorkReferenceChecker workReferenceChecker;

    /**
     * 새 카테고리를 생성
     *
     * @param command 카테고리 생성 입력값
     * @return 생성된 카테고리 결과
     * @throws CategorySlugDuplicateException slug가 이미 존재하는 경우
     */
    public CategoryResult createCategory(CreateCategoryCommand command) {
        if (categoryRepository.existsBySlug(command.slug())) {
            throw new CategorySlugDuplicateException();
        }
        Category category = Category.create(
                command.name(),
                command.slug(),
                command.displayOrder(),
                command.visible()
        );
        return CategoryResult.from(categoryRepository.save(category));
    }

    /**
     * 기존 카테고리 정보를 수정
     *
     * @param command 카테고리 수정 입력값
     * @return 수정된 카테고리 결과
     * @throws CategoryNotFoundException       카테고리를 찾을 수 없는 경우
     * @throws CategorySlugDuplicateException  slug가 이미 존재하는 경우
     */
    public CategoryResult updateCategory(UpdateCategoryCommand command) {
        Category category = categoryRepository.findById(command.id())
                .orElseThrow(CategoryNotFoundException::new);
        if (categoryRepository.existsBySlugAndIdNot(command.slug(), command.id())) {
            throw new CategorySlugDuplicateException();
        }
        category.update(command.name(), command.slug(), command.displayOrder(), command.visible());
        return CategoryResult.from(category);
    }

    /**
     * 기존 카테고리를 삭제
     *
     * @param command 카테고리 삭제 입력값
     * @throws CategoryNotFoundException 카테고리를 찾을 수 없는 경우
     * @throws CategoryHasWorksException 작업물이 연결된 경우
     */
    public void deleteCategory(DeleteCategoryCommand command) {
        Category category = categoryRepository.findById(command.id())
                .orElseThrow(CategoryNotFoundException::new);
        if (workReferenceChecker.existsByCategoryId(category.getId())) {
            throw new CategoryHasWorksException();
        }
        categoryRepository.delete(category);
    }
}
