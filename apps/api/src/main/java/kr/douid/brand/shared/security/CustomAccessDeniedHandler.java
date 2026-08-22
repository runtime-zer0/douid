package kr.douid.brand.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.shared.exception.ErrorCode;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.response.ErrorResponse;

/**
 * 인가 실패 요청에 대한 403 JSON 응답 작성
 */
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handler 생성
     *
     * @param objectMapper 403 응답 직렬화에 사용할 Jackson ObjectMapper
     */
    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 인가 실패 요청의 403 JSON 응답 작성
     *
     * @param request               HTTP 요청
     * @param response              HTTP 응답
     * @param accessDeniedException 인가 실패 예외
     * @throws IOException 응답 쓰기 실패 시
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<ErrorResponse> body = ApiResponse.failure(ErrorResponse.of(
                ErrorCode.FORBIDDEN.getCode(),
                ErrorCode.FORBIDDEN.getDefaultMessage()));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
