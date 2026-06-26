package kr.douid.brand.category.application.port;

/**
 * 카테고리 존재 여부 확인 port
 *
 * 다른 feature는 Category 저장 구현을 알지 않고 이 contract로 존재 여부만 확인한다.
 */
public interface CategoryExistenceChecker {

    /**
     * 카테고리 존재 여부 확인
     *
     * @param categoryId 확인할 카테고리 ID
     * @return 존재하면 true
     */
    boolean existsById(Long categoryId);
}
