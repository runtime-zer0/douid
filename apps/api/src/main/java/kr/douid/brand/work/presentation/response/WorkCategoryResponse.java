package kr.douid.brand.work.presentation.response;

import kr.douid.brand.work.application.query.WorkCategoryView;

/**
 * Work 조회 응답에 포함되는 Category 정보
 *
 * Admin 응답은 {@code id}를 포함하고, Public 응답은 포함하지 않는다.
 */
public record WorkCategoryResponse(
        Long id,
        String name,
        String slug) {

    /**
     * Admin 응답용 변환 (id 포함)
     *
     * @param view Category 조회 view (nullable)
     * @return 변환된 응답 (view가 null이면 null)
     */
    public static WorkCategoryResponse fromAdmin(WorkCategoryView view) {
        if (view == null) {
            return null;
        }
        return new WorkCategoryResponse(view.id(), view.name(), view.slug());
    }

    /**
     * Public 응답용 변환 (id 미포함)
     *
     * @param view Category 조회 view (nullable)
     * @return 변환된 응답 (view가 null이면 null)
     */
    public static WorkCategoryResponse fromPublic(WorkCategoryView view) {
        if (view == null) {
            return null;
        }
        return new WorkCategoryResponse(null, view.name(), view.slug());
    }
}
