package kr.douid.brand.auth.application;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;
import kr.douid.brand.auth.domain.AuthenticationFailedException;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 로그인·현재 관리자 조회 유스케이스를 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;

    /**
     * 관리자 자격 증명을 검증
     *
     * @param command 로그인 입력값
     * @return 인증된 관리자 결과
     * @throws AuthenticationFailedException 계정 미존재, 비밀번호 불일치, 비활성 계정인 경우 모두 동일하게 발생
     */
    public AdminResult authenticate(LoginCommand command) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(command.email(), command.password()));
        } catch (AuthenticationException e) {
            throw new AuthenticationFailedException();
        }

        Admin admin = adminRepository.findByEmail(authentication.getName())
                .orElseThrow(AuthenticationFailedException::new);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return AdminResult.from(admin);
    }

    /**
     * 현재 인증된 관리자 정보를 조회
     *
     * @return 현재 관리자 결과
     * @throws AuthenticationFailedException 인증 주체에 해당하는 계정을 찾을 수 없는 경우
     */
    public AdminResult getCurrentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(AuthenticationFailedException::new);
        return AdminResult.from(admin);
    }
}
