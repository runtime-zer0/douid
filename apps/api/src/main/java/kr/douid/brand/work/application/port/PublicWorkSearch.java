package kr.douid.brand.work.application.port;

import java.util.List;

/**
 * 방문자 자연어 질문으로 공개 작업물을 찾는 work feature 계약
 *
 * 다른 feature는 이 계약과 {@link PublicWorkSearchItem}에만 의존한다.
 */
public interface PublicWorkSearch {

    /**
     * 방문자 자연어 질문과 관련된 공개 작업물 검색
     *
     * @param text  방문자 질문 원문 (비신뢰, null·공백 허용)
     * @param limit 원하는 최대 결과 수 (서버 상한을 넘으면 상한으로 줄어듦)
     * @return 관련도 순 공개 작업물, 관련 작업물이나 검색어가 없으면 빈 목록
     * @throws IllegalArgumentException  limit이 1보다 작을 때
     * @throws PublicWorkSearchException 검색을 실행하지 못했을 때
     */
    List<PublicWorkSearchItem> search(String text, int limit);
}
