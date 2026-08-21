package kr.douid.brand.auth.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.douid.brand.auth.domain.Admin;

public interface AdminJpaRepository extends JpaRepository<Admin, Long> {

    /**
     * email로 관리자 계정을 조회
     *
     * @param email 조회할 로그인 식별자
     * @return 조회 결과
     */
    Optional<Admin> findByEmail(String email);
}
