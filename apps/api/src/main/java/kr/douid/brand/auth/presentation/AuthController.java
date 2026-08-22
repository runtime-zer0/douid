package kr.douid.brand.auth.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.douid.brand.auth.application.AdminResult;
import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.auth.presentation.response.AdminResponse;
import kr.douid.brand.auth.presentation.response.CsrfTokenResponse;
import kr.douid.brand.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

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
}
