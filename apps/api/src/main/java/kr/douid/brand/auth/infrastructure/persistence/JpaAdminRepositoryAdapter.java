package kr.douid.brand.auth.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaAdminRepositoryAdapter implements AdminRepository {

    private final AdminJpaRepository adminJpaRepository;

    /**
     * email로 관리자 계정을 조회
     *
     * @param email 조회할 로그인 식별자
     * @return 조회 결과
     */
    @Override
    public Optional<Admin> findByEmail(String email) {
        return adminJpaRepository.findByEmail(email);
    }

    /**
     * 관리자 계정을 저장
     *
     * @param admin 저장 대상 관리자 계정
     * @return 저장된 관리자 계정
     */
    @Override
    public Admin save(Admin admin) {
        return adminJpaRepository.save(admin);
    }

    /**
     * 등록된 관리자 계정이 하나라도 존재하는지 확인
     *
     * @return 계정 존재 여부
     */
    @Override
    public boolean existsAny() {
        return adminJpaRepository.count() > 0;
    }
}
