package kr.douid.brand.category.infrastructure;

import org.springframework.stereotype.Component;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryDeletionPolicy;

@Component
public class DefaultCategoryDeletionPolicy implements CategoryDeletionPolicy {

    @Override
    public void validate(Category category) {
        // Work 단계에서 WorkLinkedCategoryDeletionPolicy로 교체
    }
}
