package kr.douid.brand.work.presentation.request;

import jakarta.validation.constraints.NotNull;
import kr.douid.brand.work.application.command.ChangeVisibilityCommand;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 작업물 공개 여부 변경 요청 DTO
 *
 * @param visibility 변경할 공개 여부
 */
public record ChangeVisibilityRequest(@NotNull WorkVisibility visibility) {

    /**
     * 요청값을 command로 변환
     *
     * @param id 작업물 ID
     * @return ChangeVisibilityCommand
     */
    public ChangeVisibilityCommand toCommand(Long id) {
        return new ChangeVisibilityCommand(id, visibility);
    }
}
