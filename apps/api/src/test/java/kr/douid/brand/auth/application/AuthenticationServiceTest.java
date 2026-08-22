package kr.douid.brand.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminNotFoundException;
import kr.douid.brand.auth.domain.AdminRepository;
import kr.douid.brand.auth.domain.AdminRole;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AdminRepository adminRepository;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(adminRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolve_이메일로_관리자_정상_조회() {
        Admin admin = Admin.create("admin@douid.kr", "hashed", AdminRole.ADMIN);
        given(adminRepository.findByEmail("admin@douid.kr")).willReturn(Optional.of(admin));

        AdminResult result = authenticationService.resolve("admin@douid.kr");

        assertThat(result.email()).isEqualTo("admin@douid.kr");
        assertThat(result.role()).isEqualTo(AdminRole.ADMIN);
    }

    @Test
    void resolve_계정_미존재시_예외() {
        given(adminRepository.findByEmail("unknown@douid.kr")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.resolve("unknown@douid.kr"))
                .isInstanceOf(AdminNotFoundException.class);
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
    void getCurrentAdmin_계정_미존재_예외() {
        given(adminRepository.findByEmail("admin@douid.kr")).willReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@douid.kr", null, List.of()));

        assertThatThrownBy(() -> authenticationService.getCurrentAdmin())
                .isInstanceOf(AdminNotFoundException.class);
    }
}
