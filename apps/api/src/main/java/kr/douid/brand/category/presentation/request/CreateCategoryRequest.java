package kr.douid.brand.category.presentation.request;

import jakarta.validation.constraints.NotBlank;
import kr.douid.brand.category.application.command.CreateCategoryCommand;

public record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        int displayOrder,
        Boolean visible
) {
    /**
     * 요청값을 카테고리 생성 command로 변환
     *
     * @return 카테고리 생성 command
     */
    public CreateCategoryCommand toCommand() {
        return new CreateCategoryCommand(
                name,
                slug,
                displayOrder,
                visible == null || visible
        );
    }
}
