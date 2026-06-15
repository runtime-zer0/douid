package kr.douid.brand.shared.pagination;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final boolean hasNext;

    private PageResponse(List<T> content, int page, int size, long totalElements, boolean hasNext) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        boolean hasNext = (long) (page.getNumber() + 1) * page.getSize() < page.getTotalElements();
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                hasNext
        );
    }
}
