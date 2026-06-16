package kr.douid.brand.category.domain;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    List<Category> findAllByVisibleTrueOrderByDisplayOrderAscCreatedAtAsc();

    void delete(Category category);
}
