package kr.douid.brand.work.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.category.application.port.CategoryExistenceChecker;
import kr.douid.brand.category.domain.CategoryNotFoundException;
import kr.douid.brand.media.application.port.MediaUsageValidator;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.work.application.command.ChangeVisibilityCommand;
import kr.douid.brand.work.application.command.CreateWorkCommand;
import kr.douid.brand.work.application.command.UpdateWorkCommand;
import kr.douid.brand.work.application.command.WorkCommandService;
import kr.douid.brand.work.application.command.WorkMediaItemCommand;
import kr.douid.brand.work.application.command.WorkResult;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkNotFoundException;
import kr.douid.brand.work.domain.WorkRepository;
import kr.douid.brand.work.domain.WorkSlugDuplicateException;
import kr.douid.brand.work.domain.WorkThumbnailDuplicateException;
import kr.douid.brand.work.domain.WorkVisibility;

@ExtendWith(MockitoExtension.class)
class WorkCommandServiceTest {

    @Mock
    private WorkRepository workRepository;

    @Mock
    private CategoryExistenceChecker categoryExistenceChecker;

    @Mock
    private MediaUsageValidator mediaUsageValidator;

    private WorkCommandService workCommandService;

    @BeforeEach
    void setUp() {
        workCommandService = new WorkCommandService(
                workRepository, categoryExistenceChecker, mediaUsageValidator);
    }

    @Test
    void create_정상_생성() {
        CreateWorkCommand command = createCommand();
        given(workRepository.existsBySlug("brand-renewal")).willReturn(false);
        given(categoryExistenceChecker.existsById(1L)).willReturn(true);
        given(workRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        WorkResult result = workCommandService.create(command);

        assertThat(result.visibility()).isEqualTo(WorkVisibility.VISIBLE);
        ArgumentCaptor<Work> captor = ArgumentCaptor.forClass(Work.class);
        then(workRepository).should().save(captor.capture());
        Work saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("브랜드 리뉴얼");
        assertThat(saved.getSlug()).isEqualTo("brand-renewal");
        assertThat(saved.getCategoryId()).isEqualTo(1L);
        assertThat(saved.getMediaItems()).hasSize(2);
        then(mediaUsageValidator).should().validateUsable(List.of(10L, 11L));
        then(workRepository).should().flush();
    }

    @Test
    void create_슬러그_중복_예외() {
        CreateWorkCommand command = createCommand();
        given(workRepository.existsBySlug("brand-renewal")).willReturn(true);

        assertThatThrownBy(() -> workCommandService.create(command))
                .isInstanceOf(WorkSlugDuplicateException.class);

        then(categoryExistenceChecker).should(never()).existsById(anyLong());
        then(mediaUsageValidator).should(never()).validateUsable(any());
        then(workRepository).should(never()).save(any());
    }

    @Test
    void create_카테고리_미존재_예외() {
        CreateWorkCommand command = createCommand();
        given(workRepository.existsBySlug("brand-renewal")).willReturn(false);
        given(categoryExistenceChecker.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> workCommandService.create(command))
                .isInstanceOf(CategoryNotFoundException.class);

        then(mediaUsageValidator).should(never()).validateUsable(any());
        then(workRepository).should(never()).save(any());
    }

    @Test
    void create_미디어_미존재_예외() {
        CreateWorkCommand command = createCommand();
        given(workRepository.existsBySlug("brand-renewal")).willReturn(false);
        given(categoryExistenceChecker.existsById(1L)).willReturn(true);
        willThrow(new MediaNotFoundException()).given(mediaUsageValidator)
                .validateUsable(List.of(10L, 11L));

        assertThatThrownBy(() -> workCommandService.create(command))
                .isInstanceOf(MediaNotFoundException.class);

        then(workRepository).should(never()).save(any());
    }

    @Test
    void create_대표이미지_중복_예외() {
        CreateWorkCommand command = new CreateWorkCommand(
                "브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE,
                List.of(
                        new WorkMediaItemCommand(10L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지"),
                        new WorkMediaItemCommand(11L, WorkMediaRole.THUMBNAIL, 1, "다른 대표 이미지")
                ));
        given(workRepository.existsBySlug("brand-renewal")).willReturn(false);
        given(categoryExistenceChecker.existsById(1L)).willReturn(true);

        assertThatThrownBy(() -> workCommandService.create(command))
                .isInstanceOf(WorkThumbnailDuplicateException.class);

        then(workRepository).should(never()).save(any());
    }

    @Test
    void update_정상_수정() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.HIDDEN);
        UpdateWorkCommand command = updateCommand();
        given(workRepository.findById(1L)).willReturn(Optional.of(work));
        given(workRepository.existsBySlugAndIdNot("ux-improvement", 1L)).willReturn(false);
        given(categoryExistenceChecker.existsById(2L)).willReturn(true);

        WorkResult result = workCommandService.update(command);

        assertThat(result.visibility()).isEqualTo(WorkVisibility.VISIBLE);
        assertThat(work.getTitle()).isEqualTo("UX 개선");
        assertThat(work.getSlug()).isEqualTo("ux-improvement");
        assertThat(work.getCategoryId()).isEqualTo(2L);
        assertThat(work.getMediaItems()).hasSize(1);
        then(mediaUsageValidator).should().validateUsable(List.of(20L));
        then(workRepository).should().flush();
    }

    @Test
    void update_미존재_예외() {
        UpdateWorkCommand command = updateCommand();
        given(workRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workCommandService.update(command))
                .isInstanceOf(WorkNotFoundException.class);

        then(workRepository).should(never()).existsBySlugAndIdNot(any(), anyLong());
    }

    @Test
    void update_슬러그_중복_예외() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.HIDDEN);
        UpdateWorkCommand command = updateCommand();
        given(workRepository.findById(1L)).willReturn(Optional.of(work));
        given(workRepository.existsBySlugAndIdNot("ux-improvement", 1L)).willReturn(true);

        assertThatThrownBy(() -> workCommandService.update(command))
                .isInstanceOf(WorkSlugDuplicateException.class);

        then(categoryExistenceChecker).should(never()).existsById(anyLong());
        then(workRepository).should(never()).flush();
    }

    @Test
    void delete_정상_삭제() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.VISIBLE);
        given(workRepository.findById(1L)).willReturn(Optional.of(work));

        workCommandService.delete(1L);

        then(workRepository).should().delete(work);
    }

    @Test
    void delete_미존재_예외() {
        given(workRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workCommandService.delete(99L))
                .isInstanceOf(WorkNotFoundException.class);

        then(workRepository).should(never()).delete(any());
    }

    @Test
    void changeVisibility_정상_변경() {
        Work work = Work.create("브랜드 리뉴얼", "brand-renewal", "요약", "상세", 1L,
                WorkVisibility.HIDDEN);
        given(workRepository.findById(1L)).willReturn(Optional.of(work));

        WorkResult result = workCommandService.changeVisibility(
                new ChangeVisibilityCommand(1L, WorkVisibility.VISIBLE));

        assertThat(result.visibility()).isEqualTo(WorkVisibility.VISIBLE);
        assertThat(work.getVisibility()).isEqualTo(WorkVisibility.VISIBLE);
    }

    private CreateWorkCommand createCommand() {
        return new CreateWorkCommand(
                "브랜드 리뉴얼",
                "brand-renewal",
                "요약",
                "상세",
                1L,
                WorkVisibility.VISIBLE,
                List.of(
                        new WorkMediaItemCommand(10L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지"),
                        new WorkMediaItemCommand(11L, WorkMediaRole.DETAIL_IMAGE, 1, "상세 이미지")
                )
        );
    }

    private UpdateWorkCommand updateCommand() {
        return new UpdateWorkCommand(
                1L,
                "UX 개선",
                "ux-improvement",
                "새 요약",
                "새 상세",
                2L,
                WorkVisibility.VISIBLE,
                List.of(new WorkMediaItemCommand(20L, WorkMediaRole.THUMBNAIL, 0, "대표 이미지"))
        );
    }
}
