package kr.douid.brand.auth.domain;

import java.util.Optional;

/**
 * 관리자 계정 상태 변경 포트
 *
 * Aggregate 저장과 복원을 담당한다
 */
public interface AdminRepository {

    /**
     * email로 관리자 계정을 조회
     *
     * @param email 조회할 로그인 식별자
     * @return 조회 결과
     */
    Optional<Admin> findByEmail(String email);

    /**
     * 관리자 계정을 저장
     *
     * @param admin 저장 대상 관리자 계정
     * @return 저장된 관리자 계정
     */
    Admin save(Admin admin);

    /**
     * 등록된 관리자 계정이 하나라도 존재하는지 확인
     *
     * @return 계정 존재 여부
     */
    boolean existsAny();
}
