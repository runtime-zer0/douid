package kr.douid.brand.work.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.work.domain.Work;

/**
 * 작업물 JPA 전용 기술 repository
 *
 * Domain Repository 직접 상속 없이 {@link JpaWorkRepositoryAdapter}에서 위임 호출
 */
public interface WorkJpaRepository extends JpaRepository<Work, Long> {

    /**
     * 슬러그 중복 여부를 확인
     *
     * @param slug 확인할 슬러그
     * @return 중복 여부
     */
    boolean existsBySlug(String slug);

    /**
     * 현재 작업물을 제외한 슬러그 중복 여부를 확인
     *
     * @param slug 확인할 슬러그
     * @param id   제외할 작업물 ID
     * @return 중복 여부
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * 카테고리 ID로 연결된 작업물 존재 여부를 확인
     *
     * @param categoryId 카테고리 ID
     * @return 연결된 작업물 존재 여부
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * 미디어 ID로 연결된 작업물 존재 여부를 확인
     *
     * @param mediaId 미디어 ID
     * @return 연결된 작업물 존재 여부
     */
    boolean existsByMediaItems_MediaId(Long mediaId);
}
