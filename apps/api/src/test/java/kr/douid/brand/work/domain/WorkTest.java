package kr.douid.brand.work.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class WorkTest {

    @Test
    void create_작업물_생성() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE);

        assertThat(work.getTitle()).isEqualTo("브랜드 리뉴얼");
        assertThat(work.getSlug()).isEqualTo("brand-renewal");
        assertThat(work.getSummary()).isEqualTo("요약");
        assertThat(work.getDescription()).isEqualTo("상세");
        assertThat(work.getCategoryId()).isEqualTo(1L);
        assertThat(work.getVisibility()).isEqualTo(WorkVisibility.VISIBLE);
        assertThat(work.getMediaItems()).isEmpty();
    }

    @Test
    void updateBasicInfo_기본정보_변경() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.HIDDEN);

        work.updateBasicInfo("UX 개선", "ux-improvement", "새 요약", "새 상세", 2L);

        assertThat(work.getTitle()).isEqualTo("UX 개선");
        assertThat(work.getSlug()).isEqualTo("ux-improvement");
        assertThat(work.getSummary()).isEqualTo("새 요약");
        assertThat(work.getDescription()).isEqualTo("새 상세");
        assertThat(work.getCategoryId()).isEqualTo(2L);
    }

    @Test
    void changeVisibility_공개여부_변경() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.HIDDEN);

        work.changeVisibility(WorkVisibility.VISIBLE);

        assertThat(work.getVisibility()).isEqualTo(WorkVisibility.VISIBLE);
    }

    @Test
    void replaceMediaItems_미디어_목록_교체() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE);

        work.replaceMediaItems(List.of(
                new WorkMediaItem(10L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지"),
                new WorkMediaItem(11L, WorkMediaRole.DETAIL_IMAGE, 1, "상세 이미지")
        ));

        assertThat(work.getMediaItems()).hasSize(2);
        assertThat(work.getMediaItems().get(0).getMediaId()).isEqualTo(10L);
        assertThat(work.getMediaItems().get(0).getRole()).isEqualTo(WorkMediaRole.THUMBNAIL);
        assertThat(work.getMediaItems().get(1).getMediaId()).isEqualTo(11L);
        assertThat(work.getMediaItems().get(1).getRole()).isEqualTo(WorkMediaRole.DETAIL_IMAGE);
    }

    @Test
    void replaceMediaItems_대표이미지_중복_예외() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE);

        assertThatThrownBy(() -> work.replaceMediaItems(List.of(
                new WorkMediaItem(10L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지"),
                new WorkMediaItem(11L, WorkMediaRole.THUMBNAIL, 1, "다른 대표 이미지")
        ))).isInstanceOf(WorkThumbnailDuplicateException.class);
    }
}
