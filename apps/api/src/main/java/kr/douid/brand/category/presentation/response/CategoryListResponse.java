package kr.douid.brand.category.presentation.response;

import kr.douid.brand.category.application.query.CategoryListItem;

public record CategoryListResponse(
        Long id,
        String name,
        String slug,
        int displayOrder,
        boolean visible
) {
    /**
     * application 조회 항목을 목록 응답으로 변환
     *
     * @param item 카테고리 목록 항목
     * @return 카테고리 목록 응답
     */
    public static CategoryListResponse from(CategoryListItem item) {
        return new CategoryListResponse(
                item.id(),
                item.name(),
                item.slug(),
                item.displayOrder(),
                item.visible()
        );
    }
}
