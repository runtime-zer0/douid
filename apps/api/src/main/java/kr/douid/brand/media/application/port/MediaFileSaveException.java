package kr.douid.brand.media.application.port;

import kr.douid.brand.media.domain.MediaErrorCode;
import kr.douid.brand.shared.exception.IntegrationErrorType;
import kr.douid.brand.shared.exception.IntegrationException;

/**
 * 파일 저장소 연동 실패 예외
 *
 * 로컬 저장소 또는 S3 등 외부 저장소 호출 실패 시 사용
 */
public class MediaFileSaveException extends IntegrationException {

    /**
     * 파일 저장 실패 예외 생성
     */
    public MediaFileSaveException() {
        super(IntegrationErrorType.INTERNAL_ERROR,
                MediaErrorCode.MEDIA_SAVE_FAILED.getCode(),
                MediaErrorCode.MEDIA_SAVE_FAILED.getDefaultMessage());
    }

    /**
     * 원인 예외를 포함한 파일 저장 실패 예외 생성
     *
     * @param cause 원인 예외
     */
    public MediaFileSaveException(Throwable cause) {
        super(IntegrationErrorType.INTERNAL_ERROR,
                MediaErrorCode.MEDIA_SAVE_FAILED.getCode(),
                MediaErrorCode.MEDIA_SAVE_FAILED.getDefaultMessage(),
                cause);
    }
}
