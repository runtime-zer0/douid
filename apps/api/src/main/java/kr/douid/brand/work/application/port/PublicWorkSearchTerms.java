package kr.douid.brand.work.application.port;

/**
 * 방문자 질문에 검색어가 남는지 확인하는 work feature 계약
 *
 * 검색어 없음과 결과 없음을 구분해야 하는 호출자를 위해 {@link PublicWorkSearch}와 같은 추출 규칙을 쓴다.
 */
public interface PublicWorkSearchTerms {

    /**
     * 검색어 존재 여부 확인
     *
     * @param text 방문자 질문 원문 (비신뢰, null·공백 허용)
     * @return 검색어가 하나 이상이면 true, DB를 조회하지 않음
     */
    boolean hasSearchTerms(String text);
}
