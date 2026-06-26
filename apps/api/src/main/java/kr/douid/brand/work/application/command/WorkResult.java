package kr.douid.brand.work.application.command;

import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * 작업물 command 결과
 *
 * @param id         작업물 ID
 * @param visibility 공개 여부
 */
public record WorkResult(Long id, WorkVisibility visibility) {

    /**
     * Work 엔티티로부터 결과를 생성
     *
     * @param work 저장된 작업물
     * @return 작업물 결과
     */
    public static WorkResult from(Work work) {
        return new WorkResult(work.getId(), work.getVisibility());
    }
}
