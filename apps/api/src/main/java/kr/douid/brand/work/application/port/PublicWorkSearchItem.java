package kr.douid.brand.work.application.port;

/**
 * 공개 작업물 검색 결과 항목
 *
 * 텍스트는 제어 문자를 정리하고 길이 상한을 적용한 값이다. summary만 null일 수 있다.
 */
public record PublicWorkSearchItem(
        String slug,
        String title,
        String summary,
        String categoryName,
        String categorySlug) {
}
