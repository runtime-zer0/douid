package kr.douid.brand.work.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link WorkRepository} domain port의 JPA 구현체
 *
 * {@link WorkJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaWorkRepositoryAdapter implements WorkRepository {

    private final WorkJpaRepository workJpaRepository;

    /**
     * 작업물을 저장하고 반환
     *
     * @param work 저장할 작업물
     * @return 저장된 작업물
     */
    @Override
    public Work save(Work work) {
        return workJpaRepository.save(work);
    }

    /**
     * ID로 작업물을 조회
     *
     * @param id 작업물 ID
     * @return 작업물 (없으면 empty)
     */
    @Override
    public Optional<Work> findById(Long id) {
        return workJpaRepository.findById(id);
    }

    /**
     * 슬러그 중복 여부를 확인
     *
     * @param slug 확인할 슬러그
     * @return 슬러그 중복 여부
     */
    @Override
    public boolean existsBySlug(String slug) {
        return workJpaRepository.existsBySlug(slug);
    }

    /**
     * 현재 작업물을 제외한 슬러그 중복 여부를 확인
     *
     * @param slug 확인할 슬러그
     * @param id   제외할 작업물 ID
     * @return 슬러그 중복 여부
     */
    @Override
    public boolean existsBySlugAndIdNot(String slug, Long id) {
        return workJpaRepository.existsBySlugAndIdNot(slug, id);
    }

    /**
     * 작업물을 삭제
     *
     * @param work 삭제할 작업물
     */
    @Override
    public void delete(Work work) {
        workJpaRepository.delete(work);
    }

    /**
     * 변경 사항을 즉시 DB에 반영
     */
    @Override
    public void flush() {
        workJpaRepository.flush();
    }
}
