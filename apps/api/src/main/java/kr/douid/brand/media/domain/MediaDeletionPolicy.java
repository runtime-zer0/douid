package kr.douid.brand.media.domain;

/**
 * 미디어 삭제 정책
 *
 * 미디어 삭제 전 도메인 제약 검증
 * Work 단계에서 WorkLinkedMediaDeletionPolicy로 사용 중 미디어 삭제 제한 예정
 */
public interface MediaDeletionPolicy {

    /**
     * 주어진 미디어가 삭제 가능한지 검증
     *
     * @param media 삭제 대상 미디어
     */
    void validate(Media media);
}
