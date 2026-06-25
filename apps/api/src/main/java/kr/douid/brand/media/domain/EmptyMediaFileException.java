package kr.douid.brand.media.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 빈 파일을 업로드하려 할 때 발생하는 예외
 */
public class EmptyMediaFileException extends DomainException {

    /**
     * 빈 파일 예외 생성
     */
    public EmptyMediaFileException() {
        super(DomainErrorType.BAD_REQUEST,
                MediaErrorCode.EMPTY_FILE.getCode(),
                MediaErrorCode.EMPTY_FILE.getDefaultMessage());
    }
}
