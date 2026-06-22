package kr.douid.brand.category.application.command;

public record UpdateCategoryCommand(
   Long id,
        String name,
        String slug,
        int displayOrder,
        boolean visible
) {
   
}
