package kr.douid.brand.work.infrastructure.query;

/**
 * QueryDSL 조회용 Work + Category flat projection (공개 상세 조회 기본 정보)
 *
 * WorkMedia는 별도 조회로 채워지므로 포함하지 않는다.
 */
public record PublicWorkBasicProjection(
        Long workId,
        String title,
        String slug,
        String summary,
        String description,
        Long categoryId,
        String categoryName,
        String categorySlug,
        Boolean categoryVisible) {
}
