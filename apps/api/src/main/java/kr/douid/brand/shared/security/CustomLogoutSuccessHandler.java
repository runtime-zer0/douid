package kr.douid.brand.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.shared.response.ApiResponse;

/**
 * 관리자 로그아웃 성공 시 200 JSON 응답을 작성
 */
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handler 생성
     *
     * @param objectMapper 200 응답 직렬화에 사용할 Jackson ObjectMapper
     */
    public CustomLogoutSuccessHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 로그아웃 성공 200 JSON 응답 작성
     *
     * @param request        HTTP 요청
     * @param response       HTTP 응답
     * @param authentication 로그아웃 처리된 인증 정보
     * @throws IOException 응답 쓰기 실패 시
     */
    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.success();
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
