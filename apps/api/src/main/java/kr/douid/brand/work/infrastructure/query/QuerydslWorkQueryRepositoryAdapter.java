package kr.douid.brand.work.infrastructure.query;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.douid.brand.work.application.query.AdminWorkDetail;
import kr.douid.brand.work.application.query.AdminWorkListItem;
import kr.douid.brand.work.application.query.PublicWorkDetail;
import kr.douid.brand.work.application.query.PublicWorkListItem;
import kr.douid.brand.work.application.query.WorkCategoryView;
import kr.douid.brand.work.application.query.WorkMediaView;
import kr.douid.brand.work.application.query.WorkQueryRepository;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkVisibility;
import static kr.douid.brand.category.domain.QCategory.category;
import static kr.douid.brand.media.domain.QMedia.media;
import static kr.douid.brand.work.domain.QWork.work;
import static kr.douid.brand.work.domain.QWorkMedia.workMedia;
import lombok.RequiredArgsConstructor;

/**
 * {@link WorkQueryRepository} port의 QueryDSL 구현체
 *
 * Query-side Composition Rule({@code apps/api/CLAUDE.md})과 work/CLAUDE.md의 Query Split 규칙에 따라
 * read model 구성을 위해 {@code QCategory}, {@code QMedia}를 join한다.
 * Category/Media entity는 외부로 반환하지 않고 Projection DTO로만 변환해 반환한다.
 */
@Repository
@RequiredArgsConstructor
public class QuerydslWorkQueryRepositoryAdapter implements WorkQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 관리자용 작업물 목록을 페이지네이션 조회
     *
     * Category는 leftJoin으로 연결해 categoryId가 없거나 비공개 Category에 속한 Work도 모두 포함한다.
     * 대표 이미지(THUMBNAIL)는 목록 쿼리 자체에 leftJoin으로 포함해 N+1 없이 조합한다.
     *
     * @param pageable 페이지네이션 파라미터
     * @return 관리자용 작업물 목록 페이지
     */
    @Override
    public Page<AdminWorkListItem> findAdminWorkList(Pageable pageable) {
        List<AdminWorkListItem> content = queryFactory
                .select(Projections.constructor(
                        AdminWorkListProjection.class,
                        work.id,
                        work.title,
                        work.slug,
                        work.visibility,
                        work.createdAt,
                        work.updatedAt,
                        category.id,
                        category.name,
                        category.slug,
                        category.visible,
                        media.id,
                        workMedia.role,
                        workMedia.sortOrder,
                        workMedia.altText,
                        media.filePath,
                        media.originalFilename
                ))
                .from(work)
                .leftJoin(category).on(category.id.eq(work.categoryId))
                .leftJoin(workMedia).on(workMedia.work.eq(work).and(workMedia.role.eq(WorkMediaRole.THUMBNAIL)))
                .leftJoin(media).on(media.id.eq(workMedia.mediaId))
                .orderBy(work.createdAt.desc(), work.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(AdminWorkListProjection::toItem)
                .toList();

        long total = queryFactory
                .select(work.count())
                .from(work)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 관리자용 작업물 상세를 조회
     *
     * Work+Category 단건 조회와 WorkMedia+Media 목록 조회를 분리해 WorkMedia 개수만큼 Work 행이
     * 중복되는 카테시안 곱을 방지한다.
     *
     * @param id Work 식별자
     * @return 관리자용 작업물 상세 (없으면 empty)
     */
    @Override
    public Optional<AdminWorkDetail> findAdminWorkDetail(Long id) {
        AdminWorkBasicProjection basic = queryFactory
                .select(Projections.constructor(
                        AdminWorkBasicProjection.class,
                        work.id, work.title, work.slug, work.summary, work.description,
                        work.visibility, work.createdAt, work.updatedAt,
                        category.id, category.name, category.slug, category.visible))
                .from(work)
                .leftJoin(category).on(category.id.eq(work.categoryId))
                .where(work.id.eq(id))
                .fetchOne();

        if (basic == null) {
            return Optional.empty();
        }

        WorkCategoryView categoryView = new WorkCategoryProjection(
                basic.categoryId(), basic.categoryName(),
                basic.categorySlug(), basic.categoryVisible())
                .toView();

        List<WorkMediaView> mediaItems = findMediaItems(id);

        return Optional.of(new AdminWorkDetail(
                basic.workId(),
                basic.title(),
                basic.slug(),
                basic.summary(),
                basic.description(),
                basic.visibility(),
                categoryView,
                mediaItems,
                basic.createdAt(),
                basic.updatedAt()));
    }

    /**
     * 공개 작업물 목록을 페이지네이션 조회
     *
     * Category는 innerJoin으로 연결해 Public Visibility Policy(Work 공개 AND Category 공개)를
     * join 조건 자체로 표현한다.
     *
     * @param pageable 페이지네이션 파라미터
     * @return 공개 작업물 목록 페이지
     */
    @Override
    public Page<PublicWorkListItem> findPublicWorkList(Pageable pageable) {
        return findPublicWorkListInternal(null, pageable);
    }

    /**
     * 공개 카테고리 slug 기준으로 공개 작업물 목록을 페이지네이션 조회
     *
     * {@link #findPublicWorkList}와 동일한 Public Visibility Policy join에 카테고리 slug 조건만 추가한다.
     * 카테고리가 존재하지 않거나 비공개면 join 결과가 없어 자연히 빈 페이지가 반환된다.
     *
     * @param categorySlug 카테고리 슬러그
     * @param pageable     페이지네이션 파라미터
     * @return 공개 작업물 목록 페이지
     */
    @Override
    public Page<PublicWorkListItem> findPublicWorkListByCategorySlug(String categorySlug, Pageable pageable) {
        return findPublicWorkListInternal(categorySlug, pageable);
    }

    /**
     * 공개 작업물 목록 조회 공통 구현
     *
     * {@link #findPublicWorkList}와 {@link #findPublicWorkListByCategorySlug}가 공유하는 join/정렬/페이징을
     * 한 곳에 모으고, categorySlug 유무로 where절 조건만 다르게 조립한다.
     *
     * @param categorySlug 카테고리 슬러그 (null이면 전체 공개 작업물 대상)
     * @param pageable     페이지네이션 파라미터
     * @return 공개 작업물 목록 페이지
     */
    private Page<PublicWorkListItem> findPublicWorkListInternal(String categorySlug, Pageable pageable) {
        var predicate = work.visibility.eq(WorkVisibility.VISIBLE).and(category.visible.isTrue());
        if (categorySlug != null) {
            predicate = predicate.and(category.slug.eq(categorySlug));
        }

        List<PublicWorkListItem> content = queryFactory
                .select(Projections.constructor(
                        PublicWorkListProjection.class,
                        work.title, work.slug, work.summary,
                        category.id, category.name, category.slug, category.visible,
                        media.id, workMedia.role, workMedia.sortOrder, workMedia.altText,
                        media.filePath, media.originalFilename
                ))
                .from(work)
                .innerJoin(category).on(category.id.eq(work.categoryId))
                .leftJoin(workMedia).on(workMedia.work.eq(work).and(workMedia.role.eq(WorkMediaRole.THUMBNAIL)))
                .leftJoin(media).on(media.id.eq(workMedia.mediaId))
                .where(predicate)
                .orderBy(work.createdAt.desc(), work.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(PublicWorkListProjection::toItem)
                .toList();

        long total = queryFactory
                .select(work.count())
                .from(work)
                .innerJoin(category).on(category.id.eq(work.categoryId))
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * slug로 공개 작업물 상세를 조회
     *
     * 미존재, Work 비공개, Category 비공개 세 경우 모두 Public Visibility Policy where절에서
     * 자연스럽게 걸러져 별도 분기 없이 동일하게 {@code Optional.empty()}를 반환한다.
     *
     * @param slug 작업물 슬러그
     * @return 공개 작업물 상세 (조건 불충족 시 empty)
     */
    @Override
    public Optional<PublicWorkDetail> findPublicWorkDetailBySlug(String slug) {
        PublicWorkBasicProjection basic = queryFactory
                .select(Projections.constructor(
                        PublicWorkBasicProjection.class,
                        work.id, work.title, work.slug, work.summary, work.description,
                        category.id, category.name, category.slug, category.visible))
                .from(work)
                .innerJoin(category).on(category.id.eq(work.categoryId))
                .where(work.slug.eq(slug)
                        .and(work.visibility.eq(WorkVisibility.VISIBLE))
                        .and(category.visible.isTrue()))
                .fetchOne();

        if (basic == null) {
            return Optional.empty();
        }

        WorkCategoryView categoryView = new WorkCategoryProjection(
                basic.categoryId(), basic.categoryName(),
                basic.categorySlug(), basic.categoryVisible())
                .toView();

        List<WorkMediaView> mediaItems = findMediaItems(basic.workId());

        return Optional.of(new PublicWorkDetail(
                basic.title(),
                basic.slug(),
                basic.summary(),
                basic.description(),
                categoryView,
                mediaItems));
    }

    private List<WorkMediaView> findMediaItems(Long workId) {
        return queryFactory
                .select(Projections.constructor(
                        WorkMediaProjection.class,
                        media.id,
                        workMedia.role,
                        workMedia.sortOrder,
                        workMedia.altText,
                        media.filePath,
                        media.originalFilename
                ))
                .from(workMedia)
                .innerJoin(media).on(media.id.eq(workMedia.mediaId))
                .where(workMedia.work.id.eq(workId))
                .orderBy(workMedia.sortOrder.asc())
                .fetch()
                .stream()
                .map(WorkMediaProjection::toView)
                .toList();
    }
}
