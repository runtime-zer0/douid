package kr.douid.brand.media.application.port;

import java.util.List;

/**
 * 미디어 사용 가능 여부 검증 port
 *
 * 다른 feature는 Media 저장 구현을 알지 않고 이 contract로 사용 가능 여부만 검증한다.
 */
public interface MediaUsageValidator {

    /**
     * 미디어 ID 목록이 모두 존재하고 사용 가능한 상태인지 검증
     *
     * @param mediaIds 검증할 미디어 ID 목록
     * @throws kr.douid.brand.media.domain.MediaNotFoundException 존재하지 않는 미디어가 포함된 경우
     */
    void validateUsable(List<Long> mediaIds);
}
