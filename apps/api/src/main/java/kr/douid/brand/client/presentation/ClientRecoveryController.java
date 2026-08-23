package kr.douid.brand.client.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.douid.brand.client.application.RecoveryConfirmationService;
import kr.douid.brand.client.application.RecoveryRequestService;
import kr.douid.brand.client.presentation.request.RecoveryConfirmRequest;
import kr.douid.brand.client.presentation.request.RecoveryRequest;
import kr.douid.brand.client.presentation.response.RecoveryConfirmResponse;
import kr.douid.brand.client.presentation.response.RecoveryRequestResponse;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.security.ClientTokenCookieProvider;
import lombok.RequiredArgsConstructor;

/**
 * Magic Link 기반 상담 복원 요청·완료 API
 *
 * 두 엔드포인트 모두 {@code client_token} 인증을 요구하지 않는다 — Cookie가 없는 사용자를 위한
 * 진입점이다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/recovery")
public class ClientRecoveryController {

    private final RecoveryRequestService recoveryRequestService;
    private final RecoveryConfirmationService recoveryConfirmationService;
    private final ClientTokenCookieProvider clientTokenCookieProvider;

    /**
     * Magic Link 기반 상담 복원을 요청
     *
     * @param request 복원을 요청할 이메일이 담긴 요청
     * @return 이메일 등록 여부와 무관하게 항상 동일한 202 응답
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<RecoveryRequestResponse>> request(@Valid @RequestBody RecoveryRequest request) {
        recoveryRequestService.request(request.email());

        RecoveryRequestResponse body = new RecoveryRequestResponse("해당 이메일로 복원 가능한 경우 안내를 전송했습니다.");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(body));
    }

    /**
     * Magic Link Recovery Token을 검증해 상담 주체를 복원
     *
     * @param request Magic Link의 raw token이 담긴 요청
     * @return 200 응답과 새로 발급된 {@code client_token} 쿠키
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<RecoveryConfirmResponse>> confirm(
            @Valid @RequestBody RecoveryConfirmRequest request) {
        String rawClientToken = recoveryConfirmationService.confirm(request.token());
        ResponseCookie cookie = clientTokenCookieProvider.issueCookie(rawClientToken);

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(ApiResponse.success(new RecoveryConfirmResponse(true)));
    }
}
