package kr.douid.brand.work.application.command;

import kr.douid.brand.work.domain.WorkMediaItem;
import kr.douid.brand.work.domain.WorkMediaRole;

/**
 * WorkMedia 항목을 표현하는 command DTO
 *
 * @param mediaId   미디어 ID
 * @param role      Work에서의 미디어 역할
 * @param sortOrder 정렬 순서
 * @param altText   대체 텍스트
 */
public record WorkMediaItemCommand(Long mediaId, WorkMediaRole role, int sortOrder, String altText) {

    /**
     * 도메인 값 객체로 변환
     *
     * @return WorkMediaItem
     */
    public WorkMediaItem toMediaItem() {
        return new WorkMediaItem(mediaId, role, sortOrder, altText);
    }
}
