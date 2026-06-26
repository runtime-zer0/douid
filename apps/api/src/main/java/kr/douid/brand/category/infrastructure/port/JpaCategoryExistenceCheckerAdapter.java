package kr.douid.brand.category.infrastructure.port;

import org.springframework.stereotype.Component;

import kr.douid.brand.category.application.port.CategoryExistenceChecker;
import kr.douid.brand.category.infrastructure.persistence.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link CategoryExistenceChecker} port의 JPA 구현체
 */
@Component
@RequiredArgsConstructor
public class JpaCategoryExistenceCheckerAdapter implements CategoryExistenceChecker {

    private final CategoryJpaRepository categoryJpaRepository;

    /**
     * 카테고리 존재 여부 확인
     *
     * @param categoryId 확인할 카테고리 ID
     * @return 존재하면 true
     */
    @Override
    public boolean existsById(Long categoryId) {
        return categoryJpaRepository.existsById(categoryId);
    }
}
