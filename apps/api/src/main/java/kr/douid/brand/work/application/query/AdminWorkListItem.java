package kr.douid.brand.work.application.query;

import java.time.LocalDateTime;

import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 관리자 작업물 목록 조회 항목
 *
 * @param id          Work 식별자
 * @param title       제목
 * @param slug        슬러그
 * @param visibility  공개 상태
 * @param category    연결된 Category 정보 (nullable)
 * @param thumbnail   대표 이미지 (nullable)
 * @param createdAt   생성 시각
 * @param updatedAt   수정 시각
 */
public record AdminWorkListItem(
        Long id,
        String title,
        String slug,
        WorkVisibility visibility,
        WorkCategoryView category,
        WorkMediaView thumbnail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
