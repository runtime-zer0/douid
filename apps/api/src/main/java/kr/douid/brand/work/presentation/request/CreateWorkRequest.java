package kr.douid.brand.work.presentation.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.douid.brand.work.application.command.CreateWorkCommand;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 작업물 생성 요청 DTO
 *
 * @param title       작업물 제목
 * @param slug        작업물 슬러그
 * @param summary     요약 설명
 * @param description 상세 설명
 * @param categoryId  카테고리 ID (없으면 null)
 * @param visibility  공개 여부
 * @param mediaItems  연결할 미디어 항목 목록
 */
public record CreateWorkRequest(
        @NotBlank String title,
        @NotBlank String slug,
        String summary,
        String description,
        Long categoryId,
        @NotNull WorkVisibility visibility,
        @Valid List<WorkMediaItemRequest> mediaItems
) {
    public CreateWorkRequest {
        visibility = visibility == null ? WorkVisibility.HIDDEN : visibility;
        mediaItems = mediaItems == null ? List.of() : mediaItems;
    }

    /**
     * 요청값을 작업물 생성 command로 변환
     *
     * @return CreateWorkCommand
     */
    public CreateWorkCommand toCommand() {
        return new CreateWorkCommand(
                title,
                slug,
                summary,
                description,
                categoryId,
                visibility,
                mediaItems.stream().map(WorkMediaItemRequest::toCommand).toList()
        );
    }
}
