package kr.douid.brand.admin.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @Test
    void 관리자_계정이_없고_환경변수가_설정되어있으면_초기_계정을_생성() {
        given(adminRepository.existsAny()).willReturn(false);
        given(environment.getProperty("ADMIN_INIT_EMAIL")).willReturn("admin@douid.kr");
        given(environment.getProperty("ADMIN_INIT_PASSWORD")).willReturn("password");
        given(passwordEncoder.encode("password")).willReturn("encoded-password");

        AdminInitializer initializer = new AdminInitializer(adminRepository, passwordEncoder, environment);
        initializer.run(null);

        verify(adminRepository).save(any(Admin.class));
    }

    @Test
    void 관리자_계정이_이미_존재하면_생성을_스킵() {
        given(adminRepository.existsAny()).willReturn(true);

        AdminInitializer initializer = new AdminInitializer(adminRepository, passwordEncoder, environment);
        initializer.run(null);

        verify(adminRepository, never()).save(any(Admin.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void 환경변수가_설정되지_않으면_예외_없이_생성을_스킵() {
        given(adminRepository.existsAny()).willReturn(false);
        given(environment.getProperty("ADMIN_INIT_EMAIL")).willReturn(null);
        given(environment.getProperty("ADMIN_INIT_PASSWORD")).willReturn(null);

        AdminInitializer initializer = new AdminInitializer(adminRepository, passwordEncoder, environment);

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
        verify(adminRepository, never()).save(any(Admin.class));
    }
}
