package kr.douid.brand.work.presentation.response;

import kr.douid.brand.work.application.command.WorkResult;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 작업물 공개 여부 변경 응답 DTO
 *
 * @param id         작업물 ID
 * @param visibility 변경된 공개 여부
 */
public record WorkVisibilityResponse(Long id, WorkVisibility visibility) {

    /**
     * application 결과를 응답으로 변환
     *
     * @param result 작업물 결과
     * @return 공개 여부 응답
     */
    public static WorkVisibilityResponse from(WorkResult result) {
        return new WorkVisibilityResponse(result.id(), result.visibility());
    }
}
