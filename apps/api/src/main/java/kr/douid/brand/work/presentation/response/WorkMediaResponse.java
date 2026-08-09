package kr.douid.brand.work.presentation.response;

import kr.douid.brand.work.application.query.WorkMediaView;
import kr.douid.brand.work.domain.WorkMediaRole;

/**
 * Work 조회 응답에 포함되는 Media 정보
 *
 * Admin 응답은 {@code mediaId}, {@code sortOrder}를 포함하고, Public 응답은 포함하지 않는다
 * (정렬은 서버가 이미 적용해 배열 순서로 전달한다).
 */
public record WorkMediaResponse(
        Long mediaId,
        WorkMediaRole role,
        Integer sortOrder,
        String altText,
        String fileUrl) {

    /**
     * Admin 응답용 변환 (mediaId, sortOrder 포함)
     *
     * @param view    WorkMedia 조회 view (nullable)
     * @param baseUrl 미디어 API 기본 경로 (예: /api/media)
     * @return 변환된 응답 (view가 null이면 null)
     */
    public static WorkMediaResponse fromAdmin(WorkMediaView view, String baseUrl) {
        if (view == null) {
            return null;
        }
        return new WorkMediaResponse(
                view.mediaId(), view.role(), view.sortOrder(), view.altText(), toFileUrl(view, baseUrl));
    }

    /**
     * Public 응답용 변환 (mediaId, sortOrder 미포함)
     *
     * @param view    WorkMedia 조회 view (nullable)
     * @param baseUrl 미디어 API 기본 경로 (예: /api/media)
     * @return 변환된 응답 (view가 null이면 null)
     */
    public static WorkMediaResponse fromPublic(WorkMediaView view, String baseUrl) {
        if (view == null) {
            return null;
        }
        return new WorkMediaResponse(null, view.role(), null, view.altText(), toFileUrl(view, baseUrl));
    }

    private static String toFileUrl(WorkMediaView view, String baseUrl) {
        return baseUrl + "/" + view.mediaId() + "/file";
    }
}
