package kr.douid.brand.media.domain;

import java.util.Optional;

/**
 * 미디어 Aggregate 저장·복원을 위한 domain port
 */
public interface MediaRepository {

    /**
     * 미디어를 저장하고 반환
     *
     * @param media 저장할 미디어
     * @return 저장된 미디어
     */
    Media save(Media media);

    /**
     * 식별자로 미디어 조회
     *
     * @param id 미디어 식별자
     * @return 미디어 (없으면 empty)
     */
    Optional<Media> findById(Long id);

    /**
     * 미디어 삭제
     *
     * @param media 삭제할 미디어
     */
    void delete(Media media);
}
