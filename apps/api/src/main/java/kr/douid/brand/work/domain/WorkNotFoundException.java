package kr.douid.brand.work.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 작업물을 찾을 수 없을 때 발생하는 예외
 */
public class WorkNotFoundException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public WorkNotFoundException() {
        super(WorkErrorCode.NOT_FOUND.getType(),
                WorkErrorCode.NOT_FOUND.getCode(),
                WorkErrorCode.NOT_FOUND.getDefaultMessage());
    }
}
