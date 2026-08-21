package kr.douid.brand.auth.infrastructure.security;

import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link Admin}을 Spring Security {@link UserDetails}로 변환하는 인프라 어댑터
 *
 * domain은 Spring Security 타입을 알지 못하므로, 이 어댑터가 조회와 변환을 함께 담당한다
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AdminRepository adminRepository;

    /**
     * email로 관리자 계정을 조회해 {@link UserDetails}로 변환
     *
     * @param email 로그인 식별자
     * @return 변환된 사용자 인증 정보
     * @throws UsernameNotFoundException 계정을 찾을 수 없는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return User.builder()
                .username(admin.getEmail())
                .password(admin.getPasswordHash())
                .disabled(!admin.isActive())
                .authorities(List.of(() -> ROLE_PREFIX + admin.getRole().name()))
                .build();
    }
}
