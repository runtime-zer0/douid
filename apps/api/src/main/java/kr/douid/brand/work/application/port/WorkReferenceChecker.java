package kr.douid.brand.work.application.port;

/**
 * 외부 aggregate가 Work 참조 여부를 조회하기 위한 application port
 *
 * category, media application flow가 Work 내부 persistence 구현에 직접 의존하지 않도록 경계를 분리한다.
 */
public interface WorkReferenceChecker {

    /**
     * 해당 카테고리를 참조하는 작업물 존재 여부를 확인
     *
     * @param categoryId 확인할 카테고리 ID
     * @return 참조 작업물 존재 여부
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * 해당 미디어를 참조하는 작업물 존재 여부를 확인
     *
     * @param mediaId 확인할 미디어 ID
     * @return 참조 작업물 존재 여부
     */
    boolean existsByMediaId(Long mediaId);
}
