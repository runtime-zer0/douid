package kr.douid.brand.category.infrastructure.query;

import kr.douid.brand.category.application.query.CategoryListItem;

public record CategoryListProjection(
        Long id,
        String name,
        String slug,
        Integer displayOrder,
        Boolean visible
) {
    /**
     * 조회 projection을 application 조회 항목으로 변환
     *
     * @return 카테고리 목록 항목
     */
    CategoryListItem toItem() {
        return new CategoryListItem(
                id,
                name,
                slug,
                displayOrder == null ? 0 : displayOrder,
                Boolean.TRUE.equals(visible)
        );
    }
}
