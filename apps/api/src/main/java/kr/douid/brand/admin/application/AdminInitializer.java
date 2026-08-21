package kr.douid.brand.admin.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;
import kr.douid.brand.auth.domain.AdminRole;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 기동 시 초기 관리자 계정을 준비하는 provisioning 절차
 *
 * 등록된 관리자 계정이 하나도 없을 때만 {@code ADMIN_INIT_EMAIL}/{@code ADMIN_INIT_PASSWORD}
 * 환경변수로 계정을 1회 생성한다. 회원가입 API를 대신하는 운영 초기화 수단이다
 */
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    /**
     * 관리자 계정이 없을 때만 환경변수 기반으로 초기 계정을 생성
     *
     * @param args 애플리케이션 실행 인자 (사용하지 않음)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.existsAny()) {
            return;
        }

        String email = environment.getProperty("ADMIN_INIT_EMAIL");
        String password = environment.getProperty("ADMIN_INIT_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_INIT_EMAIL 또는 ADMIN_INIT_PASSWORD가 설정되지 않아 초기 관리자 계정을 생성하지 않음");
            return;
        }

        adminRepository.save(Admin.create(email, passwordEncoder.encode(password), AdminRole.ADMIN));
        log.info("초기 관리자 계정 생성 완료: {}", email);
    }
}
