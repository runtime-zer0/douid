package kr.douid.brand.media.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.domain.MediaRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link MediaRepository} domain port의 JPA 구현체
 *
 * {@link MediaJpaRepository} 위임 호출, JpaRepository 직접 주입 방지
 */
@Repository
@RequiredArgsConstructor
public class JpaMediaRepositoryAdapter implements MediaRepository {

    private final MediaJpaRepository mediaJpaRepository;

    /**
     * 미디어를 저장하고 반환
     *
     * @param media 저장할 미디어
     * @return 저장된 미디어
     */
    @Override
    public Media save(Media media) {
        return mediaJpaRepository.save(media);
    }

    /**
     * 식별자로 미디어 조회
     *
     * @param id 미디어 식별자
     * @return 미디어 (없으면 empty)
     */
    @Override
    public Optional<Media> findById(Long id) {
        return mediaJpaRepository.findById(id);
    }

    /**
     * 미디어 삭제
     *
     * @param media 삭제할 미디어
     */
    @Override
    public void delete(Media media) {
        mediaJpaRepository.delete(media);
    }
}
