package kr.douid.brand.work.application.query;

/**
 * Work 조회 결과에 조합되는 Category 정보
 *
 * @param id      Category 식별자
 * @param name    Category 이름
 * @param slug    Category slug
 * @param visible Category 공개 여부 (Admin 응답에서만 사용)
 */
public record WorkCategoryView(
        Long id,
        String name,
        String slug,
        boolean visible) {
}
