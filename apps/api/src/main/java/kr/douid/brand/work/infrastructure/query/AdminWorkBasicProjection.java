package kr.douid.brand.work.infrastructure.query;

import java.time.LocalDateTime;

import kr.douid.brand.work.domain.WorkVisibility;

/**
 * QueryDSL 조회용 Work + Category flat projection (상세 조회 기본 정보)
 *
 * WorkMedia는 별도 조회로 채워지므로 포함하지 않는다.
 */
public record AdminWorkBasicProjection(
        Long workId,
        String title,
        String slug,
        String summary,
        String description,
        WorkVisibility visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long categoryId,
        String categoryName,
        String categorySlug,
        Boolean categoryVisible) {
}
