package kr.douid.brand.media.application.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaDeletionPolicy;
import kr.douid.brand.media.domain.MediaNotFoundException;
import kr.douid.brand.media.domain.MediaRepository;
import lombok.RequiredArgsConstructor;

/**
 * 미디어 삭제 유스케이스를 처리하는 서비스
 *
 * 삭제 정책 검증 → 파일 삭제 → 메타데이터 삭제 순서
 * 파일 삭제 실패 시 로그 기록 후 메타데이터 삭제 계속 진행
 * S3 전환 시 보상 트랜잭션 전략 별도 검토 대상
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MediaDeleteCommandService {

    private static final Logger log = LoggerFactory.getLogger(MediaDeleteCommandService.class);

    private final MediaRepository mediaRepository;
    private final FileStoragePort fileStoragePort;
    private final List<MediaDeletionPolicy> deletionPolicies;

    /**
     * 미디어를 삭제
     *
     * @param id 삭제할 미디어 식별자
     * @throws MediaNotFoundException 미디어가 존재하지 않는 경우
     */
    public void delete(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(MediaNotFoundException::new);

        deletionPolicies.forEach(policy -> policy.validate(media));

        try {
            fileStoragePort.delete(media.getFilePath());
        } catch (Exception e) {
            log.warn("미디어 파일 삭제 실패 (id={}, path={}): {}", id, media.getFilePath(), e.getMessage());
        }

        mediaRepository.delete(media);
    }
}
