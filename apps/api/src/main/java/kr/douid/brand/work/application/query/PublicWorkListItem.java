package kr.douid.brand.work.application.query;

/**
 * 공개 작업물 목록 조회 항목
 *
 * Admin 전용 필드(id, visibility, createdAt, updatedAt)는 갖지 않는다.
 *
 * @param title     제목
 * @param slug      슬러그
 * @param summary   요약 설명 (nullable)
 * @param category  연결된 Category 정보 (항상 non-null, Public Visibility Policy상 카테고리 공개가 필수)
 * @param thumbnail 대표 이미지 (nullable)
 */
public record PublicWorkListItem(
        String title,
        String slug,
        String summary,
        WorkCategoryView category,
        WorkMediaView thumbnail) {
}
