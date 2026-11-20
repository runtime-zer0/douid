package kr.douid.brand.work.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.querydsl.jpa.impl.JPAQueryFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.PersistenceException;
import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.infrastructure.persistence.CategoryJpaRepository;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.infrastructure.persistence.MediaJpaRepository;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;
import kr.douid.brand.work.application.port.PublicWorkSearchException;
import kr.douid.brand.work.application.query.PublicWorkSearchRow;
import kr.douid.brand.work.application.query.SearchTermGroup;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkMediaItem;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkVisibility;
import kr.douid.brand.work.infrastructure.persistence.WorkJpaRepository;
import kr.douid.brand.work.infrastructure.query.QuerydslPublicWorkSearchAdapter;
import kr.douid.brand.work.infrastructure.query.QuerydslWorkQueryRepositoryAdapter;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslPublicWorkSearchAdapter.class, QuerydslWorkQueryRepositoryAdapter.class,
        QuerydslPublicWorkSearchAdapterTest.MeterConfig.class})
class QuerydslPublicWorkSearchAdapterTest extends PostgresIntegrationTest {

    @TestConfiguration
    static class MeterConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private WorkJpaRepository workJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private MediaJpaRepository mediaJpaRepository;

    @Autowired
    private QuerydslPublicWorkSearchAdapter adapter;

    @Autowired
    private QuerydslWorkQueryRepositoryAdapter workQueryAdapter;

    @Autowired
    private MeterRegistry meterRegistry;

    private Category category(String name, String slug, boolean visible) {
        return categoryJpaRepository.save(Category.create(name, slug, 0, visible));
    }

    private Category branding() {
        return category("Branding", "branding", true);
    }

    private Work work(String slug, Category category, String title, String summary, String description) {
        return work(slug, category == null ? null : category.getId(), WorkVisibility.VISIBLE, title, summary, description);
    }

    private Work work(String slug, Long categoryId, WorkVisibility visibility,
            String title, String summary, String description) {
        return workJpaRepository.saveAndFlush(Work.create(title, slug, summary, description, categoryId, visibility));
    }

    private void altTexts(Work work, String... altTexts) {
        List<WorkMediaItem> items = new ArrayList<>();
        for (int i = 0; i < altTexts.length; i++) {
            Media media = mediaJpaRepository.save(
                    Media.upload("m" + i + ".png", work.getSlug() + i, "media/" + work.getSlug() + i, "image/png", 1L));
            items.add(new WorkMediaItem(media.getId(), WorkMediaRole.DETAIL_IMAGE, i, altTexts[i]));
        }
        work.replaceMediaItems(items);
        workJpaRepository.saveAndFlush(work);
    }

    private static List<SearchTermGroup> groups(String... variants) {
        return Arrays.stream(variants).map(variant -> new SearchTermGroup(List.of(variant))).toList();
    }

    private List<String> slugs(List<SearchTermGroup> groups) {
        return adapter.search(groups, 9).stream().map(PublicWorkSearchRow::slug).toList();
    }

    @Nested
    class 관련도 {

        @Test
        void 필드별_가중치_순서는_제목_요약_alt_text_설명() {
            Category branding = branding();
            Work description = work("stay-description", branding, "스테이 C", "숙소", "오래된 한옥의 결");
            Work alt = work("stay-alt", branding, "스테이 D", "숙소 사인", null);
            altTexts(alt, "한옥 대문 사진");
            work("stay-summary", branding, "스테이 B", "한옥을 개조한 숙소", null);
            work("hanok-title", branding, "한옥 스테이", "숙소", null);
            work("unrelated", branding, "베이커리", "빵집", null);

            assertThat(slugs(groups("한옥")))
                    .containsExactly("hanok-title", "stay-summary", "stay-alt", description.getSlug());
        }

        @Test
        void 여러_그룹이_일치한_작업물이_앞선다() {
            Category branding = branding();
            work("cafe-only", branding, "카페 사인", null, null);
            work("cafe-bakery", branding, "베이커리", "카페 겸 베이커리", null);

            assertThat(slugs(groups("카페", "베이커리"))).containsExactly("cafe-bakery", "cafe-only");
        }

        @Test
        void 같은_점수는_최근_생성_순() {
            Category branding = branding();
            work("roasters-tie-1", branding, "Roastery Tie", "동점", null);
            work("roasters-tie-2", branding, "Roastery Tie", "동점", null);

            assertThat(slugs(groups("roastery"))).containsExactly("roasters-tie-2", "roasters-tie-1");
        }

        @Test
        void 그룹_안의_표기는_OR로_묶여_한_번만_점수를_더한다() {
            Category branding = branding();
            work("both-variants", branding, "카페 coffee", null, null);
            work("summary-cafe", branding, "베이커리", "카페", null);
            work("title-coffee", branding, "Coffee Roasters", null, null);

            List<PublicWorkSearchRow> rows = adapter.search(List.of(new SearchTermGroup(List.of("카페", "coffee"))), 9);

            // 두 표기 모두 제목에 있어도 5점 한 번이라 같은 점수인 title-coffee와 생성 순으로만 갈림
            assertThat(rows).extracting(PublicWorkSearchRow::slug)
                    .containsExactly("title-coffee", "both-variants", "summary-cafe");
        }
    }

    @Nested
    class 공개_범위 {

        @Test
        void 비공개_작업물은_어떤_필드가_일치해도_나오지_않는다() {
            Category branding = branding();
            Category hiddenCategory = category("Editorial", "editorial", false);
            work("visible-cafe", branding, "카페", null, null);
            work("secret-cafe-renewal", branding.getId(), WorkVisibility.HIDDEN, "Secret Cafe 카페", "카페", "카페");
            work("cafe-zine-editorial", hiddenCategory.getId(), WorkVisibility.VISIBLE, "카페 매거진", null, null);
            work("no-category-cafe-menu", null, WorkVisibility.VISIBLE, "카페 메뉴판", null, null);
            Work hiddenAlt = work("hidden-alt", branding.getId(), WorkVisibility.HIDDEN, "숨김", null, null);
            altTexts(hiddenAlt, "카페 사진");

            assertThat(slugs(groups("카페"))).containsExactly("visible-cafe");
            assertThat(slugs(groups("secret", "cafe", "renewal"))).isEmpty();
            assertThat(slugs(groups("editorial", "매거진"))).isEmpty();
            assertThat(slugs(groups("메뉴판"))).isEmpty();
        }

        @Test
        void 공개_상태를_바꾸면_바로_다음_검색에_반영된다() {
            Category branding = branding();
            Work target = work("hanok-stay", branding, "한옥 스테이", null, null);
            assertThat(slugs(groups("한옥"))).containsExactly("hanok-stay");

            target.changeVisibility(WorkVisibility.HIDDEN);
            workJpaRepository.saveAndFlush(target);
            assertThat(slugs(groups("한옥"))).isEmpty();

            target.changeVisibility(WorkVisibility.VISIBLE);
            workJpaRepository.saveAndFlush(target);
            branding.update("Branding", "branding", 0, false);
            categoryJpaRepository.saveAndFlush(branding);
            assertThat(slugs(groups("한옥"))).isEmpty();
        }
    }

    @Nested
    class 카테고리 {

        @Test
        void 카테고리_이름이나_slug가_일치하면_찾는다() {
            Category packaging = category("Packaging Design", "packaging-design", true);
            Category detail = category("상세 이미지", "detail-page", true);
            work("snack-pouch", packaging, "Snack Pouch", null, null);
            work("skincare", detail, "스킨케어", null, null);

            assertThat(slugs(groups("packag"))).containsExactly("snack-pouch");
            assertThat(slugs(groups("detail"))).containsExactly("skincare");
        }

        @Test
        void 이름과_slug가_함께_일치해도_카테고리_점수는_한_번이다() {
            Category packaging = category("Packaging Design", "packaging-design", true);
            Category other = category("Other", "other", true);
            work("category-only", packaging, "Snack Pouch", null, null);
            work("summary-only", other, "Box", "packaging box", null);
            // 카테고리 점수가 두 번(8점) 더해지면 category-only가 title-only(5점)보다 앞섬
            work("title-only", other, "Packaging Kit", null, null);

            assertThat(slugs(groups("packaging"))).containsExactly("title-only", "category-only", "summary-only");
        }

        @Test
        void 카테고리와_제목이_함께_일치하면_카테고리만_일치한_작업물보다_앞선다() {
            Category branding = branding();
            work("cafe-mellow-branding", branding, "Cafe Mellow 브랜딩", null, null);
            work("onda-bakery", branding, "온다 베이커리", null, null);

            List<SearchTermGroup> brandGroup = List.of(new SearchTermGroup(List.of("브랜딩", "브랜드", "brand")));

            assertThat(slugs(brandGroup)).containsExactly("cafe-mellow-branding", "onda-bakery");
        }
    }

    @Nested
    class 빈_결과와_실패 {

        @Test
        void 공개_작업물이_없으면_빈_목록() {
            assertThat(slugs(groups("카페"))).isEmpty();
        }

        @Test
        void 어떤_필드와도_일치하지_않으면_빈_목록() {
            work("cafe", branding(), "카페", "요약", "설명");

            assertThat(slugs(groups("주차"))).isEmpty();
        }

        @Test
        void 질의_실행_실패는_질의어_없는_검색_예외로_바뀐다() {
            JPAQueryFactory failing = mock(JPAQueryFactory.class, invocation -> {
                throw new PersistenceException("select ... where lower(title) like '%비밀질의어%'");
            });
            QuerydslPublicWorkSearchAdapter failingAdapter =
                    new QuerydslPublicWorkSearchAdapter(failing, new StaticListableBeanFactory().getBeanProvider(MeterRegistry.class));

            assertThatThrownBy(() -> failingAdapter.search(groups("비밀질의어"), 5))
                    .isInstanceOf(PublicWorkSearchException.class)
                    .hasMessage("작업물 검색을 처리하지 못했습니다.")
                    .hasMessageNotContaining("비밀질의어");
        }
    }

    @Nested
    class 상한과_특수_문자 {

        @Test
        void 일치가_많아도_limit만큼만_돌려준다() {
            Category branding = branding();
            for (int i = 0; i < 15; i++) {
                work("cafe-" + i, branding, "카페 " + i, null, null);
            }

            assertThat(adapter.search(groups("카페"), 5)).hasSize(5);
        }

        @Test
        void alt_text가_여러_개_일치해도_작업물은_한_번만_나온다() {
            Work stay = work("stay-alt", branding(), "스테이", null, null);
            altTexts(stay, "한옥 대문", "한옥 마당");

            assertThat(slugs(groups("한옥"))).containsExactly("stay-alt");
        }

        @Test
        void 와일드카드와_escape_문자는_문자_그대로_일치한다() {
            Category branding = branding();
            work("percent", branding, "할인 100% 패키지", null, null);
            work("underscore", branding, "snake_case 로고", null, null);
            work("bang", branding, "wow! 포스터", null, null);
            work("backslash", branding, "path\\name 배너", null, null);
            work("plain", branding, "할인 1000 패키지 snakeXcase wowX pathXname", null, null);

            assertThat(slugs(groups("100%"))).containsExactly("percent");
            assertThat(slugs(groups("e_c"))).containsExactly("underscore");
            assertThat(slugs(groups("w!"))).containsExactly("bang");
            assertThat(slugs(groups("h\\n"))).containsExactly("backslash");
            assertThat(slugs(groups("%"))).containsExactly("percent");
            assertThat(slugs(groups("_"))).containsExactly("underscore");
        }

        @Test
        void 요약과_설명이_없는_작업물도_제목으로_찾는다() {
            work("title-only", branding(), "한옥 스테이", null, null);

            assertThat(adapter.search(groups("한옥"), 5))
                    .containsExactly(new PublicWorkSearchRow("title-only", "한옥 스테이", null, "Branding", "branding"));
        }
    }

    @Nested
    class 계약_연결과_관측 {

        @Test
        void 검색_결과_slug로_공개_상세를_조회할_수_있다() {
            work("hanok-stay", branding(), "한옥 스테이", null, null);

            String slug = adapter.search(groups("한옥"), 5).getFirst().slug();

            assertThat(workQueryAdapter.findPublicWorkDetailBySlug(slug)).isPresent();
        }

        @Test
        void metric에는_질의어가_남지_않고_outcome_tag만_있다() {
            work("hanok-stay", branding(), "한옥 스테이", null, null);

            adapter.search(groups("한옥", "비밀질의어"), 5);

            assertThat(meterRegistry.find("douid.work.search").timers()).isNotEmpty();
            assertThat(meterRegistry.find("douid.work.search.results").summary()).isNotNull();
            meterRegistry.getMeters().stream()
                    .filter(meter -> meter.getId().getName().startsWith("douid.work.search"))
                    .forEach(meter -> {
                        assertThat(meter.getId().getTags()).extracting(Tag::getKey)
                                .allMatch("outcome"::equals);
                        assertThat(meter.getId().getTags()).extracting(Tag::getValue)
                                .allMatch(value -> value.equals("success") || value.equals("failure"));
                    });
        }
    }
}
