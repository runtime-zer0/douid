package kr.douid.brand.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.auth.application.AdminResult;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.auth.presentation.response.AdminResponse;
import kr.douid.brand.shared.response.ApiResponse;

/**
 * 관리자 로그인 성공 시 세션을 구성하고 200 JSON 응답을 작성
 *
 * Session Fixation 방어를 위해 세션 ID를 교체한 뒤 인증 정보를 세션에 저장한다
 */
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationService authenticationService;
    private final SecurityContextRepository securityContextRepository;
    private final ObjectMapper objectMapper;

    /**
     * Handler 생성
     *
     * @param authenticationService     인증된 이메일로 관리자 정보를 조회할 서비스
     * @param securityContextRepository 인증 정보를 세션에 저장할 저장소
     * @param objectMapper              200 응답 직렬화에 사용할 Jackson ObjectMapper
     */
    public CustomLoginSuccessHandler(
            AuthenticationService authenticationService,
            SecurityContextRepository securityContextRepository,
            ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.securityContextRepository = securityContextRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 세션 고정 방어 후 인증 정보를 저장하고 관리자 정보를 200 JSON으로 응답
     *
     * @param request        HTTP 요청
     * @param response       HTTP 응답
     * @param authentication 인증 결과
     * @throws IOException 응답 쓰기 실패 시
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);

        AdminResult result = authenticationService.resolve(authentication.getName());

        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<AdminResponse> body = ApiResponse.success(AdminResponse.from(result));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
