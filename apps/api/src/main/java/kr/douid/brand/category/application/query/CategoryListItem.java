package kr.douid.brand.category.application.query;

public record CategoryListItem(
        Long id,
        String name,
        String slug,
        int displayOrder,
        boolean visible
) {
}
