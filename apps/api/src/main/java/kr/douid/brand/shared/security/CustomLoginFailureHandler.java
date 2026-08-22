package kr.douid.brand.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.auth.domain.AuthErrorCode;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.response.ErrorResponse;

/**
 * 관리자 로그인 실패 시 401 JSON 응답을 작성
 *
 * 계정 미존재, 비밀번호 불일치, 비활성 계정을 모두 동일한 응답으로 취급한다
 */
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handler 생성
     *
     * @param objectMapper 401 응답 직렬화에 사용할 Jackson ObjectMapper
     */
    public CustomLoginFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 로그인 실패 원인과 무관하게 동일한 401 JSON 응답 작성
     *
     * @param request   HTTP 요청
     * @param response  HTTP 응답
     * @param exception 인증 실패 예외
     * @throws IOException 응답 쓰기 실패 시
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<ErrorResponse> body = ApiResponse.failure(ErrorResponse.of(
                AuthErrorCode.AUTHENTICATION_FAILED.getCode(),
                AuthErrorCode.AUTHENTICATION_FAILED.getDefaultMessage()));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
