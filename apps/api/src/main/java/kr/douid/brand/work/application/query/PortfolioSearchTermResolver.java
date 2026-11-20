package kr.douid.brand.work.application.query;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 방문자 자연어 질문에서 작업물 검색 표기 그룹을 뽑는 정책 객체
 *
 * 형태소 분석 없이 분리, 조사 제거, 불용 표현 제외, 동의어 확장 규칙만 적용한다.
 * 상태가 없고 같은 입력에는 항상 같은 결과를 낸다.
 */
public class PortfolioSearchTermResolver {

    static final int MAX_INPUT_CODE_POINTS = 500;
    static final int MAX_GROUPS = 6;
    private static final int MIN_TERM_CODE_POINTS = 2;

    // 단일 문자 클래스 반복이라 backtracking이 생기지 않음
    private static final Pattern SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    // 긴 조사부터 확인해야 "에서"가 "서"보다 먼저 떨어짐
    private static final List<String> PARTICLES = List.of(
            "에서는", "으로는", "이라는", "에서", "으로", "이랑", "하고", "처럼", "까지", "부터", "이요",
            "은", "는", "이", "가", "을", "를", "의", "에", "와", "과", "도", "만", "로", "랑", "요");

    private static final List<String> PREDICATE_ENDINGS = List.of(
            "나요", "세요", "어요", "아요", "해요", "까요", "줘", "습니다");

    private static final Set<String> STOPWORDS = Set.of(
            "안녕", "안녕하세요", "있어", "있어요", "있나요", "보여", "보여줘", "보여주세요", "주세요", "해주세요",
            "보고", "싶어요", "알려줘", "혹시", "어떤", "관련", "느낌", "만든", "만들어", "작업", "작업물", "포트폴리오",
            "사례", "디자인", "그리고", "저희", "제가",
            "the", "and", "or", "show", "me", "please", "work", "works", "portfolio", "design", "of", "for", "with");

    private static final List<Synonym> DEFAULT_SYNONYMS = List.of(
            new Synonym(Set.of("패키지", "패키징", "package", "packaging"), List.of("패키지", "패키징", "packag")),
            new Synonym(Set.of("브랜딩", "브랜드", "branding", "brand"), List.of("브랜딩", "브랜드", "brand")),
            new Synonym(Set.of("상세페이지", "상세", "detail"), List.of("상세", "detail")),
            new Synonym(Set.of("로고", "logo"), List.of("로고", "logo")),
            new Synonym(Set.of("카페", "커피", "cafe", "café", "coffee"), List.of("카페", "커피", "cafe", "café", "coffee")),
            new Synonym(Set.of("라벨", "레이블", "label"), List.of("라벨", "레이블", "label")),
            new Synonym(Set.of("편집", "에디토리얼", "editorial"), List.of("편집", "에디토리얼", "editorial")),
            new Synonym(Set.of("웹사이트", "website"), List.of("웹사이트", "website")),
            new Synonym(Set.of("일러스트", "illustration"), List.of("일러스트", "illustrat")),
            new Synonym(Set.of("굿즈", "goods", "merch"), List.of("굿즈", "goods", "merch")));

    private final List<Synonym> synonyms;

    /**
     * 기본 동의어 표를 쓰는 규칙 생성
     */
    public PortfolioSearchTermResolver() {
        this(DEFAULT_SYNONYMS);
    }

    PortfolioSearchTermResolver(List<Synonym> synonyms) {
        this.synonyms = List.copyOf(synonyms);
    }

    /**
     * 질문에서 검색 표기 그룹 추출
     *
     * @param text 방문자 질문 원문 (null 허용)
     * @return 등장 순서의 검색 표기 그룹 0~6개, 검색어가 없으면 빈 목록
     */
    public List<SearchTermGroup> resolve(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = Normalizer.normalize(headCodePoints(text), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);

        Set<List<String>> groups = new LinkedHashSet<>();
        for (String token : SEPARATOR.split(normalized)) {
            List<String> forms = forms(token);
            if (forms.isEmpty()) {
                continue;
            }
            groups.add(expand(forms));
            if (groups.size() == MAX_GROUPS) {
                break;
            }
        }
        return groups.stream().map(SearchTermGroup::new).toList();
    }

    private static String headCodePoints(String text) {
        if (text.codePointCount(0, text.length()) <= MAX_INPUT_CODE_POINTS) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, MAX_INPUT_CODE_POINTS));
    }

    // 원형과 조사 제거형, 어느 쪽이든 불용 표현이면 토큰 전체를 버림
    private static List<String> forms(String token) {
        if (codePoints(token) < MIN_TERM_CODE_POINTS || PREDICATE_ENDINGS.stream().anyMatch(token::endsWith)) {
            return List.of();
        }
        List<String> forms = new ArrayList<>(2);
        forms.add(token);
        String stripped = stripParticle(token);
        if (stripped != null) {
            forms.add(stripped);
        }
        return forms.stream().anyMatch(STOPWORDS::contains) ? List.of() : forms;
    }

    private static String stripParticle(String token) {
        for (String particle : PARTICLES) {
            if (token.endsWith(particle)) {
                String stem = token.substring(0, token.length() - particle.length());
                return codePoints(stem) >= MIN_TERM_CODE_POINTS ? stem : null;
            }
        }
        return null;
    }

    private List<String> expand(List<String> forms) {
        for (Synonym synonym : synonyms) {
            if (forms.stream().anyMatch(synonym.inputs()::contains)) {
                return synonym.variants();
            }
        }
        return forms;
    }

    private static int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }

    /**
     * 동의어 그룹
     *
     * @param inputs   토큰과 정확히 비교할 입력 표기
     * @param variants 부분 일치에 쓸 검색 표기 (최대 5개)
     */
    record Synonym(Set<String> inputs, List<String> variants) {
    }
}
