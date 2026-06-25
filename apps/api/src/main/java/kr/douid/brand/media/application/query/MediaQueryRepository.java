package kr.douid.brand.media.application.query;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 미디어 조회 전용 포트
 *
 * QueryDSL DTO projection 기반 read model 반환
 */
public interface MediaQueryRepository {

    /**
     * 식별자로 미디어 단건 조회
     *
     * @param id 미디어 식별자
     * @return 미디어 view (없으면 empty)
     */
    Optional<MediaView> findById(Long id);

    /**
     * 미디어 목록을 페이지네이션으로 조회
     *
     * @param pageable 페이지네이션 파라미터
     * @return 미디어 view 페이지
     */
    Page<MediaView> findAll(Pageable pageable);
}
