package kr.douid.brand.category.presentation.request;

import jakarta.validation.constraints.NotBlank;
import kr.douid.brand.category.application.command.CreateCategoryCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class CreateCategoryRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private int displayOrder;

    private boolean visible = true;

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
                visible
        );
    }
}
