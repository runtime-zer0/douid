package kr.douid.brand.category.presentation.response;

import java.time.LocalDateTime;

import kr.douid.brand.category.application.command.CategoryResult;

public record CategoryCommandResponse(
        Long id,
        String name,
        String slug,
        int displayOrder,
        boolean visible,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * application 결과를 명령 응답으로 변환
     *
     * @param result 카테고리 명령 결과
     * @return 카테고리 명령 응답
     */
    public static CategoryCommandResponse from(CategoryResult result) {
        return new CategoryCommandResponse(
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
