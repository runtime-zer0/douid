package kr.douid.brand.shared.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.douid.brand.client.application.ClientAuthenticationService;
import lombok.RequiredArgsConstructor;

/**
 * client_token 쿠키를 검증해 {@link ClientIdentityContext}에 세팅하는 인증 경계
 *
 * Admin Session 인증({@code SecurityContext})과 완전히 독립된 별도 메커니즘이다.
 * 쿠키가 없거나 무효해도 요청을 차단하지 않고 다음 필터로 통과시킨다(익명 상담 시작 자체는
 * credential 없이도 가능해야 하므로, 인증 필요 여부 판단은 각 use case가 담당한다).
 */
@RequiredArgsConstructor
public class ClientCredentialFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "client_token";

    private final ClientAuthenticationService clientAuthenticationService;

    /**
     * 요청의 client_token 쿠키를 해석해 컨텍스트에 세팅한 뒤 다음 필터로 전달
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 다음 필터 체인
     * @throws ServletException 필터 체인 처리 중 서블릿 오류가 발생한 경우
     * @throws IOException      필터 체인 처리 중 입출력 오류가 발생한 경우
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            findRawToken(request)
                    .flatMap(clientAuthenticationService::resolve)
                    .ifPresent(ClientIdentityContext::set);
            filterChain.doFilter(request, response);
        } finally {
            ClientIdentityContext.clear();
        }
    }

    private Optional<String> findRawToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
