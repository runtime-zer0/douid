package kr.douid.brand.category.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.category.domain.Category;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    /**
     * slug로 카테고리를 조회
     *
     * @param slug 조회할 카테고리 slug
     * @return 조회 결과
     */
    Optional<Category> findBySlug(String slug);

    /**
     * slug 중복 여부를 확인
     *
     * @param slug 확인할 카테고리 slug
     * @return slug 중복 여부
     */
    boolean existsBySlug(String slug);

    /**
     * 현재 카테고리를 제외한 slug 중복 여부를 확인
     *
     * @param slug 확인할 카테고리 slug
     * @param id 제외할 카테고리 ID
     * @return slug 중복 여부
     */
    boolean existsBySlugAndIdNot(String slug, Long id);
}
