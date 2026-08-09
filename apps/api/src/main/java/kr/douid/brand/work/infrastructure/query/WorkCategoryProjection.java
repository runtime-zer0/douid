package kr.douid.brand.work.infrastructure.query;

import kr.douid.brand.work.application.query.WorkCategoryView;

/**
 * QueryDSL 조회용 Category flat projection
 *
 * {@link WorkCategoryView} 변환 후 application 계층 반환
 */
public record WorkCategoryProjection(
        Long id,
        String name,
        String slug,
        Boolean visible) {

    /**
     * projection을 application query view로 변환
     *
     * @return 모든 필드가 null이면 null, 아니면 변환된 {@link WorkCategoryView}
     */
    public WorkCategoryView toView() {
        if (id == null) {
            return null;
        }
        return new WorkCategoryView(id, name, slug, Boolean.TRUE.equals(visible));
    }
}
