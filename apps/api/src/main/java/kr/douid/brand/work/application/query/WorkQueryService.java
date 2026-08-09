package kr.douid.brand.work.application.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.work.domain.WorkNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * 작업물 조회 유스케이스를 처리하는 서비스
 *
 * 관리자용 조회와 공개용 조회를 담당하며, Public 조회는 Public Visibility Policy를 통과한 작업물만 반환한다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkQueryService {

    private final WorkQueryRepository workQueryRepository;

    /**
     * 관리자용 작업물 목록을 반환
     *
     * @param pageable 페이지네이션 파라미터
     * @return 관리자용 작업물 목록 페이지
     */
    public Page<AdminWorkListItem> getAdminWorkList(Pageable pageable) {
        return workQueryRepository.findAdminWorkList(pageable);
    }

    /**
     * 관리자용 작업물 상세를 반환
     *
     * @param id Work 식별자
     * @return 관리자용 작업물 상세
     * @throws WorkNotFoundException 작업물이 존재하지 않는 경우
     */
    public AdminWorkDetail getAdminWorkDetail(Long id) {
        return workQueryRepository.findAdminWorkDetail(id)
                .orElseThrow(WorkNotFoundException::new);
    }

    /**
     * 공개 작업물 목록을 반환
     *
     * @param pageable 페이지네이션 파라미터
     * @return 공개 작업물 목록 페이지
     */
    public Page<PublicWorkListItem> getPublicWorkList(Pageable pageable) {
        return workQueryRepository.findPublicWorkList(pageable);
    }

    /**
     * slug로 공개 작업물 상세를 반환
     *
     * 미존재, Work 비공개, Category 비공개 세 경우 모두 동일한 예외로 처리한다 (Not Found Policy)
     *
     * @param slug 작업물 슬러그
     * @return 공개 작업물 상세
     * @throws WorkNotFoundException 노출 가능한 작업물이 없는 경우
     */
    public PublicWorkDetail getPublicWorkDetail(String slug) {
        return workQueryRepository.findPublicWorkDetailBySlug(slug)
                .orElseThrow(WorkNotFoundException::new);
    }

    /**
     * 공개 카테고리 기준 작업물 목록을 반환
     *
     * @param categorySlug 카테고리 슬러그
     * @param pageable     페이지네이션 파라미터
     * @return 공개 작업물 목록 페이지 (카테고리 미존재/비공개면 빈 페이지)
     */
    public Page<PublicWorkListItem> getPublicWorkListByCategory(String categorySlug, Pageable pageable) {
        return workQueryRepository.findPublicWorkListByCategorySlug(categorySlug, pageable);
    }
}
