package kr.douid.brand.shared.pagination;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

/**
 * 페이지네이션 결과를 API 응답 형식으로 감싸는 제네릭 래퍼
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final boolean hasNext;

    /**
     * 페이지 응답 객체 생성
     *
     * @param content 페이지 내용
     * @param page 현재 페이지 번호
     * @param size 페이지 크기
     * @param totalElements 전체 항목 수
     * @param hasNext 다음 페이지 존재 여부
     */
    private PageResponse(List<T> content, int page, int size, long totalElements, boolean hasNext) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    /**
     * Spring Data {@link Page}의 API 응답 변환
     *
     * 다음 페이지 여부는 {@code (currentPage + 1) * size < totalElements} 기준으로 계산
     * {@code page.hasNext()}는 슬라이스 기준이라 사용하지 않음
     *
     * @param <T>  페이지 항목 타입
     * @param page Spring Data Page 객체
     * @return 변환된 {@link PageResponse}
     */
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
