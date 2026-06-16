package kr.douid.brand.category.presentation.dto;

import jakarta.validation.constraints.NotBlank;
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
}
