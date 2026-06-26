package kr.douid.brand.media.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 작업물에서 사용 중인 미디어를 삭제하려 할 때 발생하는 예외
 */
public class MediaInUseException extends DomainException {

    /**
     * 미디어 사용 중 예외 생성
     */
    public MediaInUseException() {
        super(DomainErrorType.CONFLICT,
                MediaErrorCode.MEDIA_IN_USE.getCode(),
                MediaErrorCode.MEDIA_IN_USE.getDefaultMessage());
    }
}
