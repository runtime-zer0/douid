package kr.douid.brand.work.application.query;

import java.util.List;

/**
 * 공개 작업물 상세 조회 결과
 *
 * Admin 전용 필드는 갖지 않는다.
 *
 * @param title       제목
 * @param slug        슬러그
 * @param summary     요약 설명 (nullable)
 * @param description 상세 설명 (nullable)
 * @param category    연결된 Category 정보 (항상 non-null)
 * @param mediaItems  sortOrder 오름차순으로 정렬된 Media 목록
 */
public record PublicWorkDetail(
        String title,
        String slug,
        String summary,
        String description,
        WorkCategoryView category,
        List<WorkMediaView> mediaItems) {
}
