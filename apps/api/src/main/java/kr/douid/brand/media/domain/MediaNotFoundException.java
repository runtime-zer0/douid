package kr.douid.brand.media.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 존재하지 않는 미디어를 조회하거나 삭제하려 할 때 발생하는 예외
 */
public class MediaNotFoundException extends DomainException {

    /**
     * 미디어 없음 예외 생성
     */
    public MediaNotFoundException() {
        super(DomainErrorType.NOT_FOUND,
                MediaErrorCode.MEDIA_NOT_FOUND.getCode(),
                MediaErrorCode.MEDIA_NOT_FOUND.getDefaultMessage());
    }
}
