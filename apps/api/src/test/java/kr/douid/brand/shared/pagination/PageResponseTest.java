package kr.douid.brand.shared.pagination;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void 중간_페이지는_hasNext_true() {
        // given: 총 50개, 페이지 크기 20, 현재 0페이지 → 다음 있음
        List<String> items = List.of("a", "b");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 20), 50);

        // when
        PageResponse<String> response = PageResponse.from(page);

        // then
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(50);
        assertThat(response.getContent()).hasSize(2);
    }

    @Test
    void 마지막_페이지는_hasNext_false() {
        // given: 총 10개, 페이지 크기 10, 현재 0페이지 → 다음 없음
        List<String> items = List.of("a");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 10), 10);

        // when
        PageResponse<String> response = PageResponse.from(page);

        // then
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    void 빈_목록은_hasNext_false() {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.isHasNext()).isFalse();
    }
}
