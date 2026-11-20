package kr.douid.brand.work.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PortfolioSearchTermResolverTest {

    private final PortfolioSearchTermResolver resolver = new PortfolioSearchTermResolver();
    private final PortfolioSearchTermResolver withoutSynonyms = new PortfolioSearchTermResolver(List.of());

    private static List<List<String>> variants(List<SearchTermGroup> groups) {
        return groups.stream().map(SearchTermGroup::variants).toList();
    }

    @Nested
    class 문장_분리 {

        @Test
        void 공백과_구두점으로_나누고_소문자로_바꾼다() {
            List<SearchTermGroup> groups = withoutSynonyms.resolve("Mellow,  한옥!스테이?");

            assertThat(variants(groups)).containsExactly(List.of("mellow"), List.of("한옥"), List.of("스테이", "스테"));
        }

        @Test
        void 조사가_붙은_단어는_원형과_제거형을_함께_쓴다() {
            assertThat(variants(withoutSynonyms.resolve("카페의 패키지를")))
                    .containsExactly(List.of("카페의", "카페"), List.of("패키지를", "패키지"));
        }

        @Test
        void 조사를_떼면_2글자_미만이면_원형만_쓴다() {
            assertThat(variants(withoutSynonyms.resolve("차를 마셔"))).containsExactly(List.of("차를"), List.of("마셔"));
        }

        @Test
        void 불용_표현과_서술어_어미_토큰은_버린다() {
            List<SearchTermGroup> groups = withoutSynonyms.resolve("안녕하세요 한옥 작업 포트폴리오 디자인 사례 있어요 보여주세요 있나요");

            assertThat(variants(groups)).containsExactly(List.of("한옥"));
        }

        @Test
        void 조사를_뗀_형태가_불용_표현이면_토큰을_버린다() {
            assertThat(withoutSynonyms.resolve("작업을 포트폴리오를 디자인이")).isEmpty();
        }

        @Test
        void 같은_그룹은_한_번만_등장_순서대로_남긴다() {
            assertThat(variants(withoutSynonyms.resolve("한옥 스테이 한옥")))
                    .containsExactly(List.of("한옥"), List.of("스테이", "스테"));
        }
    }

    @Nested
    class 동의어 {

        @Test
        void 한국어와_영어_패키지_표현은_같은_그룹이_된다() {
            List<String> expected = List.of("패키지", "패키징", "packag");

            assertThat(variants(resolver.resolve("패키지"))).containsExactly(expected);
            assertThat(variants(resolver.resolve("패키지를"))).containsExactly(expected);
            assertThat(variants(resolver.resolve("Packaging"))).containsExactly(expected);
        }

        @Test
        void 대소문자가_달라도_같은_그룹이_된다() {
            assertThat(resolver.resolve("BRANDING")).isEqualTo(resolver.resolve("branding"));
        }

        @Test
        void 다른_토큰이_같은_그룹이면_한_번만_남긴다() {
            assertThat(variants(resolver.resolve("카페 커피 cafe"))).containsExactly(
                    List.of("카페", "커피", "cafe", "café", "coffee"));
        }

        @Test
        void 그룹당_표기는_5개를_넘지_않는다() {
            List<SearchTermGroup> groups = resolver.resolve("패키지 브랜딩 상세페이지 로고 카페 라벨 편집 웹사이트 일러스트 굿즈");

            assertThat(groups).allSatisfy(group -> assertThat(group.variants()).hasSizeLessThanOrEqualTo(5));
        }

        @Test
        void 빈_동의어_표로_만들면_확장하지_않는다() {
            assertThat(variants(withoutSynonyms.resolve("패키지"))).containsExactly(List.of("패키지"));
        }
    }

    @Nested
    class 검색어_없음 {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "안녕하세요", "있어요?", "디자인 작업 보여줘"})
        void 검색어가_없으면_빈_목록(String text) {
            assertThat(resolver.resolve(text)).isEmpty();
        }
    }

    @Nested
    class 비신뢰_입력 {

        @Test
        void 입력은_앞_500_code_point만_쓴다() {
            String head = "다".repeat(499) + " ";
            String text = head + "한옥";

            assertThat(resolver.resolve(text)).isEqualTo(resolver.resolve(head));
            assertThat(variants(resolver.resolve(text))).containsExactly(List.of("다".repeat(499)));
        }

        @Test
        void 대리_쌍_경계에서_잘려도_깨지지_않는다() {
            String text = "다".repeat(499) + "😀한옥";

            assertThat(variants(resolver.resolve(text))).containsExactly(List.of("다".repeat(499)));
        }

        @Test
        void 그룹은_최대_6개() {
            List<SearchTermGroup> groups = withoutSynonyms.resolve("하나 둘둘 셋셋 넷넷 다섯 여섯 일곱 여덟");

            assertThat(groups).hasSize(6);
            assertThat(groups.get(5).variants()).containsExactly("여섯");
        }

        @Test
        void 제어_문자_탭_개행은_구분자로_처리한다() {
            assertThat(variants(withoutSynonyms.resolve("한옥\t스테이\n숙소\u0000정원")))
                    .containsExactly(List.of("한옥"), List.of("스테이", "스테"), List.of("숙소"), List.of("정원"));
        }

        @Test
        void 이모지는_버린다() {
            assertThat(resolver.resolve("☕️ 카페 브랜딩 🙏")).isEqualTo(resolver.resolve("카페 브랜딩"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"%", "_", "\\", "%%__", "' OR 1=1 --", "'; DROP TABLE works; --"})
        void 특수_문자와_SQL_구문만_있으면_빈_목록이거나_문자_토큰만_남는다(String text) {
            assertThat(resolver.resolve(text)).allSatisfy(group -> assertThat(group.variants())
                    .allSatisfy(variant -> assertThat(variant).matches("[\\p{L}\\p{N}]+")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"%", "_", "\\", "' OR 1=1 --"})
        void 와일드카드와_따옴표만_있으면_빈_목록(String text) {
            assertThat(resolver.resolve(text)).isEmpty();
        }

        @Test
        void NFD_입력은_NFC와_같은_그룹이_된다() {
            String nfd = Normalizer.normalize("카페 브랜딩", Normalizer.Form.NFD);

            assertThat(resolver.resolve(nfd)).isEqualTo(resolver.resolve("카페 브랜딩"));
        }

        @Test
        void 호환_자모는_음절과_다른_그룹이다() {
            assertThat(variants(resolver.resolve("ㅋㅏㅍㅔ"))).containsExactly(List.of("ㅋㅏㅍㅔ"));
        }
    }
}
