package kr.douid.brand.work.presentation.response;

import java.time.LocalDateTime;
import java.util.List;

import kr.douid.brand.work.application.query.AdminWorkDetail;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 관리자 작업물 상세 조회 API 응답
 */
public record AdminWorkDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String description,
        WorkVisibility visibility,
        WorkCategoryResponse category,
        List<WorkMediaResponse> mediaItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /**
     * application 조회 결과를 상세 응답으로 변환
     *
     * @param detail       관리자 작업물 상세 조회 결과
     * @param mediaBaseUrl 미디어 API 기본 경로
     * @return 변환된 응답
     */
    public static AdminWorkDetailResponse from(AdminWorkDetail detail, String mediaBaseUrl) {
        return new AdminWorkDetailResponse(
                detail.id(),
                detail.title(),
                detail.slug(),
                detail.summary(),
                detail.description(),
                detail.visibility(),
                WorkCategoryResponse.fromAdmin(detail.category()),
                detail.mediaItems().stream()
                        .map(view -> WorkMediaResponse.fromAdmin(view, mediaBaseUrl))
                        .toList(),
                detail.createdAt(),
                detail.updatedAt());
    }
}
