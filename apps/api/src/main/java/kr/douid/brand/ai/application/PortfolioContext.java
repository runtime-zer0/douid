package kr.douid.brand.ai.application;

import java.util.List;
import java.util.Objects;

/**
 * 한 요청 안에서만 쓰는 서버 소유 공개 작업물 맥락
 *
 * @param state 검색 상태
 * @param block instructions 끝에 붙일 블록, NO_TERMS면 빈 문자열
 * @param portfolioIds 추천 이벤트로 보낼 검색 결과 slug, FOUND가 아니면 빈 목록
 */
public record PortfolioContext(State state, String block, List<String> portfolioIds) {

    public enum State { NO_TERMS, EMPTY, FOUND, FAILED }

    public PortfolioContext {
        Objects.requireNonNull(state);
        Objects.requireNonNull(block);
        portfolioIds = List.copyOf(portfolioIds);
    }

    /**
     * 검색어가 없어 블록을 넣지 않는 맥락
     *
     * @return 빈 블록과 빈 추천 목록
     */
    public static PortfolioContext noTerms() {
        return new PortfolioContext(State.NO_TERMS, "", List.of());
    }
}
