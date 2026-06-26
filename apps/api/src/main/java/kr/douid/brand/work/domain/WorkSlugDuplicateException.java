package kr.douid.brand.work.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 작업물 슬러그가 중복될 때 발생하는 예외
 */
public class WorkSlugDuplicateException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public WorkSlugDuplicateException() {
        super(WorkErrorCode.SLUG_DUPLICATE.getType(),
                WorkErrorCode.SLUG_DUPLICATE.getCode(),
                WorkErrorCode.SLUG_DUPLICATE.getDefaultMessage());
    }
}
