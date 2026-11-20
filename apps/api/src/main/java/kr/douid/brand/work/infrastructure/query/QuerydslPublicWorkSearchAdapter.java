package kr.douid.brand.work.infrastructure.query;

import static kr.douid.brand.category.domain.QCategory.category;
import static kr.douid.brand.work.domain.QWork.work;
import static kr.douid.brand.work.domain.QWorkMedia.workMedia;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.PersistenceException;
import kr.douid.brand.work.application.port.PublicWorkSearchException;
import kr.douid.brand.work.application.query.PublicWorkSearchQueryRepository;
import kr.douid.brand.work.application.query.PublicWorkSearchRow;
import kr.douid.brand.work.application.query.SearchTermGroup;

/**
 * {@link PublicWorkSearchQueryRepository} port의 QueryDSL 구현체
 *
 * 그룹마다 일치한 필드 가중치(제목 5, 카테고리 4, 요약 3, alt text 2, 설명 1)를 더한 점수로 정렬한다.
 * 공개 조건은 {@link PublicWorkVisibility}를 where절에 적용하고, alt text는 EXISTS로만 판정해 Work 행이 늘지 않는다.
 * 질의어는 로그와 Metric tag에 남기지 않는다.
 */
@Repository
public class QuerydslPublicWorkSearchAdapter implements PublicWorkSearchQueryRepository {

    private static final String TIMER = "douid.work.search";
    private static final String RESULTS = "douid.work.search.results";

    private final JPAQueryFactory queryFactory;
    private final MeterRegistry meterRegistry;

    /**
     * 검색 adapter 생성
     *
     * @param queryFactory QueryDSL 조회 팩토리
     * @param registries   관측 registry (없으면 기록만 버림)
     */
    public QuerydslPublicWorkSearchAdapter(JPAQueryFactory queryFactory, ObjectProvider<MeterRegistry> registries) {
        this.queryFactory = queryFactory;
        this.meterRegistry = registries.getIfAvailable(SimpleMeterRegistry::new);
    }

    /**
     * 검색 표기 그룹과 일치하는 공개 작업물을 관련도 순으로 조회
     *
     * @param groups 검색 표기 그룹 (1개 이상)
     * @param limit  최대 결과 수
     * @return 관련도 순 결과 행, 일치가 없으면 빈 목록
     * @throws PublicWorkSearchException 조회를 실행하지 못했을 때
     */
    @Override
    public List<PublicWorkSearchRow> search(List<SearchTermGroup> groups, int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            NumberExpression<Integer> score = score(groups);
            List<PublicWorkSearchRow> rows = queryFactory
                    .select(Projections.constructor(
                            PublicWorkSearchRow.class,
                            work.slug, work.title, work.summary, category.name, category.slug))
                    .from(work)
                    .innerJoin(category).on(category.id.eq(work.categoryId))
                    .where(PublicWorkVisibility.condition().and(score.gt(0)))
                    .orderBy(score.desc(), work.createdAt.desc(), work.id.desc())
                    .limit(limit)
                    .fetch();
            sample.stop(timer("success"));
            DistributionSummary.builder(RESULTS).register(meterRegistry).record(rows.size());
            return rows;
        } catch (DataAccessException | PersistenceException e) {
            sample.stop(timer("failure"));
            throw new PublicWorkSearchException(e);
        }
    }

    private Timer timer(String outcome) {
        return Timer.builder(TIMER).tag("outcome", outcome).register(meterRegistry);
    }

    private static NumberExpression<Integer> score(List<SearchTermGroup> groups) {
        NumberExpression<Integer> score = Expressions.asNumber(0);
        for (SearchTermGroup group : groups) {
            List<String> variants = group.variants();
            score = score
                    .add(weight(matches(work.title, variants), 5))
                    .add(weight(matches(category.name, variants).or(matches(category.slug, variants)), 4))
                    .add(weight(matches(work.summary, variants), 3))
                    .add(weight(JPAExpressions.selectOne()
                            .from(workMedia)
                            .where(workMedia.work.id.eq(work.id), matches(workMedia.altText, variants))
                            .exists(), 2))
                    .add(weight(matches(work.description, variants), 1));
        }
        return score;
    }

    private static NumberExpression<Integer> weight(BooleanExpression matched, int weight) {
        return new CaseBuilder().when(matched).then(weight).otherwise(0);
    }

    private static BooleanExpression matches(StringExpression field, List<String> variants) {
        BooleanExpression any = null;
        for (String variant : variants) {
            BooleanExpression one = field.containsIgnoreCase(variant);
            any = any == null ? one : any.or(one);
        }
        return any;
    }
}
