package kr.douid.brand.work.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import kr.douid.brand.work.application.port.PublicWorkSearchItem;

/**
 * 최대 크기 검색 결과를 Phase 10 맥락 형식으로 직렬화한 크기 측정 (research R10)
 */
class PublicWorkSearchBudgetTest {

    private static final int WORST_CASE_CODE_POINTS = 3_125;

    @Test
    void 최대_길이_결과_5건의_직렬화_크기는_예산_안이다() {
        PublicWorkSearchQueryRepository repository = mock(PublicWorkSearchQueryRepository.class);
        PublicWorkSearchRow longest = new PublicWorkSearchRow(
                "s".repeat(255), "가".repeat(255), "나".repeat(255), "다".repeat(100), "c".repeat(100));
        given(repository.search(any(), anyInt())).willReturn(Collections.nCopies(5, longest));
        PublicWorkSearchQueryService service = new PublicWorkSearchQueryService(
                new PortfolioSearchTermResolver(), repository, new PublicWorkSearchPolicy(5));

        List<PublicWorkSearchItem> items = service.search("한옥", 9);
        String context = items.stream()
                .map(item -> "- %s | %s | %s(%s) | %s".formatted(
                        item.slug(), item.title(), item.categoryName(), item.categorySlug(), item.summary()))
                .collect(Collectors.joining("\n"));
        int codePoints = context.codePointCount(0, context.length());
        int bytes = context.getBytes(StandardCharsets.UTF_8).length;

        System.out.printf("search context worst case: items=%d, codePoints=%d, utf8Bytes=%d%n",
                items.size(), codePoints, bytes);
        assertThat(items).hasSize(5);
        assertThat(codePoints).isLessThanOrEqualTo(WORST_CASE_CODE_POINTS);
    }
}
