package kr.douid.brand.media.infrastructure.port;

import java.util.List;

import org.springframework.stereotype.Component;

import kr.douid.brand.media.application.port.MediaUsageValidator;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.media.infrastructure.persistence.MediaJpaRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link MediaUsageValidator} port의 JPA 구현체
 *
 * Media에 별도 status 필드가 없으므로 존재 여부로 사용 가능 여부를 판단한다.
 */
@Component
@RequiredArgsConstructor
public class JpaMediaUsageValidatorAdapter implements MediaUsageValidator {

    private final MediaJpaRepository mediaJpaRepository;

    /**
     * 미디어 ID 목록이 모두 존재하는지 검증
     *
     * @param mediaIds 검증할 미디어 ID 목록
     * @throws MediaNotFoundException 존재하지 않는 미디어가 포함된 경우
     */
    @Override
    public void validateUsable(List<Long> mediaIds) {
        List<Long> foundIds = mediaJpaRepository.findAllById(mediaIds)
                .stream()
                .map(media -> media.getId())
                .toList();

        boolean hasInvalid = mediaIds.stream().anyMatch(id -> !foundIds.contains(id));
        if (hasInvalid) {
            throw new MediaNotFoundException();
        }
    }
}
