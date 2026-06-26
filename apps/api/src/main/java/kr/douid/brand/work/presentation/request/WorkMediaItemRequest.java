package kr.douid.brand.work.presentation.request;

import jakarta.validation.constraints.NotNull;
import kr.douid.brand.work.application.command.WorkMediaItemCommand;
import kr.douid.brand.work.domain.WorkMediaRole;

/**
 * 미디어 항목 요청 DTO
 *
 * @param mediaId   미디어 ID
 * @param role      Work에서의 미디어 역할
 * @param sortOrder 정렬 순서
 * @param altText   대체 텍스트
 */
public record WorkMediaItemRequest(
        @NotNull Long mediaId,
        @NotNull WorkMediaRole role,
        int sortOrder,
        String altText
) {
    /**
     * 요청값을 command DTO로 변환
     *
     * @return WorkMediaItemCommand
     */
    public WorkMediaItemCommand toCommand() {
        return new WorkMediaItemCommand(mediaId, role, sortOrder, altText);
    }
}
