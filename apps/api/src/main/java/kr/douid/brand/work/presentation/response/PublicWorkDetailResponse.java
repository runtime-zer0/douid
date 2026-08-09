package kr.douid.brand.work.presentation.response;

import java.util.List;

import kr.douid.brand.work.application.query.PublicWorkDetail;

/**
 * 공개 작업물 상세 조회 API 응답
 *
 * Admin 전용 필드는 포함하지 않는다.
 */
public record PublicWorkDetailResponse(
        String title,
        String slug,
        String summary,
        String description,
        WorkCategoryResponse category,
        List<WorkMediaResponse> mediaItems) {

    /**
     * application 조회 결과를 상세 응답으로 변환
     *
     * @param detail       공개 작업물 상세 조회 결과
     * @param mediaBaseUrl 미디어 API 기본 경로
     * @return 변환된 응답
     */
    public static PublicWorkDetailResponse from(PublicWorkDetail detail, String mediaBaseUrl) {
        return new PublicWorkDetailResponse(
                detail.title(),
                detail.slug(),
                detail.summary(),
                detail.description(),
                WorkCategoryResponse.fromPublic(detail.category()),
                detail.mediaItems().stream()
                        .map(view -> WorkMediaResponse.fromPublic(view, mediaBaseUrl))
                        .toList());
    }
}
