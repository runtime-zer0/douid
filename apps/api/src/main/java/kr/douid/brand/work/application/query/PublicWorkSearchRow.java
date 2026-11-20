package kr.douid.brand.work.application.query;

/**
 * 검색 query repository가 돌려주는 정리 전 결과 행
 */
public record PublicWorkSearchRow(
        String slug,
        String title,
        String summary,
        String categoryName,
        String categorySlug) {
}
