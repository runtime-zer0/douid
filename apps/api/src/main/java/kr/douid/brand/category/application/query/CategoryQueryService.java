package kr.douid.brand.category.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리 조회 유스케이스를 처리하는 서비스
 *
 * 관리자용 목록과 공개용 목록 조회를 담당한다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService {

    private final CategoryQueryRepository categoryQueryRepository;

    /**
     * 관리자용 카테고리 목록을 반환
     *
     * @return 관리자용 카테고리 목록
     */
    public List<CategoryListItem> getAdminCategoryList() {
        return categoryQueryRepository.findAdminCategoryList();
    }

    /**
     * 공개용 카테고리 목록을 반환
     *
     * @return 공개용 카테고리 목록
     */
    public List<CategoryListItem> getPublicCategoryList() {
        return categoryQueryRepository.findPublicCategoryList();
    }
}
