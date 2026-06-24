package kr.douid.brand.category.domain;

import kr.douid.brand.shared.exception.DomainException;

/**
 * 카테고리 slug가 이미 사용 중일 때 발생하는 예외
 */
public class CategorySlugDuplicateException extends DomainException {

    /**
     * 기본 메시지로 예외 생성
     */
    public CategorySlugDuplicateException() {
        super(CategoryErrorCode.SLUG_DUPLICATE.getType(),
                CategoryErrorCode.SLUG_DUPLICATE.getCode(),
                CategoryErrorCode.SLUG_DUPLICATE.getDefaultMessage());
    }
}
