package kr.douid.brand.category.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 카테고리를 찾을 수 없을 때 발생하는 예외
 */
public class CategoryNotFoundException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public CategoryNotFoundException() {
        super(CategoryErrorCode.NOT_FOUND.getType(),
                CategoryErrorCode.NOT_FOUND.getCode(),
                CategoryErrorCode.NOT_FOUND.getDefaultMessage());
    }
}
