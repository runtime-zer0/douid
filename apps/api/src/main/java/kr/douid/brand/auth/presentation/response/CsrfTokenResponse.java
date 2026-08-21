package kr.douid.brand.auth.presentation.response;

/**
 * CSRF 토큰 발급 응답
 *
 * @param headerName 상태 변경 요청에 담아야 하는 헤더 이름
 * @param token      CSRF 토큰 값
 */
public record CsrfTokenResponse(String headerName, String token) {
}
