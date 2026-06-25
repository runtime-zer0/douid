package kr.douid.brand.media.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;

/**
 * 이미지 파일이 아닌 파일을 업로드하려 할 때 발생하는 예외
 */
public class InvalidMediaFileTypeException extends DomainException {

    /**
     * 유효하지 않은 파일 타입 예외 생성
     */
    public InvalidMediaFileTypeException() {
        super(DomainErrorType.BAD_REQUEST,
                MediaErrorCode.INVALID_FILE_TYPE.getCode(),
                MediaErrorCode.INVALID_FILE_TYPE.getDefaultMessage());
    }
}
