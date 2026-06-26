package kr.douid.brand.work.infrastructure.port;

import org.springframework.stereotype.Component;

import kr.douid.brand.work.application.port.WorkReferenceChecker;
import kr.douid.brand.work.infrastructure.persistence.WorkJpaRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link WorkReferenceChecker} port의 JPA 구현체
 */
@Component
@RequiredArgsConstructor
public class JpaWorkReferenceCheckerAdapter implements WorkReferenceChecker {

    private final WorkJpaRepository workJpaRepository;

    /**
     * 해당 카테고리를 참조하는 작업물 존재 여부를 확인
     *
     * @param categoryId 확인할 카테고리 ID
     * @return 참조 작업물 존재 여부
     */
    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return workJpaRepository.existsByCategoryId(categoryId);
    }

    /**
     * 해당 미디어를 참조하는 작업물 존재 여부를 확인
     *
     * @param mediaId 확인할 미디어 ID
     * @return 참조 작업물 존재 여부
     */
    @Override
    public boolean existsByMediaId(Long mediaId) {
        return workJpaRepository.existsByMediaItems_MediaId(mediaId);
    }
}
