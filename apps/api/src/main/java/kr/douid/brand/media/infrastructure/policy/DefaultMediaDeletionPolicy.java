package kr.douid.brand.media.infrastructure.policy;

import org.springframework.stereotype.Component;

import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaDeletionPolicy;

/**
 * 기본 미디어 삭제 정책
 *
 * Work 기능이 없는 현재 단계에서는 추가 제약 없이 통과시킨다.
 * Work 단계에서 WorkLinkedMediaDeletionPolicy로 사용 중 미디어 삭제 제한 예정
 */
@Component
public class DefaultMediaDeletionPolicy implements MediaDeletionPolicy {

    /**
     * 주어진 미디어가 삭제 가능한지 검증
     *
     * @param media 삭제 대상 미디어
     */
    @Override
    public void validate(Media media) {
        // Work 단계에서 WorkLinkedMediaDeletionPolicy로 사용 중 미디어 삭제 제한 추가
    }
}
