package kr.douid.brand.category.application.query;

import java.util.List;

/**
 * 카테고리 조회 전용 포트
 *
 * 관리자 화면과 공개 화면에 필요한 조회 모델을 반환한다
 */
public interface CategoryQueryRepository {

    /**
     * 관리자용 카테고리 목록을 조회
     *
     * @return 관리자용 카테고리 목록
     */
    List<CategoryListItem> findAdminCategoryList();

    /**
     * 공개용 카테고리 목록을 조회
     *
     * @return 공개용 카테고리 목록
     */
    List<CategoryListItem> findPublicCategoryList();
}
