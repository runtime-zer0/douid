package kr.douid.brand.auth.application;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminNotFoundException;
import kr.douid.brand.auth.domain.AdminRepository;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 조회 유스케이스를 처리하는 서비스
 *
 * 자격 증명 검증 자체는 Spring Security 필터 체인이 담당하고, 이 서비스는 인증된 이메일로 관리자 정보를 조회하는 역할만 맡는다
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AdminRepository adminRepository;

    /**
     * 인증된 이메일로 관리자 정보를 조회
     *
     * @param email 인증에 성공한 이메일
     * @return 조회된 관리자 결과
     * @throws AdminNotFoundException 해당 이메일의 관리자를 찾을 수 없는 경우
     */
    public AdminResult resolve(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(AdminNotFoundException::new);
        return AdminResult.from(admin);
    }

    /**
     * 현재 인증된 관리자 정보를 조회
     *
     * @return 현재 관리자 결과
     * @throws AdminNotFoundException 인증 주체에 해당하는 계정을 찾을 수 없는 경우
     */
    public AdminResult getCurrentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return resolve(email);
    }
}
