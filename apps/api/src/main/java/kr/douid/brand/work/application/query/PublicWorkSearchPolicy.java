package kr.douid.brand.work.application.query;

/**
 * 공개 작업물 검색 결과 개수 정책
 *
 * @param maxResults 한 번에 돌려주는 최대 결과 수 (1~9)
 */
public record PublicWorkSearchPolicy(int maxResults) {

    /**
     * 결과 상한 범위 검증
     *
     * @param maxResults 최대 결과 수
     * @throws IllegalArgumentException 1~9 범위 밖일 때
     */
    public PublicWorkSearchPolicy {
        if (maxResults < 1 || maxResults > 9) {
            throw new IllegalArgumentException("Work search max results must be between 1 and 9");
        }
    }
}
