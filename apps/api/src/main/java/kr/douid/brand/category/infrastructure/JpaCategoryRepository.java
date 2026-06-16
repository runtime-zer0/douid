package kr.douid.brand.category.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryRepository;

public interface JpaCategoryRepository extends JpaRepository<Category, Long>, CategoryRepository {

    @Override
    Category save(Category category);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    List<Category> findAllByVisibleTrueOrderByDisplayOrderAscCreatedAtAsc();

    default void delete(Category category) {
        deleteById(category.getId());
    }
}
