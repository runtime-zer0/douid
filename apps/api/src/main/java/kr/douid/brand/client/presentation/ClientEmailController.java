package kr.douid.brand.client.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.douid.brand.client.application.ClientEmailRegistrationService;
import kr.douid.brand.client.application.ClientEmailVerificationService;
import kr.douid.brand.client.domain.ClientEmail;
import kr.douid.brand.client.presentation.request.RegisterEmailRequest;
import kr.douid.brand.client.presentation.request.VerifyEmailCodeRequest;
import kr.douid.brand.client.presentation.response.EmailRegistrationResponse;
import kr.douid.brand.client.presentation.response.EmailVerificationResponse;
import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.security.ClientIdentityContext;
import lombok.RequiredArgsConstructor;

/**
 * Recovery Email 등록·검증 API
 *
 * 두 엔드포인트 모두 현재 {@code client_token}으로 식별된 상담 주체 credential을 요구한다(FR-003).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/emails")
public class ClientEmailController {

    private static final long CODE_EXPIRATION_SECONDS = 300;

    private final ClientEmailRegistrationService clientEmailRegistrationService;
    private final ClientEmailVerificationService clientEmailVerificationService;

    /**
     * Recovery Email 등록을 요청하고 인증 코드를 발송
     *
     * @param request 등록할 이메일이 담긴 요청
     * @return 202 응답과 발송 결과
     * @throws AuthenticationRequiredException {@code client_token}이 없거나 무효한 경우
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EmailRegistrationResponse>> register(
            @Valid @RequestBody RegisterEmailRequest request) {
        Long clientIdentityId = requireClientIdentity();

        boolean alreadyVerified = clientEmailRegistrationService.register(clientIdentityId, request.email());

        EmailRegistrationResponse body =
                new EmailRegistrationResponse(request.email(), CODE_EXPIRATION_SECONDS, alreadyVerified);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(body));
    }

    /**
     * 발송된 인증 코드를 검증해 이메일을 VERIFIED 상태로 전환
     *
     * @param request 검증할 이메일과 코드가 담긴 요청
     * @return 200 응답과 검증 결과
     * @throws AuthenticationRequiredException {@code client_token}이 없거나 무효한 경우
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> verify(
            @Valid @RequestBody VerifyEmailCodeRequest request) {
        Long clientIdentityId = requireClientIdentity();

        ClientEmail clientEmail =
                clientEmailVerificationService.verify(clientIdentityId, request.email(), request.code());

        EmailVerificationResponse body =
                new EmailVerificationResponse(clientEmail.getEmail(), clientEmail.getVerifiedAt());
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    private Long requireClientIdentity() {
        return ClientIdentityContext.get().orElseThrow(AuthenticationRequiredException::new);
    }

    /**
     * {@code client_token} 인증이 필요한 API에 인증 정보가 없을 때 발생하는 예외
     */
    static class AuthenticationRequiredException extends DomainException {
        AuthenticationRequiredException() {
            super(DomainErrorType.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다.");
        }
    }
}
