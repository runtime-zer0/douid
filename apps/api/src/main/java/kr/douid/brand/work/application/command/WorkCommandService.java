package kr.douid.brand.work.application.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.category.application.port.CategoryExistenceChecker;
import kr.douid.brand.category.domain.CategoryNotFoundException;
import kr.douid.brand.media.application.port.MediaUsageValidator;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkMediaItem;
import kr.douid.brand.work.domain.WorkNotFoundException;
import kr.douid.brand.work.domain.WorkRepository;
import kr.douid.brand.work.domain.WorkSlugDuplicateException;
import lombok.RequiredArgsConstructor;

/**
 * 작업물 Command 흐름을 담당하는 서비스
 *
 * 생성, 수정, 삭제, 공개 여부 변경 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkCommandService {

    private final WorkRepository workRepository;
    private final CategoryExistenceChecker categoryExistenceChecker;
    private final MediaUsageValidator mediaUsageValidator;

    /**
     * 새 작업물을 생성
     *
     * @param command 작업물 생성 입력값
     * @return 생성된 작업물 결과
     * @throws WorkSlugDuplicateException  슬러그가 이미 존재하는 경우
     * @throws CategoryNotFoundException   존재하지 않는 카테고리 ID인 경우
     */
    public WorkResult create(CreateWorkCommand command) {
        validateSlugOnCreate(command.slug());
        validateCategoryIfPresent(command.categoryId());
        validateMediaIfPresent(command.mediaItems());

        Work work = Work.create(
                command.title(),
                command.slug(),
                command.summary(),
                command.description(),
                command.categoryId(),
                command.visibility()
        );
        work.replaceMediaItems(toMediaItems(command.mediaItems()));

        workRepository.save(work);
        workRepository.flush();
        return WorkResult.from(work);
    }

    /**
     * 기존 작업물을 수정
     *
     * @param command 작업물 수정 입력값
     * @return 수정된 작업물 결과
     * @throws WorkNotFoundException        작업물을 찾을 수 없는 경우
     * @throws WorkSlugDuplicateException   슬러그가 다른 작업물과 중복되는 경우
     * @throws CategoryNotFoundException    존재하지 않는 카테고리 ID인 경우
     */
    public WorkResult update(UpdateWorkCommand command) {
        Work work = workRepository.findById(command.id())
                .orElseThrow(WorkNotFoundException::new);
        validateSlugOnUpdate(command.slug(), command.id());
        validateCategoryIfPresent(command.categoryId());
        validateMediaIfPresent(command.mediaItems());

        work.updateBasicInfo(
                command.title(),
                command.slug(),
                command.summary(),
                command.description(),
                command.categoryId()
        );
        work.changeVisibility(command.visibility());
        work.replaceMediaItems(toMediaItems(command.mediaItems()));

        workRepository.flush();
        return WorkResult.from(work);
    }

    /**
     * 작업물을 삭제
     *
     * WorkMedia 연결 정보는 orphanRemoval로 함께 제거된다. Media 파일 자산은 삭제되지 않는다.
     *
     * @param id 삭제할 작업물 ID
     * @throws WorkNotFoundException 작업물을 찾을 수 없는 경우
     */
    public void delete(Long id) {
        Work work = workRepository.findById(id)
                .orElseThrow(WorkNotFoundException::new);
        workRepository.delete(work);
    }

    /**
     * 작업물 공개 여부를 변경
     *
     * @param command 공개 여부 변경 입력값
     * @return 변경된 작업물 결과
     * @throws WorkNotFoundException 작업물을 찾을 수 없는 경우
     */
    public WorkResult changeVisibility(ChangeVisibilityCommand command) {
        Work work = workRepository.findById(command.id())
                .orElseThrow(WorkNotFoundException::new);
        work.changeVisibility(command.visibility());
        return WorkResult.from(work);
    }

    private void validateSlugOnCreate(String slug) {
        if (workRepository.existsBySlug(slug)) {
            throw new WorkSlugDuplicateException();
        }
    }

    private void validateSlugOnUpdate(String slug, Long id) {
        if (workRepository.existsBySlugAndIdNot(slug, id)) {
            throw new WorkSlugDuplicateException();
        }
    }

    private void validateCategoryIfPresent(Long categoryId) {
        if (categoryId != null && !categoryExistenceChecker.existsById(categoryId)) {
            throw new CategoryNotFoundException();
        }
    }

    private void validateMediaIfPresent(List<WorkMediaItemCommand> mediaItems) {
        if (mediaItems == null || mediaItems.isEmpty()) {
            return;
        }
        List<Long> mediaIds = mediaItems.stream().map(WorkMediaItemCommand::mediaId).toList();
        mediaUsageValidator.validateUsable(mediaIds);
    }

    private List<WorkMediaItem> toMediaItems(List<WorkMediaItemCommand> commands) {
        if (commands == null) {
            return List.of();
        }
        return commands.stream().map(WorkMediaItemCommand::toMediaItem).toList();
    }
}
