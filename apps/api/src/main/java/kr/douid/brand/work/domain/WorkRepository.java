package kr.douid.brand.work.domain;

import java.util.Optional;

/**
 * 작업물 Aggregate 저장·복원을 위한 domain port
 */
public interface WorkRepository {

    /**
     * 작업물을 저장하고 반환
     *
     * @param work 저장할 작업물
     * @return 저장된 작업물
     */
    Work save(Work work);

    /**
     * ID로 작업물을 조회
     *
     * @param id 작업물 ID
     * @return 작업물 (없으면 empty)
     */
    Optional<Work> findById(Long id);

    /**
     * 슬러그 중복 여부를 확인
     *
     * @param slug 확인할 슬러그
     * @return 슬러그 중복 여부
     */
    boolean existsBySlug(String slug);

    /**
     * 현재 작업물을 제외한 슬러그 중복 여부를 확인
     *
     * @param slug 확인할 슬러그
     * @param id   제외할 작업물 ID
     * @return 슬러그 중복 여부
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * 작업물을 삭제
     *
     * @param work 삭제할 작업물
     */
    void delete(Work work);

    /**
     * 변경 사항을 즉시 DB에 반영
     *
     * 동시 수정 등으로 인한 unique constraint 위반을 트랜잭션 내에서 조기에 감지할 때 사용
     */
    void flush();
}
