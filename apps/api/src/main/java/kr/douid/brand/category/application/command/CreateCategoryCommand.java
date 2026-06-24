package kr.douid.brand.category.application.command;

public record CreateCategoryCommand(
        String name,
        String slug,
        int displayOrder,
        boolean visible
) {
}
