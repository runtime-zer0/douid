package kr.douid.brand.category.presentation.dto;

import java.time.LocalDateTime;

import kr.douid.brand.category.application.CategoryResult;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        int displayOrder,
        boolean visible,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(CategoryResult result) {
        return new CategoryResponse(
                result.id(),
                result.name(),
                result.slug(),
                result.displayOrder(),
                result.visible(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
