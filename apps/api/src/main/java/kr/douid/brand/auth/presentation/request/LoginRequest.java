package kr.douid.brand.auth.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import kr.douid.brand.auth.application.LoginCommand;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
    /**
     * 요청값을 로그인 command로 변환
     *
     * @return 로그인 command
     */
    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}
