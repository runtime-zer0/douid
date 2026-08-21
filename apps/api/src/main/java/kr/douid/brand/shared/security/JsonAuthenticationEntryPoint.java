package kr.douid.brand.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.shared.exception.ErrorCode;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.response.ErrorResponse;

/**
 * 미인증 요청에 대한 401 JSON 응답 작성
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * EntryPoint 생성
     *
     * @param objectMapper 401 응답 직렬화에 사용할 Jackson ObjectMapper
     */
    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 미인증 요청의 401 JSON 응답 작성
     *
     * @param request       HTTP 요청
     * @param response      HTTP 응답
     * @param authException 인증 실패 예외
     * @throws IOException 응답 쓰기 실패 시
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<ErrorResponse> body = ApiResponse.failure(ErrorResponse.of(
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.UNAUTHORIZED.getDefaultMessage()));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
