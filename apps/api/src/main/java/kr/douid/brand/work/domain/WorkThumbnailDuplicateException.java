package kr.douid.brand.work.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 대표 이미지(THUMBNAIL) 역할이 중복될 때 발생하는 예외
 */
public class WorkThumbnailDuplicateException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public WorkThumbnailDuplicateException() {
        super(WorkErrorCode.THUMBNAIL_DUPLICATE.getType(),
                WorkErrorCode.THUMBNAIL_DUPLICATE.getCode(),
                WorkErrorCode.THUMBNAIL_DUPLICATE.getDefaultMessage());
    }
}
