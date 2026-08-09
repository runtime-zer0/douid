package kr.douid.brand.work.presentation.response;

import java.time.LocalDateTime;

import kr.douid.brand.work.application.query.AdminWorkListItem;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 관리자 작업물 목록 조회 API 응답
 */
public record AdminWorkListResponse(
        Long id,
        String title,
        String slug,
        WorkVisibility visibility,
        WorkCategoryResponse category,
        WorkMediaResponse thumbnail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /**
     * application 조회 항목을 목록 응답으로 변환
     *
     * @param item    관리자 작업물 목록 항목
     * @param mediaBaseUrl 미디어 API 기본 경로
     * @return 변환된 응답
     */
    public static AdminWorkListResponse from(AdminWorkListItem item, String mediaBaseUrl) {
        return new AdminWorkListResponse(
                item.id(),
                item.title(),
                item.slug(),
                item.visibility(),
                WorkCategoryResponse.fromAdmin(item.category()),
                WorkMediaResponse.fromAdmin(item.thumbnail(), mediaBaseUrl),
                item.createdAt(),
                item.updatedAt());
    }
}
