package kr.douid.brand.category.domain;

import kr.douid.brand.shared.exception.DomainErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 카테고리 도메인 오류 코드
 */
@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode {

    NOT_FOUND(DomainErrorType.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),
    SLUG_DUPLICATE(DomainErrorType.CONFLICT, "CATEGORY_SLUG_DUPLICATE", "이미 사용 중인 슬러그입니다."),
    HAS_WORKS(DomainErrorType.CONFLICT, "CATEGORY_HAS_WORKS", "작업물이 연결된 카테고리는 삭제할 수 없습니다.");

    private final DomainErrorType type;
    private final String code;
    private final String defaultMessage;
}
