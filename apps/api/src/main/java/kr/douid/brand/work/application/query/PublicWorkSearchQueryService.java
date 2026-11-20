package kr.douid.brand.work.application.query;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.work.application.port.PublicWorkSearch;
import kr.douid.brand.work.application.port.PublicWorkSearchItem;
import lombok.RequiredArgsConstructor;

/**
 * 공개 작업물 자연어 검색 query service
 *
 * 질의어 추출, 결과 개수 상한, 출력 텍스트 정리와 길이 상한을 이 계약 안에서 보장한다.
 * 검색어가 없으면 DB를 조회하지 않고, 조회 실패는 빈 목록으로 바꾸지 않는다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicWorkSearchQueryService implements PublicWorkSearch {

    // Phase 10 입력 예산 근거(research R10)라 설정으로 열지 않음
    private static final int MAX_TITLE_CODE_POINTS = 80;
    private static final int MAX_SUMMARY_CODE_POINTS = 120;
    private static final int MAX_CATEGORY_NAME_CODE_POINTS = 50;
    private static final Pattern CONTROL_OR_SPACE = Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Z}]+");

    private final PortfolioSearchTermResolver termResolver;
    private final PublicWorkSearchQueryRepository queryRepository;
    private final PublicWorkSearchPolicy policy;

    /**
     * 방문자 자연어 질문과 관련된 공개 작업물 검색
     *
     * @param text  방문자 질문 원문 (비신뢰, null·공백 허용)
     * @param limit 원하는 최대 결과 수 (서버 상한을 넘으면 상한으로 줄어듦)
     * @return 관련도 순 공개 작업물, 관련 작업물이나 검색어가 없으면 빈 목록
     * @throws IllegalArgumentException limit이 1보다 작을 때
     * @throws kr.douid.brand.work.application.port.PublicWorkSearchException 검색을 실행하지 못했을 때
     */
    @Override
    public List<PublicWorkSearchItem> search(String text, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Search limit must be positive");
        }
        List<SearchTermGroup> groups = termResolver.resolve(text);
        if (groups.isEmpty()) {
            return List.of();
        }
        return queryRepository.search(groups, Math.min(limit, policy.maxResults())).stream()
                .map(PublicWorkSearchQueryService::toItem)
                .toList();
    }

    private static PublicWorkSearchItem toItem(PublicWorkSearchRow row) {
        return new PublicWorkSearchItem(
                row.slug(),
                clean(row.title(), MAX_TITLE_CODE_POINTS),
                clean(row.summary(), MAX_SUMMARY_CODE_POINTS),
                clean(row.categoryName(), MAX_CATEGORY_NAME_CODE_POINTS),
                row.categorySlug());
    }

    private static String clean(String value, int maxCodePoints) {
        if (value == null) {
            return null;
        }
        String oneLine = CONTROL_OR_SPACE.matcher(value).replaceAll(" ").strip();
        if (oneLine.codePointCount(0, oneLine.length()) <= maxCodePoints) {
            return oneLine;
        }
        return oneLine.substring(0, oneLine.offsetByCodePoints(0, maxCodePoints)).strip();
    }
}
