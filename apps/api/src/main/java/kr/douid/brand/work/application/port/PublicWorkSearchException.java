package kr.douid.brand.work.application.port;

import kr.douid.brand.shared.exception.IntegrationErrorType;
import kr.douid.brand.shared.exception.IntegrationException;

/**
 * 공개 작업물 검색 실행 실패
 *
 * 빈 결과와 구분되는 유일한 실패 신호다. 메시지에 질의어나 SQL을 담지 않는다.
 */
public class PublicWorkSearchException extends IntegrationException {

    /**
     * 원인 예외를 보존한 검색 실패 예외 생성
     *
     * @param cause 원인 예외
     */
    public PublicWorkSearchException(Throwable cause) {
        super(IntegrationErrorType.INTERNAL_ERROR, "WORK_SEARCH_FAILED", "작업물 검색을 처리하지 못했습니다.", cause);
    }
}
