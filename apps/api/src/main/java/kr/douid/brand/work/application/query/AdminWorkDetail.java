package kr.douid.brand.work.application.query;

import java.time.LocalDateTime;
import java.util.List;

import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 관리자 작업물 상세 조회 결과
 *
 * @param id          Work 식별자
 * @param title       제목
 * @param slug        슬러그
 * @param summary     요약 설명 (nullable)
 * @param description 상세 설명 (nullable)
 * @param visibility  공개 상태
 * @param category    연결된 Category 정보 (nullable)
 * @param mediaItems  sortOrder 오름차순으로 정렬된 Media 목록
 * @param createdAt   생성 시각
 * @param updatedAt   수정 시각
 */
public record AdminWorkDetail(
        Long id,
        String title,
        String slug,
        String summary,
        String description,
        WorkVisibility visibility,
        WorkCategoryView category,
        List<WorkMediaView> mediaItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
