package kr.douid.brand.work.application.query;

import java.util.List;

/**
 * 같은 개념의 검색 표기 묶음
 *
 * 검색 adapter는 그룹 안 표기를 OR로 묶고, 필드 가중치를 그룹마다 한 번만 더한다.
 *
 * @param variants 소문자·NFC 검색 표기 1~5개
 */
public record SearchTermGroup(List<String> variants) {

    static final int MAX_VARIANTS = 5;

    /**
     * 표기 개수와 null 검증 후 불변 복사
     *
     * @param variants 검색 표기 목록
     * @throws IllegalArgumentException 표기가 없거나 5개를 넘을 때
     * @throws NullPointerException     목록이나 표기가 null일 때
     */
    public SearchTermGroup {
        variants = List.copyOf(variants);
        if (variants.isEmpty() || variants.size() > MAX_VARIANTS) {
            throw new IllegalArgumentException("Search term group must have 1 to 5 variants");
        }
    }
}
