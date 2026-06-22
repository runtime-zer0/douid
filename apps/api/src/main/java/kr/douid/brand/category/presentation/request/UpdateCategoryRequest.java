package kr.douid.brand.category.presentation.request;

import jakarta.validation.constraints.NotBlank;
import kr.douid.brand.category.application.command.UpdateCategoryCommand;

public record UpdateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        int displayOrder,
        boolean visible
) {
    /**
     * 요청값을 카테고리 수정 command로 변환
     *
     * @param id 수정할 카테고리 ID
     * @return 카테고리 수정 command
     */
    public UpdateCategoryCommand toCommand(Long id) {
        return new UpdateCategoryCommand(
                id,
                name,
                slug,
                displayOrder,
                visible
        );
    }
}
