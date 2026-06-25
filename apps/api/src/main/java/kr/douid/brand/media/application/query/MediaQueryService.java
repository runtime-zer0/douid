package kr.douid.brand.media.application.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.media.application.port.FileStoragePort;
import kr.douid.brand.media.domain.MediaNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * 미디어 조회 유스케이스를 처리하는 서비스
 *
 * 단건 조회와 목록 페이지네이션 조회 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaQueryService {

    private final FileStoragePort fileStoragePort;
    private final MediaQueryRepository mediaQueryRepository;

    /**
     * 미디어 단건 조회
     *
     * @param id 미디어 식별자
     * @return 미디어 view
     * @throws MediaNotFoundException 미디어가 존재하지 않는 경우
     */
    public MediaView getMedia(Long id) {
        return mediaQueryRepository.findById(id)
                .orElseThrow(MediaNotFoundException::new);
    }

    /**
     * 미디어 목록 페이지네이션 조회
     *
     * @param pageable 페이지네이션 파라미터
     * @return 미디어 view 페이지
     */
    public Page<MediaView> getMediaList(Pageable pageable) {
        return mediaQueryRepository.findAll(pageable);
    }

    /**
     * 미디어 파일 스트림 조회
     *
     * @param id 미디어 식별자
     * @return 파일 스트리밍 결과
     * @throws MediaNotFoundException 미디어가 존재하지 않는 경우
     */
    public MediaFileResult getMediaFile(Long id) {
        MediaView view = getMedia(id);
        return new MediaFileResult(
                view.originalFilename(),
                view.contentType(),
                fileStoragePort.load(view.filePath()));
    }
}
