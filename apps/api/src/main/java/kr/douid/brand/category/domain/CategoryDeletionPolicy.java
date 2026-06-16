package kr.douid.brand.category.domain;

public interface CategoryDeletionPolicy {

    void validate(Category category);
}
