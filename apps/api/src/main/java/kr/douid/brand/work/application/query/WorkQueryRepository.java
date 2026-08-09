package kr.douid.brand.work.application.query;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 작업물 조회 전용 포트
 *
 * 관리자 화면과 공개 화면에 필요한 조회 모델을 반환한다
 */
public interface WorkQueryRepository {

    /**
     * 관리자용 작업물 목록을 페이지네이션 조회
     *
     * @param pageable 페이지네이션 파라미터
     * @return 관리자용 작업물 목록 페이지
     */
    Page<AdminWorkListItem> findAdminWorkList(Pageable pageable);

    /**
     * 관리자용 작업물 상세를 조회
     *
     * @param id Work 식별자
     * @return 관리자용 작업물 상세 (없으면 empty)
     */
    Optional<AdminWorkDetail> findAdminWorkDetail(Long id);

    /**
     * 공개 작업물 목록을 페이지네이션 조회
     *
     * @param pageable 페이지네이션 파라미터
     * @return 공개 작업물 목록 페이지
     */
    Page<PublicWorkListItem> findPublicWorkList(Pageable pageable);

    /**
     * slug로 공개 작업물 상세를 조회
     *
     * @param slug 작업물 슬러그
     * @return 공개 작업물 상세 (미존재/Work 비공개/Category 비공개 모두 empty)
     */
    Optional<PublicWorkDetail> findPublicWorkDetailBySlug(String slug);

    /**
     * 공개 카테고리 slug 기준으로 공개 작업물 목록을 페이지네이션 조회
     *
     * @param categorySlug 카테고리 슬러그
     * @param pageable     페이지네이션 파라미터
     * @return 공개 작업물 목록 페이지 (카테고리 미존재/비공개면 빈 페이지)
     */
    Page<PublicWorkListItem> findPublicWorkListByCategorySlug(String categorySlug, Pageable pageable);
}
