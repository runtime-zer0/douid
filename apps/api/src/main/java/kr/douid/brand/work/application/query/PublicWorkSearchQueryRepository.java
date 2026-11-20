package kr.douid.brand.work.application.query;

import java.util.List;

/**
 * 공개 작업물 검색 조회 port
 */
public interface PublicWorkSearchQueryRepository {

    /**
     * 검색 표기 그룹과 일치하는 공개 작업물을 관련도 순으로 조회
     *
     * 공개 조건은 조회 질의 안에서 적용한다. 동점은 최신 생성 순이다.
     *
     * @param groups 검색 표기 그룹 (1개 이상)
     * @param limit  최대 결과 수
     * @return 관련도 순 결과 행, 일치가 없으면 빈 목록
     * @throws kr.douid.brand.work.application.port.PublicWorkSearchException 조회를 실행하지 못했을 때
     */
    List<PublicWorkSearchRow> search(List<SearchTermGroup> groups, int limit);
}
