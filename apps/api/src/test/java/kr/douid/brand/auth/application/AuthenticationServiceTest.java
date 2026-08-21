package kr.douid.brand.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;
import kr.douid.brand.auth.domain.AdminRole;
import kr.douid.brand.auth.domain.AuthenticationFailedException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AdminRepository adminRepository;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(authenticationManager, adminRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticate_정상_인증() {
        LoginCommand command = new LoginCommand("admin@douid.kr", "password");
        Admin admin = Admin.create("admin@douid.kr", "hashed", AdminRole.ADMIN);
        Authentication authenticated =
                new UsernamePasswordAuthenticationToken("admin@douid.kr", "password", List.of());
        given(authenticationManager.authenticate(any())).willReturn(authenticated);
        given(adminRepository.findByEmail("admin@douid.kr")).willReturn(Optional.of(admin));

        AdminResult result = authenticationService.authenticate(command);

        assertThat(result.email()).isEqualTo("admin@douid.kr");
        assertThat(result.role()).isEqualTo(AdminRole.ADMIN);
    }

    @Test
    void authenticate_계정_미존재_인증실패예외() {
        LoginCommand command = new LoginCommand("unknown@douid.kr", "password");
        given(authenticationManager.authenticate(any()))
                .willThrow(new UsernameNotFoundException("unknown@douid.kr"));

        assertThatThrownBy(() -> authenticationService.authenticate(command))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void authenticate_비밀번호_불일치_인증실패예외() {
        LoginCommand command = new LoginCommand("admin@douid.kr", "wrong-password");
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate(command))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void authenticate_비활성_계정_인증실패예외() {
        LoginCommand command = new LoginCommand("admin@douid.kr", "password");
        given(authenticationManager.authenticate(any()))
                .willThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> authenticationService.authenticate(command))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void authenticate_세가지_실패케이스_동일한_예외타입과_메시지() {
        LoginCommand command = new LoginCommand("admin@douid.kr", "password");
        given(authenticationManager.authenticate(any()))
                .willThrow(new UsernameNotFoundException("x"))
                .willThrow(new BadCredentialsException("x"))
                .willThrow(new DisabledException("x"));

        String accountNotFoundMessage = catchMessage(command);
        String badPasswordMessage = catchMessage(command);
        String disabledMessage = catchMessage(command);

        assertThat(accountNotFoundMessage)
                .isEqualTo(badPasswordMessage)
                .isEqualTo(disabledMessage);
    }

    private String catchMessage(LoginCommand command) {
        try {
            authenticationService.authenticate(command);
            throw new AssertionError("예외가 발생해야 한다");
        } catch (AuthenticationFailedException e) {
            return e.getMessage();
        }
    }

    @Test
    void getCurrentAdmin_인증컨텍스트_기준_정상_조회() {
        Admin admin = Admin.create("admin@douid.kr", "hashed", AdminRole.ADMIN);
        given(adminRepository.findByEmail("admin@douid.kr")).willReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@douid.kr", null, List.of()));

        AdminResult result = authenticationService.getCurrentAdmin();

        assertThat(result.email()).isEqualTo("admin@douid.kr");
        assertThat(result.role()).isEqualTo(AdminRole.ADMIN);
    }

    @Test
    void getCurrentAdmin_계정_미존재_인증실패예외() {
        given(adminRepository.findByEmail("admin@douid.kr")).willReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@douid.kr", null, List.of()));

        assertThatThrownBy(() -> authenticationService.getCurrentAdmin())
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
