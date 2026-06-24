package kr.douid.brand.category.application.command;

public record DeleteCategoryCommand(
        Long id
) {
    /**
     * 카테고리 삭제 command를 생성
     *
     * @param id 삭제할 카테고리 ID
     * @return 카테고리 삭제 command
     */
    public static DeleteCategoryCommand of(Long id) {
        return new DeleteCategoryCommand(id);
    }
}
