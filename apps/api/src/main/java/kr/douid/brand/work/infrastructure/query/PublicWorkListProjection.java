package kr.douid.brand.work.infrastructure.query;

import kr.douid.brand.work.application.query.PublicWorkListItem;
import kr.douid.brand.work.application.query.WorkCategoryView;
import kr.douid.brand.work.application.query.WorkMediaView;
import kr.douid.brand.work.domain.WorkMediaRole;

/**
 * QueryDSL 조회용 Work + Category + THUMBNAIL flat projection (공개 목록 조회)
 *
 * {@link PublicWorkListItem} 변환 후 application 계층 반환
 */
public record PublicWorkListProjection(
        String title,
        String slug,
        String summary,
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
     * @return 공개 작업물 목록 항목
     */
    public PublicWorkListItem toItem() {
        WorkCategoryView category = new WorkCategoryProjection(categoryId, categoryName, categorySlug, categoryVisible)
                .toView();
        WorkMediaView thumbnail = new WorkMediaProjection(
                thumbnailMediaId, thumbnailRole, thumbnailSortOrder, thumbnailAltText,
                thumbnailFilePath, thumbnailOriginalFilename)
                .toView();

        return new PublicWorkListItem(title, slug, summary, category, thumbnail);
    }
}
