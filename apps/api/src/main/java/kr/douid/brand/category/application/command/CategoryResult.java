package kr.douid.brand.category.application.command;

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
    /**
     * 도메인 카테고리를 명령 결과로 변환
     *
     * @param category 변환할 카테고리
     * @return 카테고리 명령 결과
     */
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
