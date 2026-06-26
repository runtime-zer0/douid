package kr.douid.brand.work.application.command;

import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 작업물 공개 여부 변경 command
 *
 * @param id         변경할 작업물 ID
 * @param visibility 변경할 공개 여부
 */
public record ChangeVisibilityCommand(Long id, WorkVisibility visibility) {
}
