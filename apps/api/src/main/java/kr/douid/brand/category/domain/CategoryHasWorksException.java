package kr.douid.brand.category.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 작업물이 연결된 카테고리를 삭제하려 할 때 발생하는 예외
 */
public class CategoryHasWorksException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public CategoryHasWorksException() {
        super(CategoryErrorCode.HAS_WORKS.getType(),
                CategoryErrorCode.HAS_WORKS.getCode(),
                CategoryErrorCode.HAS_WORKS.getDefaultMessage());
    }
}
