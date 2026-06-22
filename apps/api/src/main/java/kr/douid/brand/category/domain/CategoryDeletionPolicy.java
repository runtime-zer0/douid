package kr.douid.brand.category.domain;

/**
 * 카테고리 삭제 정책
 *
 * 카테고리 삭제 전에 도메인 제약을 검증한다
 */
public interface CategoryDeletionPolicy {

    /**
     * 주어진 카테고리가 삭제 가능한지 검증
     *
     * @param category 삭제 대상 카테고리
     */
    void validate(Category category);
}
