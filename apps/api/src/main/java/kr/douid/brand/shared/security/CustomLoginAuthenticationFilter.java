package kr.douid.brand.shared.security;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.auth.presentation.request.LoginRequest;

/**
 * JSON body 기반 로그인 요청을 처리하는 인증 필터
 *
 * request body를 {@link LoginRequest}로 역직렬화해 {@link AuthenticationManager}에 인증을 위임한다
 */
public class CustomLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper;

    /**
     * 필터 생성
     *
     * @param authenticationManager  인증 위임 대상
     * @param objectMapper           요청 body 역직렬화에 사용할 Jackson ObjectMapper
     * @param successHandler         인증 성공 처리 핸들러
     * @param failureHandler         인증 실패 처리 핸들러
     */
    public CustomLoginAuthenticationFilter(
            AuthenticationManager authenticationManager,
            ObjectMapper objectMapper,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler) {
        super(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/login"));
        this.objectMapper = objectMapper;
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
    }

    /**
     * JSON body를 파싱해 인증을 시도
     *
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @return {@link AuthenticationManager}가 반환한 인증 결과
     * @throws AuthenticationException 요청 body 형식이 올바르지 않거나 인증에 실패한 경우
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        LoginRequest loginRequest;
        try {
            loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException e) {
            throw new BadCredentialsException("요청 본문을 읽을 수 없습니다.", e);
        }

        UsernamePasswordAuthenticationToken authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.email(), loginRequest.password());

        return getAuthenticationManager().authenticate(authenticationRequest);
    }
}
