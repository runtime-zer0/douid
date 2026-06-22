package kr.douid.brand.category.infrastructure.policy;

import org.springframework.stereotype.Component;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryDeletionPolicy;

/**
 * 기본 카테고리 삭제 정책
 *
 * 다른 feature 연동 정책이 없는 기본 상태에서 추가 제약 없이 통과시킨다
 */
@Component
public class DefaultCategoryDeletionPolicy implements CategoryDeletionPolicy {

    /**
     * 주어진 카테고리가 삭제 가능한지 검증
     *
     * @param category 삭제 대상 카테고리
     */
    @Override
    public void validate(Category category) {
        // Work 단계에서 WorkLinkedCategoryDeletionPolicy로 교체
    }
}
