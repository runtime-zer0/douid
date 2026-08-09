package kr.douid.brand.work.infrastructure.query;

import java.time.LocalDateTime;

import kr.douid.brand.work.application.query.AdminWorkListItem;
import kr.douid.brand.work.application.query.WorkCategoryView;
import kr.douid.brand.work.application.query.WorkMediaView;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * QueryDSL 조회용 Work + Category + THUMBNAIL flat projection
 *
 * {@link AdminWorkListItem} 변환 후 application 계층 반환
 */
public record AdminWorkListProjection(
        Long workId,
        String title,
        String slug,
        WorkVisibility visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long categoryId,
        String categoryName,
        String categorySlug,
        Boolean categoryVisible,
        Long thumbnailMediaId,
        WorkMediaRole thumbnailRole,
        Integer thumbnailSortOrder,
        String thumbnailAltText,
        String thumbnailFilePath,
        String thumbnailOriginalFilename) {

    /**
     * projection을 application 조회 항목으로 변환
     *
     * @return 관리자 작업물 목록 항목
     */
    public AdminWorkListItem toItem() {
        WorkCategoryView category = new WorkCategoryProjection(categoryId, categoryName, categorySlug, categoryVisible)
                .toView();
        WorkMediaView thumbnail = new WorkMediaProjection(
                thumbnailMediaId, thumbnailRole, thumbnailSortOrder, thumbnailAltText,
                thumbnailFilePath, thumbnailOriginalFilename)
                .toView();

        return new AdminWorkListItem(
                workId,
                title,
                slug,
                visibility,
                category,
                thumbnail,
                createdAt,
                updatedAt);
    }
}
