package kr.douid.brand.category.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        int displayOrder,
        boolean visible
) {}
