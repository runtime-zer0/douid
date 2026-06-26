package kr.douid.brand.work.application.command;

import java.util.List;

import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 작업물 생성 command
 *
 * @param title       작업물 제목
 * @param slug        작업물 슬러그
 * @param summary     요약 설명
 * @param description 상세 설명
 * @param categoryId  카테고리 ID (없으면 null)
 * @param visibility  공개 여부
 * @param mediaItems  연결할 미디어 항목 목록
 */
public record CreateWorkCommand(
        String title,
        String slug,
        String summary,
        String description,
        Long categoryId,
        WorkVisibility visibility,
        List<WorkMediaItemCommand> mediaItems
) {
}
