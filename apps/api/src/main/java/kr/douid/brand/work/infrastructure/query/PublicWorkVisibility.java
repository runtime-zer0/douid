package kr.douid.brand.work.infrastructure.query;

import static kr.douid.brand.category.domain.QCategory.category;
import static kr.douid.brand.work.domain.QWork.work;

import com.querydsl.core.types.dsl.BooleanExpression;

import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 공개 조회와 검색이 함께 쓰는 Public Visibility Policy의 유일한 정의
 *
 * {@code category.id = work.categoryId} inner join과 함께 사용해야 한다.
 * inner join이 카테고리 없는 Work를 걸러내고, 이 조건이 Work·Category 공개 여부를 확인한다.
 */
final class PublicWorkVisibility {

    private PublicWorkVisibility() {
    }

    static BooleanExpression condition() {
        return work.visibility.eq(WorkVisibility.VISIBLE).and(category.visible.isTrue());
    }
}
