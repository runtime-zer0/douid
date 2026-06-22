package kr.douid.brand.category.infrastructure.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.douid.brand.category.application.query.CategoryListItem;
import kr.douid.brand.category.application.query.CategoryQueryRepository;
import static kr.douid.brand.category.domain.QCategory.category;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QuerydslCategoryQueryRepositoryAdapter implements CategoryQueryRepository {

   private final JPAQueryFactory queryFactory;

   /**
    * 관리자용 카테고리 목록을 조회
    *
    * @return 관리자용 카테고리 목록
    */
   @Override
   public List<CategoryListItem> findAdminCategoryList() {

      return queryFactory.select(Projections.constructor(CategoryListProjection.class, 
      category.id,
      category.name,
      category.slug,
      category.displayOrder,
      category.visible
      )).from(category)
      .orderBy(
         category.displayOrder.asc(),
   category.createdAt.asc()
).fetch().stream()
.map(CategoryListProjection::toItem)
.toList();

   }

   /**
    * 공개용 카테고리 목록을 조회
    *
    * @return 공개용 카테고리 목록
    */
   @Override
   public List<CategoryListItem> findPublicCategoryList() {
      return queryFactory
            .select(Projections.constructor(
                CategoryListProjection.class,
                category.id,
                category.name,
                category.slug,
                category.displayOrder,
                category.visible
            ))
            .from(category)
            .where(category.visible.isTrue())
            .orderBy(
                category.displayOrder.asc(),
                category.createdAt.asc()
            )
            .fetch()
            .stream()
            .map(CategoryListProjection::toItem)
            .toList();
   }

   
}
