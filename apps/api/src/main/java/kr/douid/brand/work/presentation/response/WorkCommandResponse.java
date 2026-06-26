package kr.douid.brand.work.presentation.response;

import kr.douid.brand.work.application.command.WorkResult;

/**
 * 작업물 생성·수정 응답 DTO
 *
 * @param id 작업물 ID
 */
public record WorkCommandResponse(Long id) {

    /**
     * application 결과를 응답으로 변환
     *
     * @param result 작업물 결과
     * @return 작업물 응답
     */
    public static WorkCommandResponse from(WorkResult result) {
        return new WorkCommandResponse(result.id());
    }
}
