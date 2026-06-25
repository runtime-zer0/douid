package kr.douid.brand.media.infrastructure.query;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.douid.brand.media.application.query.MediaQueryRepository;
import kr.douid.brand.media.application.query.MediaView;
import static kr.douid.brand.media.domain.QMedia.media;
import lombok.RequiredArgsConstructor;

/**
 * {@link MediaQueryRepository} port의 QueryDSL 구현체
 *
 * JPAQueryFactory 직접 조회와 DTO projection 반환
 * Spring Data JPA Repository 주입 및 호출 금지
 */
@Repository
@RequiredArgsConstructor
public class QuerydslMediaQueryRepositoryAdapter implements MediaQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 식별자로 미디어 단건 조회
     *
     * @param id 미디어 식별자
     * @return 미디어 view (없으면 empty)
     */
    @Override
    public Optional<MediaView> findById(Long id) {
        MediaListProjection projection = queryFactory
                .select(Projections.constructor(
                        MediaListProjection.class,
                        media.id,
                        media.originalFilename,
                        media.filePath,
                        media.contentType,
                        media.fileSize,
                        media.createdAt))
                .from(media)
                .where(media.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(projection).map(MediaListProjection::toView);
    }

    /**
     * 미디어 목록을 최근 업로드 순으로 페이지네이션 조회
     *
     * @param pageable 페이지네이션 파라미터
     * @return 미디어 view 페이지
     */
    @Override
    public Page<MediaView> findAll(Pageable pageable) {
        List<MediaView> content = queryFactory
                .select(Projections.constructor(
                        MediaListProjection.class,
                        media.id,
                        media.originalFilename,
                        media.filePath,
                        media.contentType,
                        media.fileSize,
                        media.createdAt))
                .from(media)
                .orderBy(media.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(MediaListProjection::toView)
                .toList();

        long total = queryFactory
                .select(media.count())
                .from(media)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
