package kr.douid.brand.category.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    /**
     * 카테고리를 저장
     *
     * @param category 저장 대상 카테고리
     * @return 저장된 카테고리
     */
    @Override
    public Category save(Category category) {
        return categoryJpaRepository.save(category);
    }

    /**
     * ID로 카테고리를 조회
     *
     * @param id 조회할 카테고리 ID
     * @return 조회 결과
     */
    @Override
    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id);
    }

    /**
     * slug로 카테고리를 조회
     *
     * @param slug 조회할 카테고리 slug
     * @return 조회 결과
     */
    @Override
    public Optional<Category> findBySlug(String slug) {
        return categoryJpaRepository.findBySlug(slug);
    }

    /**
     * slug 중복 여부를 확인
     *
     * @param slug 확인할 카테고리 slug
     * @return slug 중복 여부
     */
    @Override
    public boolean existsBySlug(String slug) {
        return categoryJpaRepository.existsBySlug(slug);
    }

    /**
     * 현재 카테고리를 제외한 slug 중복 여부를 확인
     *
     * @param slug 확인할 카테고리 slug
     * @param id 제외할 카테고리 ID
     * @return slug 중복 여부
     */
    @Override
    public boolean existsBySlugAndIdNot(String slug, Long id) {
        return categoryJpaRepository.existsBySlugAndIdNot(slug, id);
    }

    /**
     * 카테고리를 삭제
     *
     * @param category 삭제 대상 카테고리
     */
    @Override
    public void delete(Category category) {
        categoryJpaRepository.delete(category);
    }
}
