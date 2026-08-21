package kr.douid.brand.auth.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.douid.brand.auth.application.AdminResult;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.auth.presentation.request.LoginRequest;
import kr.douid.brand.auth.presentation.response.AdminResponse;
import kr.douid.brand.auth.presentation.response.CsrfTokenResponse;
import kr.douid.brand.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    /**
     * 관리자 로그인 요청을 처리
     *
     * 인증 성공 시 Session Fixation 방어를 위해 세션 ID를 교체하고 인증 정보를 세션에 저장한다
     *
     * @param request 로그인 요청값
     * @param httpRequest Session Fixation 방어에 사용할 HTTP 요청
     * @param httpResponse 인증 정보를 세션에 저장하기 위한 HTTP 응답
     * @return 인증된 관리자 최소 정보
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AdminResult result = authenticationService.authenticate(request.toCommand());

        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);

        return ResponseEntity.ok(ApiResponse.success(AdminResponse.from(result)));
    }

    /**
     * 현재 인증된 관리자 정보를 조회
     *
     * @return 현재 관리자 최소 정보
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminResponse>> me() {
        AdminResult result = authenticationService.getCurrentAdmin();

        return ResponseEntity.ok(ApiResponse.success(AdminResponse.from(result)));
    }

    /**
     * CSRF 토큰을 발급
     *
     * Frontend가 상태 변경 요청 전 호출해 토큰을 확보하는 용도
     * 컨트롤러 메서드 인자로 {@link CsrfToken}을 직접 받아야 지연 로딩된 토큰이 실제로 응답 쿠키에 반영된다
     *
     * @param csrfToken 현재 요청의 CSRF 토큰
     * @return CSRF 헤더 이름과 토큰 값
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<ApiResponse<CsrfTokenResponse>> csrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok(ApiResponse.success(
                new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken())));
    }

    /**
     * 관리자 로그아웃 요청을 처리
     *
     * 현재 Session을 무효화하고 인증 컨텍스트를 제거한다
     *
     * @param httpRequest Session 무효화에 사용할 HTTP 요청
     * @param httpResponse 로그아웃 처리에 사용할 HTTP 응답
     * @return 빈 성공 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        logoutHandler.logout(httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());

        return ResponseEntity.ok(ApiResponse.success());
    }
}
