package kr.douid.brand.category.application;

import java.time.LocalDateTime;

import kr.douid.brand.category.domain.Category;

public record CategoryResult(
        Long id,
        String name,
        String slug,
        int displayOrder,
        boolean visible,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResult from(Category category) {
        return new CategoryResult(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDisplayOrder(),
                category.isVisible(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
