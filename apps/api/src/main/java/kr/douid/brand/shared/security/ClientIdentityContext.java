package kr.douid.brand.shared.security;

import java.util.Optional;

/**
 * 현재 요청의 신뢰된 상담 주체 ID를 보관하는 request-scoped holder
 *
 * Admin {@code SecurityContext}와 완전히 독립된 별도 인증 경계다.
 * {@link ClientCredentialFilter}만 값을 세팅하며, 이 필터 밖에서는 절대 값을 설정하지 않는다.
 */
public final class ClientIdentityContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private ClientIdentityContext() {
    }

    /**
     * 현재 요청의 상담 주체 ID를 설정
     *
     * @param clientIdentityId 인증된 상담 주체 ID
     */
    public static void set(Long clientIdentityId) {
        HOLDER.set(clientIdentityId);
    }

    /**
     * 현재 요청의 상담 주체 ID를 조회
     *
     * @return 인증된 상담 주체 ID (미인증이면 empty)
     */
    public static Optional<Long> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    /**
     * 현재 요청의 상담 주체 ID를 초기화
     *
     * 요청 처리가 끝난 뒤 스레드 재사용으로 인한 컨텍스트 누수를 막기 위해 반드시 호출해야 한다.
     */
    public static void clear() {
        HOLDER.remove();
    }
}
