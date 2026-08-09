package kr.douid.brand.work.presentation.response;

import kr.douid.brand.work.application.query.PublicWorkListItem;

/**
 * 공개 작업물 목록 조회 API 응답
 *
 * Admin 전용 필드(id, visibility, createdAt, updatedAt)는 포함하지 않는다.
 */
public record PublicWorkListResponse(
        String title,
        String slug,
        String summary,
        WorkCategoryResponse category,
        WorkMediaResponse thumbnail) {

    /**
     * application 조회 항목을 목록 응답으로 변환
     *
     * @param item         공개 작업물 목록 항목
     * @param mediaBaseUrl 미디어 API 기본 경로
     * @return 변환된 응답
     */
    public static PublicWorkListResponse from(PublicWorkListItem item, String mediaBaseUrl) {
        return new PublicWorkListResponse(
                item.title(),
                item.slug(),
                item.summary(),
                WorkCategoryResponse.fromPublic(item.category()),
                WorkMediaResponse.fromPublic(item.thumbnail(), mediaBaseUrl));
    }
}
