package kr.douid.brand.shared.pagination;

import java.util.List;

import lombok.Getter;

/**
 * cursor(keyset) 기반 페이지네이션 결과를 API 응답 형식으로 감싸는 제네릭 래퍼
 */
@Getter
public class CursorPageResponse<T> {

    private final List<T> items;
    private final Long nextCursor;
    private final boolean hasNext;

    /**
     * cursor 페이지 응답 객체 생성
     *
     * @param items      페이지 내용
     * @param nextCursor 다음 페이지 조회에 사용할 cursor (다음 페이지 없으면 null)
     * @param hasNext    다음 페이지 존재 여부
     */
    public CursorPageResponse(List<T> items, Long nextCursor, boolean hasNext) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
