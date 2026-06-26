package kr.douid.brand.work.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 작업물 도메인 오류 코드
 */
@Getter
@RequiredArgsConstructor
public enum WorkErrorCode {

    NOT_FOUND(DomainErrorType.NOT_FOUND, "WORK_NOT_FOUND", "작업물을 찾을 수 없습니다."),
    SLUG_DUPLICATE(DomainErrorType.CONFLICT, "WORK_SLUG_DUPLICATE", "이미 사용 중인 슬러그입니다."),
    THUMBNAIL_DUPLICATE(DomainErrorType.BAD_REQUEST, "WORK_THUMBNAIL_DUPLICATE", "대표 이미지는 하나만 등록할 수 있습니다.");

    private final DomainErrorType type;
    private final String code;
    private final String defaultMessage;
}
